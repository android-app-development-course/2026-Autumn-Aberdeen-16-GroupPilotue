package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.*
import com.example.mycoursepractice.ui.*

class GroupOverviewFragment : Fragment() {
    private lateinit var content: LinearLayout
    private var request: UiRequest? = null
    private val project get() = Demo.projects.first { it.id == requireArguments().getString("project") }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) arguments = Bundle().apply { putString("project", Demo.current.id) }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val shell = requireContext().column().apply { setBackgroundColor(colorOf(R.color.paper)) }
        shell.addView(inflater.inflate(R.layout.view_back_bar, shell, false))
        content = requireContext().column(20)
        shell.addView(ScrollView(requireContext()).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        return shell
    }
    override fun onViewCreated(view: View, state: Bundle?) { bindBackBar(view, "小组总览"); load() }
    private fun load() {
        content.removeAllViews()
        content.addView(requireContext().label("正在读取小组…"))
        request = UiServices.repository.group(project.id) { result ->
            if (view == null) return@group
            result.onSuccess { render(it) }.onFailure {
                content.removeAllViews()
                content.addView(requireContext().label(it.message ?: "读取失败"))
                content.addView(requireContext().action("重试") { load() })
            }
        }
    }
    private fun render(people: List<GroupPerson>) {
        val ctx = requireContext()
        content.removeAllViews()
        content.addView(ctx.label(project.groupLabel, 26f))
        content.addView(ctx.label("${project.name} · ${people.size} 位成员", 13f, R.color.muted))
        val me = people.firstOrNull { it.member.id == Demo.VIEWER }?.permission
        val manages = me in listOf(GroupPermission.OWNER, GroupPermission.ADMIN) && !project.archived
        content.addView(ctx.label("组长：成员与权限管理；管理员：邀请、请离普通成员；成员：查看与协作。", 12f, R.color.muted))
        content.metric("项目细则实现", TaskInsights.weightedProgress(project.items.map { TaskEntry(project, it) }))
        people.forEach { person ->
            val card = ctx.column(14).apply {
                rounded(R.color.sheet)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) }
            }
            val member = person.member
            card.addView(ctx.label("${member.name}${if (member.id == Demo.VIEWER) "（我）" else ""} · ${person.permission.label}", 16f))
            card.addView(ctx.label("参与 ${project.shareItemsOf(member.id).size} 项任务 · 已确认 ${n1(project.scoreOf(member.id))} 分", 12f, R.color.muted))
            if (manages && member.id != Demo.VIEWER && person.permission != GroupPermission.OWNER) {
                if (me == GroupPermission.OWNER) card.addView(ctx.action("权限管理") {
                    AlertDialog.Builder(ctx).setTitle("${member.name}的权限")
                        .setItems(arrayOf("管理员 · 可邀请及请离普通成员", "成员 · 查看和协作")) { _, index ->
                            request = UiServices.repository.permission(project.id, member.id,
                                if (index == 0) GroupPermission.ADMIN else GroupPermission.MEMBER, ::finishAction)
                        }.setNegativeButton("取消", null).show()
                })
                if (me == GroupPermission.OWNER || person.permission == GroupPermission.MEMBER) card.addView(ctx.action("请离小组") {
                    AlertDialog.Builder(ctx).setTitle("请离 ${member.name}？")
                        .setMessage("保留历史成果与贡献记录。若有未完成任务，需先在任务编辑中完成转交。")
                        .setNegativeButton("取消", null).setPositiveButton("确认请离") { _, _ ->
                            request = UiServices.repository.remove(project.id, member.id, ::finishAction)
                        }.show()
                })
            }
            content.addView(card)
        }
        if (manages) {
            content.addView(ctx.action("＋ 邀请成员", true) {
                val field = EditText(ctx).apply { hint = "账号或邮箱"; isSingleLine = true; setPadding(dp(24f), dp(16f), dp(24f), dp(16f)) }
                val dialog = AlertDialog.Builder(ctx).setTitle("邀请加入 ${project.groupLabel}").setView(field)
                    .setNegativeButton("取消", null).setPositiveButton("创建邀请", null).create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        request = UiServices.repository.invite(project.id, field.text.toString()) { result ->
                            if (view != null) result.onSuccess { dialog.dismiss(); load() }
                                .onFailure { field.error = it.message ?: "邀请失败" }
                        }
                    }
                }
                dialog.show()
            })
            val pending = UiServices.repository.invites(project.id)
            content.addView(ctx.label("待加入邀请 · ${pending.size}", 16f))
            pending.forEach { content.addView(ctx.label("${it.account} · ${it.permission.label} · 等待接受", 13f)) }
            content.addView(ctx.label("演示邀请仅保存在本机，尚未发送。", 11f, R.color.muted))
        } else content.addView(ctx.label("当前为查看权限，请联系组长邀请成员或调整权限。", 13f, R.color.muted))
    }
    private fun finishAction(result: Result<Unit>) {
        if (view == null) return
        result.onSuccess { load() }.onFailure { Toast.makeText(requireContext(), it.message ?: "操作失败", Toast.LENGTH_LONG).show() }
    }
    override fun onDestroyView() { request?.cancel(); super.onDestroyView() }
}
