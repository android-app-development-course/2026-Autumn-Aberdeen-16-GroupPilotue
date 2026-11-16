package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.show

/** 计划确认 — v4 preview: what changes, who carries it, then one stamp. */
class PlanFragment : Fragment() {

    private class Change(val mark: String, val text: String, val meta: String, val warn: Boolean)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_plan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(view, "计划确认")
        view.findViewById<TextView>(R.id.kicker).text = "计划 v4 · 由 何嘉俊 发起"
        view.findViewById<TextView>(R.id.titleText).text = "本周计划调整"
        view.findViewById<TextView>(R.id.metaText).text =
            "4 项变更 · 11-13 20:14 提交 · 总分 100 分不变"

        val changes = listOf(
            Change("+", "新增「可用性测试与分析」", "8 分 · 陈实 60 / 李文豪 40", false),
            Change("~", "「汇报 PPT」6 分 → 8 分", "何嘉俊 100 · 答辩前多一轮合稿", false),
            Change("~", "「演示视频剪辑」6 分 → 4 分", "陈实 100 · 改为录屏，剪辑量减少", false),
            Change("−", "移除「数据回收与复盘」8 分", "并入可用性测试，不再单独计分", true)
        )

        val diffRows = view.findViewById<LinearLayout>(R.id.diffRows)
        val views = mutableListOf<View>()
        changes.forEach { change ->
            val row = layoutInflater.inflate(R.layout.row_diff, diffRows, false)
            row.findViewById<TextView>(R.id.diffMark).apply {
                text = change.mark
                setTextColor(colorOf(if (change.warn) R.color.ochre else R.color.accent))
            }
            row.findViewById<TextView>(R.id.diffText).text = change.text
            row.findViewById<TextView>(R.id.diffMeta).text = change.meta
            diffRows.addView(row)
            views.add(row)
        }

        val shareRows = view.findViewById<LinearLayout>(R.id.shareRows)
        listOf(
            Triple("8 分", "可用性测试与分析", "陈实 60 · 李文豪 40"),
            Triple("8 分", "汇报 PPT", "何嘉俊 100"),
            Triple("4 分", "演示视频剪辑", "陈实 100")
        ).forEach { (points, name, shares) ->
            val row = layoutInflater.inflate(R.layout.row_req, shareRows, false)
            row.findViewById<TextView>(R.id.reqLabel).text = points
            row.findViewById<TextView>(R.id.reqValue).text = "$name · $shares"
            shareRows.addView(row)
            views.add(row)
        }

        val cta = view.findViewById<TextView>(R.id.cta)
        val stamp = view.findViewById<TextView>(R.id.trailingStamp)
        val note = view.findViewById<TextView>(R.id.footerNote)
        var confirmed = false

        cta.setOnClickListener {
            if (confirmed) return@setOnClickListener
            confirmed = true
            cta.text = "已确认 · 计划生效"
            cta.isEnabled = false
            cta.alpha = 0.55f
            stamp.text = "已确认"
            stamp.show(true)
            Motion.stamp(stamp)
            note.text = "计划 v4 已生效 · 已完成的确认不会被改写"
        }

        Motion.stagger(views, Motion.STAGGER_MS, 6f, 240L)
    }
}
