package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.sans
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.sp
import com.example.mycoursepractice.ui.tnum
import com.example.mycoursepractice.ui.track
import com.example.mycoursepractice.ui.widget.SegmentBar

/** 修正申请 — the only way to argue with a recorded number. */
class CorrectionFragment : Fragment() {

    companion object {
        fun newInstance(itemId: String = "w09") = CorrectionFragment().apply {
            arguments = Bundle().apply { putString("itemId", itemId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_correction, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val item = Demo.item(requireArguments().getString("itemId") ?: "w09")
        bindBackBar(view, "修正申请")

        view.findViewById<TextView>(R.id.kicker).text =
            "工作项 " + String.format("%02d", item.no)
        view.findViewById<TextView>(R.id.titleText).text = item.title
        view.findViewById<TextView>(R.id.metaText).text = buildString {
            append(Demo.nameOf(item.ownerId))
            append(" 提交 · ")
            append(item.points)
            append(" 分 · ")
            append(Demo.shareSummary(item))
        }

        view.findViewById<SegmentBar>(R.id.targetBar).configure(
            listOf("分摊", "完成度", "要求对照")
        ) { }

        val detail = view.findViewById<EditText>(R.id.detailField)
        val cta = view.findViewById<TextView>(R.id.cta)
        val stamp = view.findViewById<TextView>(R.id.trailingStamp)
        val note = view.findViewById<TextView>(R.id.footerNote)
        var submitted = false

        val owner = Demo.nameOf(item.ownerId)
        val other = item.shares.keys.firstOrNull { it != item.ownerId }
            ?.let { Demo.nameOf(it) }
        detail.setText(
            if (other != null) "分摊应改为 $owner 100%：$other 没有参与这一项的产出，依据是提交记录里只有一次编辑。"
            else "分摊无误，完成度应为 100%：提交后又补了第 9–12 页，附件已更新。"
        )
        detail.setSelection(detail.text.length)

        val steps = view.findViewById<LinearLayout>(R.id.steps)
        val step1 = stepRow("01", "提交修正申请", "待提交", R.color.muted)
        val step2 = stepRow("02", "发起人与被影响成员确认", "未开始", R.color.muted)
        val step3 = stepRow("03", "生效后回写结算单", "未开始", R.color.muted)
        steps.addView(step1.first)
        steps.addView(step2.first)
        steps.addView(step3.first)

        cta.setOnClickListener {
            if (submitted) return@setOnClickListener
            if (detail.text.trim().length < 6) {
                Toast.makeText(requireContext(), "请写清楚哪一格不对", Toast.LENGTH_SHORT).show()
                detail.requestFocus()
                requireContext().getSystemService<InputMethodManager>()
                    ?.showSoftInput(detail, InputMethodManager.SHOW_IMPLICIT)
                return@setOnClickListener
            }
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(detail.windowToken, 0)

            submitted = true
            stamp.text = "未决"
            stamp.show(true)
            stamp.setTextColor(colorOf(R.color.ochre))
            stamp.setBackgroundResource(R.drawable.bg_stamp_ochre)
            Motion.stamp(stamp)

            cta.text = "已提交 · 等待确认"
            cta.isEnabled = false
            cta.alpha = 0.55f
            detail.isEnabled = false
            note.text = "修正已提交 · 未决期间不改动结算单"
            step1.second.text = "已提交"
            step1.second.setTextColor(colorOf(R.color.accent))
            step2.second.text = "未决"
            step2.second.setTextColor(colorOf(R.color.ochre))
        }
    }

    /** A single numbered line of the correction trail; returns the row and its state label. */
    private fun stepRow(
        index: String,
        title: String,
        state: String,
        stateColor: Int
    ): Pair<LinearLayout, TextView> {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(7f), 0, dp(7f))
        }
        row.addView(TextView(requireContext()).apply {
            text = index
            sans(); tnum(); sp(11f)
            setTextColor(colorOf(R.color.muted))
            layoutParams = LinearLayout.LayoutParams(dp(30f), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(requireContext()).apply {
            text = title
            sans(); sp(13f)
            setTextColor(colorOf(R.color.ink))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val stateView = TextView(requireContext()).apply {
            text = state
            sans(); track(0.1f); sp(10f)
            setTextColor(colorOf(stateColor))
        }
        row.addView(stateView)
        return row to stateView
    }
}
