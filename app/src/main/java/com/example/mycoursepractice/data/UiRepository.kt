package com.example.mycoursepractice.data

import android.content.Context
import android.os.Handler
import android.os.Looper

fun interface UiRequest { fun cancel() }
data class ProfileDraft(val account: String, val real: String, val phone: String, val email: String, val avatar: String?)

/** Replace UiServices.repository with a server adapter. Callbacks must run on the main thread.
 * IDs are project-scoped; servers must enforce permissions independently of the UI.
 */
interface UiRepository {
    fun tasks(callback: (Result<List<TaskEntry>>) -> Unit): UiRequest
    fun group(projectId: String, callback: (Result<List<GroupPerson>>) -> Unit): UiRequest
    fun invites(projectId: String): List<GroupInvite>
    fun invite(projectId: String, account: String, callback: (Result<Unit>) -> Unit): UiRequest
    fun permission(projectId: String, memberId: String, role: GroupPermission, callback: (Result<Unit>) -> Unit): UiRequest
    fun remove(projectId: String, memberId: String, callback: (Result<Unit>) -> Unit): UiRequest
    fun saveProfile(context: Context, draft: ProfileDraft, callback: (Result<Unit>) -> Unit): UiRequest
    fun changePassword(context: Context, old: String, new: String, callback: (Result<Unit>) -> Unit): UiRequest
    fun analyze(projectId: String, itemId: String, evidence: Evidence, note: String,
                progress: (Int) -> Unit, callback: (Result<AnalysisResult>) -> Unit): UiRequest
}

object UiServices { var repository: UiRepository = DemoUiRepository() }

class DemoUiRepository : UiRepository {
    private val roles = mutableMapOf<String, MutableMap<String, GroupPermission>>()
    private val invitations = mutableMapOf<String, MutableList<GroupInvite>>()
    private fun project(id: String) = Demo.projects.first { it.id == id }
    private fun roleMap(p: Project) = roles.getOrPut(p.id) {
        p.members.associate { it.id to GroupPermission.MEMBER }.toMutableMap().apply {
            val owner = if (p.role == Role.CREATOR) Demo.VIEWER else p.members.firstOrNull { it.id != Demo.VIEWER }?.id
            owner?.let { put(it, GroupPermission.OWNER) }
        }
    }
    private fun manage(p: Project) {
        require(roleMap(p)[Demo.VIEWER] in listOf(GroupPermission.OWNER, GroupPermission.ADMIN)) { "仅组长或管理员可管理小组" }
        require(!p.archived) { "已归档项目不可管理" }
    }
    private fun <T> respond(callback: (Result<T>) -> Unit, action: () -> T): UiRequest {
        callback(runCatching(action))
        return UiRequest { }
    }
    override fun tasks(callback: (Result<List<TaskEntry>>) -> Unit) = respond(callback) {
        Demo.projects.filter { !it.archived }.flatMap { p -> p.items.map { TaskEntry(p, it) } }
    }
    override fun group(projectId: String, callback: (Result<List<GroupPerson>>) -> Unit) = respond(callback) {
        val p = project(projectId)
        p.members.map { GroupPerson(it, roleMap(p)[it.id] ?: GroupPermission.MEMBER) }
    }
    override fun invites(projectId: String): List<GroupInvite> = invitations[projectId]?.toList().orEmpty()
    override fun invite(projectId: String, account: String, callback: (Result<Unit>) -> Unit) = respond(callback) {
        val p = project(projectId)
        manage(p)
        val clean = account.trim()
        require(clean.length >= 2) { "请输入至少两个字符的账号或邮箱" }
        val pending = invitations.getOrPut(projectId) { mutableListOf() }
        require(p.members.none { it.name.equals(clean, true) || it.id.equals(clean, true) } && pending.none { it.account.equals(clean, true) }) { "该成员已在组内或已邀请" }
        pending += GroupInvite(clean, GroupPermission.MEMBER)
        Demo.bump()
    }
    override fun permission(projectId: String, memberId: String, role: GroupPermission, callback: (Result<Unit>) -> Unit) = respond(callback) {
        val p = project(projectId)
        manage(p)
        require(roleMap(p)[Demo.VIEWER] == GroupPermission.OWNER) { "只有组长可以调整权限" }
        require(p.members.any { it.id == memberId }) { "成员已离组" }
        require(memberId != Demo.VIEWER && role != GroupPermission.OWNER) { "组长身份不可在此修改" }
        roleMap(p)[memberId] = role
        Demo.bump()
    }
    override fun remove(projectId: String, memberId: String, callback: (Result<Unit>) -> Unit) = respond(callback) {
        val p = project(projectId)
        manage(p)
        require(memberId != Demo.VIEWER && roleMap(p)[memberId] != GroupPermission.OWNER) { "不能请离自己或组长" }
        require(roleMap(p)[Demo.VIEWER] == GroupPermission.OWNER || roleMap(p)[memberId] == GroupPermission.MEMBER) { "管理员只能请离普通成员" }
        // Keep attribution/score records intact. Assignment transfer is a separate backend operation.
        require(p.items.none { it.stage != Stage.DONE && (it.ownerId == memberId || it.shareOf(memberId) > 0) }) { "该成员仍有未完成任务，请先在任务编辑中转交负责人和分摊，再请离" }
        val person = p.members.first { it.id == memberId }
        p.formerMembers += person
        p.members.remove(person)
        roleMap(p).remove(memberId)
        Demo.bump()
    }
    override fun saveProfile(context: Context, draft: ProfileDraft, callback: (Result<Unit>) -> Unit) = respond(callback) {
        require(draft.account.isNotBlank()) { "账号不能为空" }
        Profile.save(context, draft.account, draft.real, draft.phone, draft.email)
        draft.avatar?.let { Profile.setAvatar(context, it) }
    }
    override fun changePassword(context: Context, old: String, new: String, callback: (Result<Unit>) -> Unit) = respond(callback) {
        require(old.isNotEmpty()) { "请输入旧密码" }
        require(Profile.password != null) { "旧版登录未记录密码，请退出后重新登录，再修改密码" }
        require(Profile.password == old) { "旧密码不正确" }
        require(new.length in 6..32 && new != old) { "新密码需为 6–32 位，且不能与旧密码相同" }
        Profile.setPassword(context, new)
    }
    override fun analyze(projectId: String, itemId: String, evidence: Evidence, note: String,
                         progress: (Int) -> Unit, callback: (Result<AnalysisResult>) -> Unit): UiRequest {
        val handler = Handler(Looper.getMainLooper())
        var cancelled = false
        val steps = listOf(10, 35, 65, 85, 100).mapIndexed { index, percent ->
            Runnable {
                if (!cancelled) {
                    progress(percent)
                    if (percent == 100) callback(runCatching {
                        val p = project(projectId)
                        val item = p.item(itemId)
                        TaskInsights.seed(p)
                        val rows = TaskInsights.criteria(item).mapIndexed { i, row ->
                            // Deliberately deterministic demo evidence, never presented as real AI analysis.
                            if (row.ownerId == Demo.VIEWER || item.ownerId == Demo.VIEWER)
                                row.copy(achieved = if (i % 2 == 0) row.target else (row.target - 1).coerceAtLeast(0))
                            else row
                        }
                        AnalysisResult(rows, item.expectedPercent).also {
                            item.analysis = it
                            item.evidence = evidence
                            item.note = note
                            item.stage = Stage.PENDING
                            Demo.bump()
                        }
                    })
                }
            }.also { handler.postDelayed(it, (index + 1) * 550L) }
        }
        return UiRequest { cancelled = true; steps.forEach { handler.removeCallbacks(it) } }
    }
}
