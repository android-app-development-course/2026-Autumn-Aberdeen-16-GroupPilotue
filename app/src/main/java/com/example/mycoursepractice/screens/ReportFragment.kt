package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Member
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.n1
import com.example.mycoursepractice.ui.pct1
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.roll
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.tnum

/** 结算单 — the settlement sheet of the tab bar. */
class ReportFragment : Fragment() {

    private lateinit var rootView: View
    private var lastVersion = -1
    private var shownTotal = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_report, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindAppSettings(view)
        bind(first = true)
    }

    override fun onResume() {
        super.onResume()
        if (lastVersion != Demo.version) bind(first = false)
        lastVersion = Demo.version
    }

    private fun bind(first: Boolean) {
        rootView.findViewById<TextView>(R.id.courseLabel).text = Demo.courseLabel
        rootView.findViewById<TextView>(R.id.groupLabel).text = Demo.groupLabel
        rootView.findViewById<TextView>(R.id.snapshotLabel).text = "快照 " + Demo.SNAPSHOT

        val total = Demo.total()
        val totalView = rootView.findViewById<TextView>(R.id.totalText)
        if (first || shownTotal == 0.0) {
            totalView.text = n1(total)
        } else {
            totalView.roll(shownTotal, total)
        }
        shownTotal = total

        rootView.findViewById<TextView>(R.id.confirmedLabel).text =
            "已确认 ${Demo.fullyConfirmedCount()} / ${Demo.items.size}"

        rootView.findViewById<TextView>(R.id.metaText).text = buildString {
            append(Demo.items.size)
            append(" 个工作项 · 总分 ")
            append(Demo.shareOfTotal().toInt())
            append(" 分 · 已计入 ")
            append(n1(Demo.confirmedShareOfTotal()))
            append("%")
        }

        buildComposition()
        buildMembers()

        val pending = Demo.pendingCount()
        rootView.findViewById<View>(R.id.pendingBox).show(pending > 0)
        if (pending > 0) {
            val mine = Demo.pendingForViewer().size
            rootView.findViewById<TextView>(R.id.pendingTitle).text =
                "$pending 项待确认" + if (mine > 0) " · $mine 项等你" else ""
            rootView.findViewById<View>(R.id.pendingAction).setOnClickListener {
                Demo.pendingForViewer().firstOrNull()?.let {
                    push(ItemDetailFragment.newInstance(it.id))
                }
            }
        }

        val zero = total <= 0.0
        rootView.findViewById<View>(R.id.zeroBox).show(zero)
        rootView.findViewById<TextView>(R.id.footerNote).text =
            "${Demo.RULE} · 占比不等同于课程成绩 · 快照 ${Demo.SNAPSHOT}"
    }

    private fun buildComposition() {
        val bar = rootView.findViewById<LinearLayout>(R.id.composition)
        bar.removeAllViews()
        val total = Demo.total()
        if (total <= 0.0) return

        val alphas = floatArrayOf(0.95f, 0.66f, 0.46f, 0.28f)
        Demo.members.forEachIndexed { index, member ->
            val ratio = Demo.ratioOf(member.id)
            if (ratio <= 0.0) return@forEachIndexed
            val cell = View(requireContext())
            cell.setBackgroundColor(colorOf(R.color.accent))
            cell.alpha = alphas[index % alphas.size]
            bar.addView(
                cell,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, ratio.toFloat())
            )
        }
    }

    private fun buildMembers() {
        val list = rootView.findViewById<LinearLayout>(R.id.memberRows)
        list.removeAllViews()

        val rows = mutableListOf<View>()
        Demo.members.forEach { member ->
            val row = layoutInflater.inflate(R.layout.row_member_settle, list, false)
            bindMemberRow(row, member)
            list.addView(row)
            rows.add(row)
            row.setOnClickListener {
                push(ScoreDetailFragment.newInstance(member.id))
            }
        }

        if (rows.isNotEmpty()) {
            Motion.stagger(rows.take(4), Motion.STAGGER_MS, 6f, 240L)
        }
    }

    private fun bindMemberRow(row: View, member: Member) {
        val score = Demo.scoreOf(member.id)
        val ratio = Demo.ratioOf(member.id)
        val items = Demo.shareItemsOf(member.id)
        val confirmed = items.count { it.accepted > 0 }

        val name = if (member.id == Demo.VIEWER) "${member.name} · 你" else member.name
        row.findViewById<TextView>(R.id.memberName).text = name
        row.findViewById<TextView>(R.id.memberScore).text = n1(score)
        row.findViewById<TextView>(R.id.memberPct).text = pct1(ratio)
        row.findViewById<TextView>(R.id.memberMeta).text =
            "${confirmed} / ${items.size} 项已确认"

        row.findViewById<TextView>(R.id.memberScore).tnum()
        if (member.id == Demo.VIEWER) {
            row.findViewById<TextView>(R.id.memberName).setTextColor(colorOf(R.color.accent))
        }

        val track = row.findViewById<FrameLayout>(R.id.memberTrack)
        val fill = row.findViewById<View>(R.id.memberFill)
        track.post {
            val width = (track.width * (ratio / 100.0)).toInt()
            fill.layoutParams = (fill.layoutParams as FrameLayout.LayoutParams).also {
                it.width = width.coerceAtLeast(if (ratio > 0) dp(2f) else 0)
            }
            fill.requestLayout()
        }
    }
}
