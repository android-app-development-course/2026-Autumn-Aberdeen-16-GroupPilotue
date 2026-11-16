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
import com.example.mycoursepractice.data.WorkItem
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.n1
import com.example.mycoursepractice.ui.pct1
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.tnum

/** 成员明细 — every point of one member, line by line, with the formula shown. */
class ScoreDetailFragment : Fragment() {

    companion object {
        private const val ARG_MEMBER = "member"

        fun newInstance(memberId: String) = ScoreDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_MEMBER, memberId) }
        }
    }

    private lateinit var rootView: View
    private val memberId: String get() = requireArguments().getString(ARG_MEMBER)!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_score_detail, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val member = Demo.member(memberId)
        bindBackBar(rootView, "成员明细")

        rootView.findViewById<TextView>(R.id.kicker).text = Demo.groupLabel
        rootView.findViewById<TextView>(R.id.initialText).text = member.initial
        rootView.findViewById<TextView>(R.id.memberName).text = buildString {
            append(member.name)
            if (member.id == Demo.VIEWER) append(" · 你")
        }

        val score = Demo.scoreOf(member.id)
        rootView.findViewById<TextView>(R.id.scoreText).apply {
            text = n1(score)
            tnum()
        }
        rootView.findViewById<TextView>(R.id.ratioText).text = "占比 " + pct1(Demo.ratioOf(member.id))

        val items = Demo.shareItemsOf(member.id).sortedBy { it.no }
        val confirmed = items.filter { it.accepted > 0 }
        val open = items.filter { it.accepted == 0 }

        rootView.findViewById<TextView>(R.id.countText).text =
            "${confirmed.size} / ${items.size} 项已确认" +
                if (open.isEmpty()) "" else " · ${open.size} 项待确认"
        rootView.findViewById<TextView>(R.id.lineCount).text = "${confirmed.size} 项"
        rootView.findViewById<TextView>(R.id.footerNote).text =
            "快照 ${Demo.SNAPSHOT} · ${Demo.RULE}"

        val rows = mutableListOf<View>()
        val confirmedList = rootView.findViewById<LinearLayout>(R.id.lineRows)
        confirmedList.removeAllViews()
        confirmed.forEach { item ->
            val row = lineRow(item, filled = true)
            confirmedList.addView(row)
            rows.add(row)
            row.setOnClickListener { Motion.settle(row) }
        }

        val openList = rootView.findViewById<LinearLayout>(R.id.openRows)
        openList.removeAllViews()
        rootView.findViewById<View>(R.id.openBlock).show(open.isNotEmpty())
        open.forEach { item ->
            val row = lineRow(item, filled = false)
            openList.addView(row)
            rows.add(row)
        }

        if (rows.isNotEmpty()) {
            Motion.stagger(rows, Motion.STAGGER_MS, 6f, 240L)
        }
    }

    private fun lineRow(item: WorkItem, filled: Boolean): View {
        val row = layoutInflater.inflate(R.layout.row_score_line, null, false)
        row.findViewById<TextView>(R.id.lineNo).text = String.format("%02d", item.no)
        row.findViewById<TextView>(R.id.lineTitle).text = item.title

        val score = row.findViewById<TextView>(R.id.lineScore)
        val formula = row.findViewById<TextView>(R.id.lineFormula)

        if (filled) {
            score.text = n1(item.scoreOf(memberId))
            formula.text = Demo.formula(item, memberId)
        } else {
            score.text = "—"
            score.setTextColor(colorOf(R.color.line))
            formula.text = "待确认 · 若 100% 则 ${n1(item.points * item.shareOf(memberId))}"
            formula.setTextColor(colorOf(R.color.ochre))
        }
        return row
    }
}
