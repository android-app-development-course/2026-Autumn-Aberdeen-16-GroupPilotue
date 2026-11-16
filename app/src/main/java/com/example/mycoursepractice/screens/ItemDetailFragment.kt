package com.example.mycoursepractice.screens

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.EvidenceKind
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.data.WorkItem
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.n1
import com.example.mycoursepractice.ui.pct1
import com.example.mycoursepractice.ui.popBack
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.showIf
import kotlin.math.roundToInt

class ItemDetailFragment : Fragment() {

    companion object {
        private const val ARG_ID = "id"
        fun newInstance(id: String) = ItemDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }

    private lateinit var rootView: View
    private var lastVersion = -1

    private val id: String get() = requireArguments().getString(ARG_ID)!!
    private val item: WorkItem get() = Demo.item(id)

    private val indexText: TextView get() = rootView.findViewById(R.id.indexText)
    private val titleText: TextView get() = rootView.findViewById(R.id.titleText)
    private val metaText: TextView get() = rootView.findViewById(R.id.metaText)
    private val stateStamp: TextView get() = rootView.findViewById(R.id.stateStamp)
    private val shareBar: LinearLayout get() = rootView.findViewById(R.id.shareBar)
    private val shareLegend: LinearLayout get() = rootView.findViewById(R.id.shareLegend)
    private val reqRows: LinearLayout get() = rootView.findViewById(R.id.reqRows)
    private val reqSource: TextView get() = rootView.findViewById(R.id.reqSource)
    private val evidenceBox: LinearLayout get() = rootView.findViewById(R.id.evidenceBox)
    private val scoreLabel: TextView get() = rootView.findViewById(R.id.scoreLabel)
    private val scoreText: TextView get() = rootView.findViewById(R.id.scoreText)
    private val formulaText: TextView get() = rootView.findViewById(R.id.formulaText)
    private val cta: TextView get() = rootView.findViewById(R.id.cta)
    private val correctionLink: TextView get() = rootView.findViewById(R.id.correctionLink)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_detail, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rootView.findViewById<View>(R.id.backBar).setOnClickListener { popBack() }
        bindAppSettings(rootView)
        indexText.transitionName = "item_${item.id}"
        bind()
        Motion.rise(indexText, 0L, 6f, 260L)
        Motion.rise(titleText, 40L, 6f, 260L)
        Motion.rise(metaText, 80L, 6f, 260L)
    }

    override fun onResume() {
        super.onResume()
        if (lastVersion != -1 && lastVersion != Demo.version) bind()
        lastVersion = Demo.version
    }

    // ── Bind ────────────────────────────────────────────────────────────

    private fun bind() {
        val item = item
        indexText.text = String.format("%02d", item.no)
        titleText.text = item.title
        metaText.text = buildString {
            append(Demo.nameOf(item.ownerId))
            append(" · ")
            append(item.points)
            append(" 分 · ")
            append(Demo.shareSummary(item))
        }

        bindStamp(item)
        bindShares(item)
        bindReqs(item)
        bindEvidence(item)
        bindScore(item)
        bindActions(item)
        bindEditLink(item)
        bindHistory(item)

        if (item.updatedAt != null && item.updatedBy != null) {
            metaText.text = metaText.text.toString() + " · 上次改动 " +
                item.updatedBy + " " + item.updatedAt
        }
    }

    /** 「编辑」只在发起人 / 负责人、且这一项还没被确认时出现。 */
    private fun bindEditLink(item: WorkItem) {
        val link = rootView.findViewById<TextView>(R.id.editLink)
        val canEdit = Demo.canEdit(item) && !Demo.locked(item)
        link.showIf(canEdit)
        if (!canEdit) return
        link.setOnClickListener { push(ItemEditFragment.edit(item.id)) }
    }

    private fun bindStamp(item: WorkItem) {
        // Only a verified state earns a stamp; in-flight work gets plain type.
        if (item.stage != Stage.DONE && item.stage != Stage.PENDING) {
            stateStamp.text = item.stage.label
            stateStamp.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            stateStamp.setTextColor(colorOf(R.color.muted))
            stateStamp.setPadding(0, 0, 0, 0)
            return
        }
        val pending = item.stage == Stage.PENDING
        stateStamp.text = if (pending) "待确认" else "已确认"
        stateStamp.setTextColor(colorOf(if (pending) R.color.ochre else R.color.accent))
        stateStamp.setBackgroundResource(if (pending) R.drawable.bg_stamp_ochre else R.drawable.bg_stamp)
        if (item.stage == Stage.DONE && item.confirmedAt != null) {
            metaText.text = metaText.text.toString() + " · " +
                Demo.nameOf(item.confirmerId) + " 确认于 " + item.confirmedAt
        }
    }

    private fun bindShares(item: WorkItem) {
        shareBar.removeAllViews()
        shareLegend.removeAllViews()

        val entries = item.shares.entries.sortedByDescending { it.value }
        entries.forEachIndexed { index, (memberId, share) ->
            val viewer = memberId == Demo.VIEWER
            shareBar.addView(View(requireContext()).apply {
                setBackgroundColor(colorOf(if (viewer) R.color.accent else R.color.ink_40))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, share.toFloat())
                    .apply { marginEnd = if (index == entries.lastIndex) 0 else dp(2f) }
            })

            val legend = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            legend.addView(View(requireContext()).apply {
                setBackgroundColor(colorOf(if (viewer) R.color.accent else R.color.ink_40))
                layoutParams = LinearLayout.LayoutParams(dp(6f), dp(6f))
            })
            legend.addView(TextView(requireContext()).apply {
                text = Demo.nameOf(memberId)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(colorOf(R.color.ink))
                typeface = Fonts.sans
                includeFontPadding = false
                setPadding(dp(6f), 0, 0, 0)
            })
            legend.addView(TextView(requireContext()).apply {
                text = pct1(share * 100.0)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(colorOf(R.color.muted))
                typeface = Fonts.sans
                fontFeatureSettings = "tnum"
                includeFontPadding = false
                setPadding(dp(5f), 0, 0, 0)
            })
            shareLegend.addView(legend)
        }
    }

    private fun bindReqs(item: WorkItem) {
        reqRows.removeAllViews()
        item.reqs.forEach { req ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(7f), 0, dp(7f))
            }
            row.addView(TextView(requireContext()).apply {
                text = req.label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                letterSpacing = 0.12f
                setTextColor(colorOf(R.color.muted))
                typeface = Fonts.sans
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(dp(56f), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            row.addView(TextView(requireContext()).apply {
                text = req.value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(colorOf(if (req.warn) R.color.ochre else R.color.ink))
                typeface = Fonts.sans
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (req.warn) {
                row.addView(TextView(requireContext()).apply {
                    text = "与要求不符"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setTextColor(colorOf(R.color.ochre))
                    typeface = Fonts.sans
                    includeFontPadding = false
                })
            }
            reqRows.addView(row)
        }
        reqSource.showIf(item.fromMaterial)
        reqSource.text = "来源 · 项目简介.pdf 第 2 页 · 由材料解析"
    }

    /** 改动记录：默认收起，一条一行。 */
    private fun bindHistory(item: WorkItem) {
        val link = rootView.findViewById<TextView>(R.id.historyLink)
        val rows = rootView.findViewById<LinearLayout>(R.id.historyRows)
        if (item.history.isEmpty()) {
            link.showIf(false)
            rows.showIf(false)
            return
        }
        link.showIf(true)
        link.text = "改动记录 " + item.history.size + " 条 ›"
        var open = false
        link.setOnClickListener {
            open = !open
            rows.showIf(open)
            link.text = if (open) "改动记录 " + item.history.size + " 条 ⌄"
            else "改动记录 " + item.history.size + " 条 ›"
        }
        rows.removeAllViews()
        item.history.forEach { edit ->
            rows.addView(TextView(requireContext()).apply {
                text = "${edit.at} · ${edit.by} · ${edit.field}  ${edit.from} → ${edit.to}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(colorOf(R.color.muted))
                typeface = Fonts.sans
                includeFontPadding = false
                setPadding(0, dp(6f), 0, dp(6f))
            })
        }
    }

    private fun bindEvidence(item: WorkItem) {
        evidenceBox.removeAllViews()
        val evidence = item.evidence
        if (evidence == null) {
            evidenceBox.addView(TextView(requireContext()).apply {
                text = "还没有提交成果"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(colorOf(R.color.ink))
                typeface = Fonts.sansMedium
                includeFontPadding = false
            })
            evidenceBox.addView(TextView(requireContext()).apply {
                text = "提交后确认人才能核对分摊与完成度"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(colorOf(R.color.muted))
                includeFontPadding = false
                setPadding(0, dp(6f), 0, 0)
            })
            return
        }

        val head = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(TextView(requireContext()).apply {
            text = evidence.kind.label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            letterSpacing = 0.1f
            setTextColor(colorOf(R.color.accent))
            typeface = Fonts.sansMedium
            includeFontPadding = false
            setPadding(0, 0, dp(12f), 0)
        })
        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        column.addView(TextView(requireContext()).apply {
            text = evidence.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(colorOf(R.color.ink))
            typeface = Fonts.sansMedium
            includeFontPadding = false
        })
        column.addView(TextView(requireContext()).apply {
            text = evidence.meta
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(colorOf(R.color.muted))
            typeface = Fonts.sans
            includeFontPadding = false
            setPadding(0, dp(4f), 0, 0)
        })
        head.addView(column)
        evidenceBox.addView(head)

        // Evidence strength: 3 slots — 文件 / 链接 / 文字
        val strengthRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14f), 0, 0)
        }
        repeat(3) { index ->
            strengthRow.addView(View(requireContext()).apply {
                setBackgroundColor(colorOf(if (index < evidence.strength) R.color.accent else R.color.line))
                layoutParams = LinearLayout.LayoutParams(0, dp(3f), 1f).apply {
                    marginEnd = if (index == 2) 0 else dp(3f)
                }
            })
        }
        strengthRow.addView(TextView(requireContext()).apply {
            text = "  证据强度 ${evidence.strength}/3"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colorOf(R.color.muted))
            typeface = Fonts.sans
            includeFontPadding = false
        })
        evidenceBox.addView(strengthRow)

        val divider = View(requireContext()).apply {
            setBackgroundColor(colorOf(R.color.line))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                topMargin = dp(14f)
                bottomMargin = dp(12f)
            }
        }
        evidenceBox.addView(divider)

        val locate = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        locate.addView(TextView(requireContext()).apply {
            text = locateLabel(evidence.kind)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(colorOf(R.color.accent))
            typeface = Fonts.sansMedium
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        locate.addView(TextView(requireContext()).apply {
            text = "›"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(colorOf(R.color.accent))
            includeFontPadding = false
        })
        evidenceBox.addView(locate)
        evidenceBox.setOnClickListener {
            push(EvidenceFragment.newInstance(item.id))
        }
    }

    private fun locateLabel(kind: EvidenceKind): String = when (kind) {
        EvidenceKind.FILE -> "定位到文档第 2 页"
        EvidenceKind.LINK -> "打开链接并定位"
        EvidenceKind.TEXT -> "查看文字说明原文"
    }

    private fun bindScore(item: WorkItem) {
        val memberId = if (item.shareOf(Demo.VIEWER) > 0.0) Demo.VIEWER else item.ownerId
        scoreLabel.text = if (memberId == Demo.VIEWER) "计入你的" else "计入${Demo.nameOf(memberId)}的"
        scoreText.text = n1(item.scoreOf(memberId))
        formulaText.text = Demo.formula(item, memberId)
        if (item.accepted == 0) {
            scoreText.setTextColor(colorOf(R.color.line))
            formulaText.text = "${item.points}.0 × 0% × ${(item.shareOf(memberId) * 100).roundToInt()}% = 0.0 · 尚未确认"
        } else {
            scoreText.setTextColor(colorOf(R.color.ink))
        }
    }

    private fun bindActions(item: WorkItem) {
        val canConfirm = item.stage == Stage.PENDING && item.ownerId != Demo.VIEWER
        val waitingOnMe = item.stage == Stage.PENDING && item.ownerId == Demo.VIEWER
        when {
            canConfirm -> {
                cta.text = "确认完成"
                cta.isEnabled = true
                cta.alpha = 1f
                cta.setOnClickListener { push(ConfirmFragment.newInstance(item.id)) }
            }
            waitingOnMe -> {
                cta.text = "已提交 · 等待 " + Demo.nameOf(Demo.confirmerFor(item)) + " 确认"
                cta.isEnabled = false
                cta.alpha = 0.55f
            }
            item.stage == Stage.DONE -> {
                cta.text = "查看结算单"
                cta.isEnabled = true
                cta.alpha = 1f
                cta.setOnClickListener { push(ReportFragment()) }
            }
            else -> {
                cta.text = "提交成果"
                cta.isEnabled = true
                cta.alpha = 1f
                cta.setOnClickListener { push(SubmitFragment.newInstance(item.id)) }
            }
        }
        correctionLink.text = "对分摊或完成度有异议？申请修正"
        correctionLink.setOnClickListener { push(CorrectionFragment.newInstance(item.id)) }
    }
}
