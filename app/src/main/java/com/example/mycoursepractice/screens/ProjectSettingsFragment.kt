package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Role
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.showIf
import com.example.mycoursepractice.ui.tabHost

/** 项目设置：邀请码、是否需要确认、成员、切换 / 退出。 */
class ProjectSettingsFragment : Fragment() {

    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_project_settings, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(root, "项目设置")
        val project = Demo.current

        root.findViewById<TextView>(R.id.projectName).text = project.name
        root.findViewById<TextView>(R.id.projectMeta).text =
            "${Demo.courseLabel} · ${project.groupLabel} · ${project.role.label}"

        val creator = project.role == com.example.mycoursepractice.data.Role.CREATOR
        if (creator) {
            root.findViewById<TextView>(R.id.inviteCode).text = spaced(project.inviteCode)
        } else {
            listOf(
                R.id.codeLabel, R.id.inviteCode, R.id.copyCode, R.id.newCode, R.id.approvalRow
            ).forEach { root.findViewById<View>(it).showIf(false) }
        }

        root.findViewById<View>(R.id.copyCode).setOnClickListener {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("邀请码", project.inviteCode))
            Toast.makeText(requireContext(), "邀请码已复制", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.newCode).setOnClickListener {
            Demo.regenerateCode(project)
            root.findViewById<TextView>(R.id.inviteCode).apply {
                text = spaced(project.inviteCode)
                Motion.stamp(this)
            }
            Toast.makeText(requireContext(), "旧邀请码立即失效", Toast.LENGTH_SHORT).show()
        }

        if (creator) bindApproval(project) else {
            val owner = project.members.firstOrNull { it.id != Demo.VIEWER }?.name ?: "发起人"
            root.findViewById<TextView>(R.id.codeNote).text =
                "邀请码与「是否需要确认」由发起人 $owner 管理；你只看到成员和已有的确认。"
        }
        buildMembers(project)

        root.findViewById<View>(R.id.switchProject).setOnClickListener {
            tabHost()?.openLibrary()
        }

        root.findViewById<View>(R.id.leaveProject).setOnClickListener {
            Demo.leave(project)
            tabHost()?.openLibrary()
        }
    }

    private fun spaced(code: String): String =
        if (code.length == 6) code.toCharArray().joinToString(" ") else code

    private fun bindApproval(project: com.example.mycoursepractice.data.Project) {
        val sw = root.findViewById<TextView>(R.id.approvalSwitch)
        val hint = root.findViewById<TextView>(R.id.approvalHint)
        val note = root.findViewById<TextView>(R.id.codeNote)
        fun paint() {
            sw.text = if (project.requireApproval) "开" else "关"
            sw.setBackgroundResource(
                if (project.requireApproval) R.drawable.bg_stamp_filled else R.drawable.bg_chip
            )
            sw.setTextColor(colorOf(if (project.requireApproval) R.color.sheet else R.color.muted))
            hint.text = if (project.requireApproval) "需要你点确认才能进组" else "关闭时直接入组"
            note.text = if (project.requireApproval)
                "拿到码的人先出现在「待确认」里，你点了确认才进组。"
            else
                "拿到码的人可以直接加入，不需要你确认；打开开关后，新成员要先等你确认。"
        }
        paint()
        root.findViewById<View>(R.id.approvalRow).setOnClickListener {
            project.requireApproval = !project.requireApproval
            Demo.bump()
            paint()
            Motion.stamp(sw)
        }
    }

    private fun buildMembers(project: com.example.mycoursepractice.data.Project) {
        root.findViewById<TextView>(R.id.membersLabel).text = "成员 ${project.members.size}"
        val rows = root.findViewById<LinearLayout>(R.id.memberRows)
        rows.removeAllViews()
        project.members.forEach { member ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10f), 0, dp(10f))
            }
            row.addView(TextView(requireContext()).apply {
                text = member.initial
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_avatar)
                setTextColor(colorOf(R.color.accent))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(dp(30f), dp(30f))
            })
            row.addView(TextView(requireContext()).apply {
                text = member.name
                setTextColor(colorOf(R.color.ink))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginStart = dp(12f) }
            })
            row.addView(TextView(requireContext()).apply {
                text = when {
                    member.id == Demo.VIEWER -> "你"
                    member.id == project.members.first().id && project.role == Role.CREATOR -> "发起人"
                    else -> ""
                }
                setTextColor(colorOf(R.color.muted))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            })
            if (member.id != Demo.VIEWER && project.role == Role.CREATOR) {
                row.addView(TextView(requireContext()).apply {
                    text = "移除"
                    setTextColor(colorOf(R.color.ochre))
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(dp(14f), dp(4f), 0, dp(4f))
                    setOnClickListener {
                        Demo.removeMember(project, member.id)
                        buildMembers(project)
                    }
                })
            }
            rows.addView(row)
            rows.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
                setBackgroundColor(colorOf(R.color.line))
            })
        }
    }
}
