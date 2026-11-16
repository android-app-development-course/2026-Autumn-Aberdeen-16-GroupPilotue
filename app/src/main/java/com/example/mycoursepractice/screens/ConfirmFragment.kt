package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.n1
import com.example.mycoursepractice.ui.popBack
import com.example.mycoursepractice.ui.roll
import com.example.mycoursepractice.ui.sans
import com.example.mycoursepractice.ui.sp
import com.example.mycoursepractice.ui.tnum
import com.example.mycoursepractice.ui.widget.SegmentBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class ConfirmFragment : Fragment() {

    companion object {
        private const val ARG_ID = "id"
        private val STEPS = intArrayOf(0, 50, 100)

        fun newInstance(id: String) = ConfirmFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }

    private lateinit var rootView: View
    private var chosen = 100
    private var shown = 0.0
    private var committed = false

    private val item get() = Demo.item(requireArguments().getString(ARG_ID)!!)

    private val segment: SegmentBar get() = rootView.findViewById(R.id.segment)
    private val scoreText: TextView get() = rootView.findViewById(R.id.scoreText)
    private val formulaText: TextView get() = rootView.findViewById(R.id.formulaText)
    private val cta: TextView get() = rootView.findViewById(R.id.cta)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_confirm, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(rootView, "完成确认")
        val item = item

        rootView.findViewById<TextView>(R.id.itemLabel).text =
            "工作项 " + String.format("%02d", item.no)
        rootView.findViewById<TextView>(R.id.titleText).text = item.title

        val evidenceNote = item.evidence?.let { " · 附件 ${it.title}" } ?: ""
        rootView.findViewById<TextView>(R.id.metaText).text = buildString {
            append(Demo.nameOf(item.ownerId))
            append(" 提交 · ")
            append(item.points)
            append(" 分 · ")
            append(Demo.shareSummary(item))
            append(evidenceNote)
        }

        chosen = if (item.accepted in STEPS) item.accepted else 100

        segment.configure(
            main = listOf("0%", "50%", "100%"),
            sub = listOf("未达标", "部分达标", "达标"),
            initial = STEPS.indexOf(chosen)
        ) { index ->
            chosen = STEPS[index]
            updateScore(animate = true)
        }

        updateScore(animate = false)

        cta.setOnClickListener { commit() }
        Motion.rise(scoreText, 60L, 8f, 260L)
    }

    private fun updateScore(animate: Boolean) {
        val item = item
        val total = item.points * chosen / 100.0

        if (animate) {
            scoreText.roll(shown, total)
        } else {
            scoreText.text = n1(total)
        }
        shown = total

        scoreText.setTextColor(colorOf(if (chosen == 0) R.color.line else R.color.ink))
        formulaText.text = String.format(Locale.US, "%.1f × %d%% = %.1f", item.points.toDouble(), chosen, total)

        val shares = item.shares.entries
            .sortedByDescending { it.value }
            .joinToString("  ") { "${Demo.nameOf(it.key)} ${(it.value * 100).roundToInt()}%" }
        rootView.findViewById<TextView>(R.id.shareText).text = "按分摊计入 · $shares"

        cta.text = "确认 · $chosen%"
        bindImpact(item)
    }

    /** The three numbers this one tap moves — the reason the screen exists. */
    private fun bindImpact(item: com.example.mycoursepractice.data.WorkItem) {
        val rows = rootView.findViewById<LinearLayout>(R.id.impactRows)
        val owner = Demo.nameOf(item.ownerId)
        val ownerScore = item.points * chosen / 100.0 * item.shareOf(item.ownerId)
        val totalNow = Demo.total()
        val pendingNow = Demo.pendingForViewer().size

        val lines = listOf(
            Triple(owner + " 的积分", n1(item.confirmedPoints), n1(ownerScore)),
            Triple("小组总分", n1(totalNow), n1(totalNow - item.confirmedPoints + item.points * chosen / 100.0)),
            Triple("待确认", "$pendingNow 项", "${(pendingNow - 1).coerceAtLeast(0)} 项")
        )

        if (rows.childCount == 0) {
            lines.forEach { rows.addView(impactRow()) }
        }
        lines.forEachIndexed { index, (label, from, to) ->
            val row = rows.getChildAt(index) as LinearLayout
            (row.getChildAt(0) as TextView).text = label
            val value = row.getChildAt(1) as TextView
            value.text = if (from == to) "$from · 不变" else "$from → $to"
            value.setTextColor(colorOf(if (from == to) R.color.muted else R.color.ink))
        }
    }

    private fun impactRow(): LinearLayout = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(6f), 0, dp(6f))
        addView(TextView(requireContext()).apply {
            sans(); sp(13f)
            setTextColor(colorOf(R.color.muted))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        addView(TextView(requireContext()).apply {
            sans(); tnum(); sp(13f)
            setTextColor(colorOf(R.color.ink))
        })
    }

    private fun commit() {
        if (committed) return
        committed = true

        val item = item
        item.accepted = chosen
        item.stage = Stage.DONE
        item.confirmerId = Demo.VIEWER
        item.confirmedAt = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date())
        Demo.bump()

        Motion.stamp(cta)
        cta.postDelayed({ if (isAdded) popBack() }, 160)
    }
}
