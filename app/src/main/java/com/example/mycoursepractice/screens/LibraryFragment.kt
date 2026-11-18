package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Profile
import com.example.mycoursepractice.data.Project
import com.example.mycoursepractice.data.Role
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.ui.*
import com.example.mycoursepractice.ui.Avatar
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.hairline
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.showIf
import com.example.mycoursepractice.ui.tabHost

/**
 * 项目库：账号层。按课程列出项目，顶部是邀请。
 * 这里没有底部标签栏 —— 只有进入某个项目之后才有。
 */
class LibraryFragment : Fragment() {

    private lateinit var root: View
    private val collapsed = mutableSetOf<String>()

    private val sections: LinearLayout get() = root.findViewById(R.id.sections)
    private val emptyBox: View get() = root.findViewById(R.id.emptyBox)
    private val inviteBox: View get() = root.findViewById(R.id.inviteBox)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_library, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindAppSettings(root)
        bindProfileChip()
        val parent = sections.parent as LinearLayout
        parent.addView(requireContext().action("个人全任务总览", true) {
            push(TaskOverviewFragment.newInstance())
        }.apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_task_tree, 0, 0, 0)
            compoundDrawablePadding = dp(12f)
            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(colorOf(R.color.sheet))
        }, parent.indexOfChild(sections))

        root.findViewById<View>(R.id.createBtn).setOnClickListener {
            push(CreateProjectFragment())
        }
        root.findViewById<View>(R.id.joinBtn).setOnClickListener {
            push(JoinProjectFragment.newInstance())
        }

        bindInvitation()
        rebuild()
    }

    override fun onResume() {
        super.onResume()
        Demo.closeProject()
        bindProfileChip()
        bindInvitation()
        rebuild()
    }

    /** 抬头左边：头像 + 账号名，点进「个人信息」。 */
    private fun bindProfileChip() {
        val name = Profile.displayName().ifBlank { Demo.me.name }
        val label = root.findViewById<TextView>(R.id.profileName)
        label.text = name
        Avatar.bind(
            requireContext(),
            root.findViewById(R.id.profileAvatarImage),
            root.findViewById(R.id.profileAvatar),
            Profile.avatarPath,
            Profile.initial(),
            24f
        )
        root.findViewById<View>(R.id.profileChip).setOnClickListener {
            push(ProfileFragment())
        }
    }

    private fun bindInvitation() {
        val invite = Demo.pendingInvitations().firstOrNull()
        inviteBox.showIf(invite != null)
        if (invite == null) return
        root.findViewById<TextView>(R.id.inviteTitle).text = invite.project.name
        root.findViewById<TextView>(R.id.inviteMeta).text =
            "${invite.inviterName} 邀请你 · ${Demo.course(invite.project.courseId).code} · ${invite.memberCount} 人"
        inviteBox.setOnClickListener {
            push(JoinProjectFragment.newInstance(invite.code))
        }
    }

    private fun rebuild() {
        val courses = Demo.coursesWithProjects()
        val active = Demo.projects.count { !it.archived }
        val pending = Demo.projects.sumOf { p -> p.items.count { it.stage == Stage.PENDING } }

        root.findViewById<TextView>(R.id.coursesLabel).text = "${courses.size} 门课程"
        root.findViewById<TextView>(R.id.summary).text = when {
            active == 0 -> "还没有项目"
            pending > 0 -> "$active 个项目 · $pending 项待确认"
            else -> "$active 个项目"
        }

        sections.removeAllViews()
        emptyBox.showIf(active == 0)

        courses.forEach { course ->
            val header = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(20f), 0, dp(2f))
            }
            header.addView(sectionLabel(course.label))
            header.addView(
                View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(1f), 1f).also {
                        it.marginStart = dp(12f)
                        it.marginEnd = dp(12f)
                    }
                    setBackgroundColor(colorOf(R.color.line))
                }
            )
            header.addView(sectionLabel(course.term, R.color.muted))
            sections.addView(header)

            Demo.projectsOf(course.id).filter { !it.archived }.forEach { project ->
                val card = requireContext().column(10).apply {
                    rounded(R.color.accent_wash)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) }
                }
                card.addView(projectRow(project))
                card.addView(requireContext().action("${if (project.id in collapsed) "▸ 展开" else "▾ 收起"}任务 · ${project.items.size} 项") {
                    if (!collapsed.add(project.id)) collapsed.remove(project.id)
                    rebuild()
                })
                if (project.id !in collapsed) project.items.forEach { item ->
                    card.addView(requireContext().action("${item.title}\n${item.stage.label} · DDL ${item.due.ifBlank { "未设置" }}") {
                        Demo.openProject(project.id)
                        push(ItemDetailFragment.newInstance(item.id))
                    }.apply { rounded(R.color.sheet); gravity = Gravity.START or Gravity.CENTER_VERTICAL })
                }
                sections.addView(card)
            }
        }
    }

    private fun sectionLabel(text: String, colorRes: Int = R.color.accent): TextView =
        TextView(requireContext()).apply {
            this.text = text
            typeface = Fonts.sansMedium
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10.5f)
            letterSpacing = 0.12f
            includeFontPadding = false
            setTextColor(colorOf(colorRes))
        }

    private fun projectRow(project: Project): View {
        val row = layoutInflater.inflate(R.layout.row_project, sections, false)
        row.rounded(R.color.accent)
        row.findViewById<TextView>(R.id.projectName).setTextColor(colorOf(R.color.sheet))
        row.findViewById<TextView>(R.id.projectMeta).setTextColor(colorOf(R.color.sheet))
        row.findViewById<TextView>(R.id.projectName).text = project.name
        row.findViewById<TextView>(R.id.projectMeta).text =
            "${project.groupLabel} · ${project.members.size} 人 · 计划 ${project.planVersion}"
        val pending = project.pendingForViewer(Demo.VIEWER).size
        row.findViewById<TextView>(R.id.projectStatus).apply {
            text = when {
                pending > 0 -> "待确认 $pending"
                project.role == Role.CREATOR -> "我创建"
                else -> "已读"
            }
            setTextColor(colorOf(R.color.sheet))
        }
        row.setOnClickListener { tabHost()?.openProject(project.id) }
        return row
    }
}
