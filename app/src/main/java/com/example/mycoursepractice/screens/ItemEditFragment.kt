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
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.data.WorkItem
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.popBack
import com.example.mycoursepractice.ui.sans
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.sp
import com.example.mycoursepractice.ui.tnum
import com.example.mycoursepractice.ui.track
import com.example.mycoursepractice.ui.widget.SegmentBar
import kotlin.math.roundToInt

/**
 * 新建 / 编辑一个工作项。两种模式共用一页：
 * 新建时它只是「账本上再添一行」，编辑时它改的是同一行。
 */
class ItemEditFragment : Fragment() {

    companion object {
        fun create() = ItemEditFragment()
        fun edit(itemId: String) = ItemEditFragment().apply {
            arguments = Bundle().apply { putString("itemId", itemId) }
        }
    }

    private val percentFields = LinkedHashMap<String, EditText>()
    private var ownerId: String = Demo.VIEWER
    private var editing: WorkItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_item_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val argId = arguments?.getString("itemId")
        val item = argId?.let { Demo.item(it) }
        editing = item
        ownerId = item?.ownerId ?: Demo.VIEWER

        bindBackBar(view, if (item == null) "新建工作项" else "编辑 " + "%02d".format(item.no))

        val titleField = view.findViewById<EditText>(R.id.titleField)
        val pointsField = view.findViewById<EditText>(R.id.pointsField)
        val pointsHint = view.findViewById<TextView>(R.id.pointsHint)
        val placeField = view.findViewById<EditText>(R.id.placeField)
        val dueField = view.findViewById<EditText>(R.id.dueField)
        val noteField = view.findViewById<EditText>(R.id.noteField)
        val cta = view.findViewById<TextView>(R.id.cta)
        val warn = view.findViewById<TextView>(R.id.warnText)
        val deleteLink = view.findViewById<TextView>(R.id.deleteLink)
        val footerNote = view.findViewById<TextView>(R.id.footerNote)

        view.findViewById<TextView>(R.id.kicker).text =
            if (item == null) "在账本上添一行" else "改这一行 · 编号 " + "%02d".format(item.no)
        view.findViewById<TextView>(R.id.titleText).text =
            if (item == null) "新建工作项" else item.title
        view.findViewById<TextView>(R.id.metaText).text =
            if (item == null) "编号会自动分配到 " + "%02d".format(Demo.nextItemNo()) + "，填完就能进板"
            else "${Demo.nameOf(item.ownerId)} · ${item.points} 分 · ${Demo.shareSummary(item)}"

        // ── 负责人 ────────────────────────────────────────────────────────
        val chips = view.findViewById<LinearLayout>(R.id.ownerChips)
        val chipViews = LinkedHashMap<String, TextView>()
        Demo.members.forEach { m ->
            val chip = TextView(requireContext()).apply {
                text = m.name
                sans(); sp(12f)
                setPadding(dp(14f), dp(9f), dp(14f), dp(9f))
                setOnClickListener {
                    ownerId = m.id
                    chipViews.forEach { (id, v) -> paintChip(v, id == ownerId) }
                    chips.post { chips.requestLayout() }
                }
            }
            paintChip(chip, m.id == ownerId)
            chipViews[m.id] = chip
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8f)
            chips.addView(chip, lp)
        }

        // ── 分值 ──────────────────────────────────────────────────────────
        fun paintHint() {
            val points = pointsField.text.toString().toIntOrNull() ?: 0
            val allocated = Demo.allocatedPoints() - (item?.points ?: 0) + points
            val plan = Demo.current.totalPoints.toInt()
            pointsHint.text = if (allocated <= plan) {
                "已分配 $allocated / $plan · 还剩 ${plan - allocated}"
            } else {
                "计划 $plan 分 · 含这一项共 $allocated 分，结算单按实际总量算"
            }
            pointsHint.setTextColor(colorOf(R.color.muted))
        }
        pointsField.setText((item?.points ?: 5).toString())
        paintHint()
        pointsField.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = paintHint()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // ── 阶段 ──────────────────────────────────────────────────────────
        val stageBar = view.findViewById<SegmentBar>(R.id.stageBar)
        val stages = mutableListOf(Stage.NOT_STARTED, Stage.DOING)
        item?.stage?.let { if (it !in stages) stages.add(it) }
        var stage = item?.stage ?: Stage.NOT_STARTED
        stageBar.configure(stages.map { it.label }, null, stages.indexOf(stage)) { stage = stages[it] }

        // ── 交付形式 ──────────────────────────────────────────────────────
        val formBar = view.findViewById<SegmentBar>(R.id.formBar)
        val forms = listOf("文件", "链接", "文字")
        var form = item?.form?.takeIf { it.isNotBlank() } ?: forms[0]
        formBar.configure(forms, null, forms.indexOf(form).coerceAtLeast(0)) { form = forms[it] }

        // ── 分摊 ──────────────────────────────────────────────────────────
        val rows = view.findViewById<LinearLayout>(R.id.shareRows)
        Demo.members.forEach { m ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(2f), 0, dp(2f))
            }
            row.addView(TextView(requireContext()).apply {
                text = m.name
                sans(); sp(13f)
                setTextColor(colorOf(if (m.id == Demo.VIEWER) R.color.accent else R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val field = EditText(requireContext()).apply {
                setText(
                    ((item?.shareOf(m.id) ?: if (m.id == ownerId) 1.0 else 0.0) * 100).roundToInt()
                        .toString()
                )
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(android.text.InputFilter.LengthFilter(3))
                gravity = android.view.Gravity.END
                sans(); tnum(); sp(14f)
                setTextColor(colorOf(R.color.ink))
                background = resources.getDrawable(R.drawable.bg_field_line, null)
                setPadding(0, dp(6f), 0, dp(8f))
                layoutParams = LinearLayout.LayoutParams(dp(64f), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            percentFields[m.id] = field
            row.addView(field)
            row.addView(TextView(requireContext()).apply {
                text = "%"
                sans(); sp(12f)
                setTextColor(colorOf(R.color.muted))
                setPadding(dp(6f), 0, 0, 0)
            })
            rows.addView(row)
        }

        // ── 其余字段 ──────────────────────────────────────────────────────
        titleField.setText(item?.title ?: "")
        placeField.setText(item?.location ?: "")
        dueField.setText(item?.due ?: "")
        noteField.setText(item?.note ?: "")

        cta.text = if (item == null) "添加到板" else "保存修改"
        footerNote.text = if (item == null)
            "添加到板后，其他成员会看到这一行" else
            "改动会记在「改动记录」里，计分立即重算"

        val locked = item != null && Demo.locked(item)
        if (locked) {
            listOf<View>(
                titleField, pointsField, placeField, dueField, noteField, stageBar, formBar
            ).forEach { it.isEnabled = false }
            percentFields.values.forEach { it.isEnabled = false }
            chipViews.values.forEach { it.isEnabled = false; it.alpha = 0.5f }
            cta.isEnabled = false
            cta.alpha = 0.55f
            cta.text = "已确认 · 不可修改"
            footerNote.text = "已确认的任务锁定；要改分数请走修正申请"
            warn.text = "这一项已有确认记录，计分字段不能再改。"
            warn.show(true)
        } else if (item != null && !Demo.canEdit(item)) {
            listOf<View>(pointsField).forEach { it.isEnabled = false }
            percentFields.values.forEach { it.isEnabled = false }
            warn.text = "只有发起人或这项的负责人能改；你可以提交修正申请。"
            warn.show(true)
            cta.isEnabled = false
            cta.alpha = 0.55f
        } else if (item != null && item.stage == Stage.PENDING) {
            // 待确认：证据已交、等确认，这时只允许改描述性字段。
            pointsField.isEnabled = false
            percentFields.values.forEach { it.isEnabled = false }
            chipViews.values.forEach { it.isEnabled = false; it.alpha = 0.5f }
            stageBar.isEnabled = false
            warn.text = "待确认期间只能改标题、位置、截止与说明；分值、分摊和负责人已锁定。"
            warn.show(true)
            footerNote.text = "确认后再改分数，请走修正申请"
        }

        if (item != null && Demo.canDelete(item)) {
            deleteLink.show(true)
            deleteLink.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除这一项？")
                    .setMessage("删除后编号不回填，" + "%02d".format(item.no) + " 会空着。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        Demo.deleteItem(item)
                        popBack()
                    }
                    .show()
            }
        }

        cta.setOnClickListener {
            val title = titleField.text.toString().trim()
            if (title.isEmpty()) return@setOnClickListener fail(warn, titleField, "标题不能为空")
            val points = pointsField.text.toString().toIntOrNull()
                ?: return@setOnClickListener fail(warn, pointsField, "分值要填数字")
            if (points < 1) return@setOnClickListener fail(warn, pointsField, "分值至少 1")

            val shares = LinkedHashMap<String, Double>()
            var sum = 0
            percentFields.forEach { (id, f) ->
                val p = f.text.toString().toIntOrNull() ?: 0
                if (p > 0) {
                    shares[id] = p / 100.0
                    sum += p
                }
            }
            if (shares.isEmpty()) return@setOnClickListener fail(warn, null, "至少给一个人分摊")
            if (sum != 100) return@setOnClickListener fail(warn, null, "分摊加起来要是 100%（现在是 $sum%）")

            if (shares.keys.none { it == ownerId }) {
                return@setOnClickListener fail(warn, null, "负责人不在分摊里，先给他分一点")
            }

            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(cta.windowToken, 0)

            val due = dueField.text.toString().trim()
            val place = placeField.text.toString().trim()
            val note = noteField.text.toString().trim()
            if (item == null) {
                Demo.createItem(title, ownerId, points, shares, stage, due, form, place, note)
            } else {
                Demo.updateItem(item, title, ownerId, points, shares, stage, due, form, place, note)
            }
            Motion.stamp(cta)
            cta.postDelayed({ popBack() }, 150)
        }
    }

    private fun fail(warn: TextView, field: EditText?, text: String) {
        warn.text = text
        warn.show(true)
        field?.requestFocus()
    }

    private fun paintChip(chip: TextView, selected: Boolean) {
        chip.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
        chip.setTextColor(colorOf(if (selected) R.color.sheet else R.color.ink))
    }
}
