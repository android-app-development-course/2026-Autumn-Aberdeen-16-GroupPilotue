package com.example.mycoursepractice.screens

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.sans
import com.example.mycoursepractice.ui.serif
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.sp
import com.example.mycoursepractice.ui.tnum
import com.example.mycoursepractice.ui.track
import com.example.mycoursepractice.ui.bindBackBar

/** 要求对照 — what the brief asked for, and where each line came from. */
class RequirementFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_requirement, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(view, "要求对照")
        view.findViewById<TextView>(R.id.sourceLabel).text = "解析来源 · 项目简介.pdf"
        view.findViewById<TextView>(R.id.titleText).text = "作业 2 的要求"
        view.findViewById<TextView>(R.id.metaText).text =
            "11-02 14:08 解析 · 6 条要求 · 由材料解析"

        val groups = view.findViewById<LinearLayout>(R.id.groups)
        val specs = listOf(
            Triple(
                "w10", 2,
                "The presentation must be submitted as PDF or PPTX to Moodle " +
                    "before 12:00 noon (UK time) on Friday 21 November."
            ),
            Triple(
                "w11", 3,
                "Recruit at least five pilot participants and record one short clip " +
                    "per session; clips must be uploaded with the group submission."
            )
        )

        specs.forEachIndexed { index, (id, page, quote) ->
            val item = Demo.item(id)
            groups.addView(groupHeader(item.no, item.title, index == 0))
            item.reqs.forEach { req ->
                val row = layoutInflater.inflate(R.layout.row_req, groups, false)
                row.findViewById<TextView>(R.id.reqLabel).text = req.label
                row.findViewById<TextView>(R.id.reqValue).apply {
                    text = req.value
                    if (req.warn) setTextColor(colorOf(R.color.ochre))
                }
                row.findViewById<TextView>(R.id.reqFlag).show(req.warn)
                groups.addView(row)
            }
            groups.addView(quoteBlock(page, quote))
            groups.addView(divider())
        }
    }

    private fun groupHeader(no: Int, title: String, first: Boolean): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, if (first) dp(26f) else dp(22f), 0, dp(6f))

            addView(TextView(requireContext()).apply {
                text = String.format("%02d", no)
                sans(); tnum(); sp(12f)
                setTextColor(colorOf(R.color.accent))
                layoutParams = LinearLayout.LayoutParams(dp(40f), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(requireContext()).apply {
                text = title
                sans(medium = true); track(0.04f); sp(12f)
                setTextColor(colorOf(R.color.ink))
            })
        }

    private fun quoteBlock(page: Int, quote: String): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(62f), dp(8f), 0, dp(14f))

            addView(TextView(requireContext()).apply {
                text = "第 $page 页 原文"
                sans(); track(0.12f); sp(10f)
                setTextColor(colorOf(R.color.muted))
            })
            addView(TextView(requireContext()).apply {
                text = "“$quote”"
                serif(); sp(13f)
                setLineSpacing(0f, 1.45f)
                setTextColor(colorOf(R.color.ink))
                typeface = Typeface.create("serif", Typeface.ITALIC)
                setPadding(0, dp(4f), 0, 0)
            })
        }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
        setBackgroundColor(colorOf(R.color.line))
    }
}
