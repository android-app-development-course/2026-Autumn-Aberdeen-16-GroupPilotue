package com.example.mycoursepractice.data

import java.util.Locale
import kotlin.math.roundToInt

data class Course(val id: String, val code: String, val name: String, val term: String) {
    val label: String get() = "$code · $name"
}

enum class Role(val label: String) { CREATOR("创建者"), MEMBER("成员") }

/** 一条「等你加入」的邀请：带着一个尚未进入当前账号的 Project。 */
data class Invitation(
    val id: String,
    val code: String,
    val inviterName: String,
    val sentAt: String,
    val memberCount: Int,
    val project: Project
)

sealed class JoinResult {
    data class Ok(val project: Project) : JoinResult()
    object NotFound : JoinResult()
    object Expired : JoinResult()
    data class AlreadyIn(val name: String) : JoinResult()
}

/**
 * 一个小组作业项目 = 一门课下的一次分组作业。
 * 所有计分都在这里做，Demo 只是「当前项目」的门面。
 */
class Project(
    val id: String,
    val courseId: String,
    var name: String,
    var groupLabel: String,
    var planVersion: String,
    var role: Role,
    val members: MutableList<Member>,
    val items: MutableList<WorkItem>,
    var totalPoints: Double = 100.0,
    var archived: Boolean = false,
    var lastActivity: String = "今天",
    var inviteCode: String = "",
    var requireApproval: Boolean = false
) {
    fun member(id: String): Member = members.first { it.id == id }

    fun nameOf(id: String?): String = id?.let { member(it).name } ?: "—"

    fun item(id: String): WorkItem = items.first { it.id == id }

    fun scoreOf(memberId: String): Double = items.sumOf { it.scoreOf(memberId) }

    fun total(): Double = items.sumOf { it.confirmedPoints }

    fun ratioOf(memberId: String): Double {
        val sum = total()
        return if (sum <= 0.0) 0.0 else scoreOf(memberId) / sum * 100.0
    }

    fun itemsOf(stage: Stage): List<WorkItem> = items.filter { it.stage == stage }

    fun pendingForViewer(viewer: String): List<WorkItem> =
        items.filter { it.stage == Stage.PENDING && it.ownerId != viewer }

    fun confirmedCount(): Int = items.count { it.accepted > 0 }

    fun fullyConfirmedCount(): Int = items.count { it.accepted >= 100 }

    fun pendingCount(): Int = items.count { it.stage == Stage.PENDING }

    fun openCount(): Int = items.count { it.stage == Stage.PENDING || it.stage == Stage.DOING }

    fun shareSummary(item: WorkItem): String {
        if (item.shares.size <= 1) return "独担"
        return "分摊 " + item.shares.values
            .sortedDescending()
            .joinToString("/") { (it * 100).roundToInt().toString() }
    }

    fun formula(item: WorkItem, memberId: String): String {
        val share = item.shareOf(memberId)
        val score = item.scoreOf(memberId)
        return String.format(
            Locale.US, "%.1f × %d%% × %d%% = %.1f",
            item.points.toDouble(), item.accepted, (share * 100).roundToInt(), score
        )
    }

    fun confirmerFor(item: WorkItem, viewer: String): String =
        members.filter { it.id != item.ownerId }
            .maxByOrNull { item.shareOf(it.id) }?.id
            ?: viewer

    fun unconfirmedCount(): Int = items.count { it.accepted < 100 }

    fun shareItemsOf(memberId: String): List<WorkItem> = items.filter { it.shareOf(memberId) > 0.0 }

    fun shareOfTotal(): Double = items.sumOf { it.points.toDouble() }

    fun confirmedShareOfTotal(): Double {
        val all = shareOfTotal()
        return if (all <= 0.0) 0.0 else total() / all * 100.0
    }

    /** 我的待办：待我确认 + 我提交但没确认的。 */
    fun waitingOnMe(viewer: String): Int = items.count { it.stage == Stage.PENDING && it.ownerId == viewer }

    fun confirmedPointsOfViewer(viewer: String): Double = scoreOf(viewer)
}
