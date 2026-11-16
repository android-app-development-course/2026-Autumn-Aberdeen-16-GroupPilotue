package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.show

/** 收件箱 — a reminder list, not a second board. */
class InboxFragment : Fragment() {

    private lateinit var rootView: View
    private var lastVersion = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_inbox, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindAppSettings(rootView)
        bind()
    }

    override fun onResume() {
        super.onResume()
        if (lastVersion != -1 && lastVersion != Demo.version) bind()
        lastVersion = Demo.version
    }

    private fun bind() {
        rootView.findViewById<TextView>(R.id.courseLabel).text = Demo.courseLabel
        rootView.findViewById<TextView>(R.id.groupLabel).text = Demo.groupLabel

        val pending = Demo.pendingForViewer().sortedBy { it.no }
        val times = listOf("2 小时前", "昨天", "11-13")
        val rows = rootView.findViewById<LinearLayout>(R.id.inboxRows)
        rows.removeAllViews()

        val views = mutableListOf<View>()
        pending.forEachIndexed { index, item ->
            val row = layoutInflater.inflate(R.layout.row_inbox, rows, false)
            row.findViewById<TextView>(R.id.inboxKind).text = "待确认"
            row.findViewById<TextView>(R.id.inboxKind).setTextColor(colorOf(R.color.accent))
            row.findViewById<TextView>(R.id.inboxTitle).text =
                String.format("%02d", item.no) + " " + item.title
            row.findViewById<TextView>(R.id.inboxMeta).text = buildString {
                append(Demo.nameOf(item.ownerId))
                append(" 提交 · ")
                append(item.points)
                append(" 分 · ")
                append(Demo.shareSummary(item))
            }
            row.findViewById<TextView>(R.id.inboxTime).text =
                times.getOrElse(index) { "11-12" }
            row.setOnClickListener { push(ItemDetailFragment.newInstance(item.id)) }
            rows.addView(row)
            views.add(row)
            if (index < pending.size - 1) rows.addView(divider())
        }

        val plan = layoutInflater.inflate(R.layout.row_inbox, rows, false)
        plan.findViewById<TextView>(R.id.inboxKind).text = "计划"
        plan.findViewById<TextView>(R.id.inboxKind).setTextColor(colorOf(R.color.ochre))
        plan.findViewById<TextView>(R.id.inboxTitle).text = "计划 v4 等你确认"
        plan.findViewById<TextView>(R.id.inboxMeta).text = "4 项变更 · 总分 100 分不变"
        plan.findViewById<TextView>(R.id.inboxTime).text = "11-13"
        plan.setOnClickListener { push(PlanFragment()) }
        if (pending.isNotEmpty()) rows.addView(divider())
        rows.addView(plan)
        views.add(plan)

        rootView.findViewById<TextView>(R.id.unreadLabel).text = "${views.size} 条未读"
        rootView.findViewById<View>(R.id.emptyBox).show(views.isEmpty())

        if (views.isNotEmpty()) Motion.stagger(views, Motion.STAGGER_MS, 6f, 240L)
    }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
        setBackgroundColor(colorOf(R.color.line))
    }
}
