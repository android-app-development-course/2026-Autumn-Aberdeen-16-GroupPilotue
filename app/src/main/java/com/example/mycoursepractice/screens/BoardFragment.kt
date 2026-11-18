package com.example.mycoursepractice.screens

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.data.WorkItem
import com.example.mycoursepractice.ui.action
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.hairline
import com.example.mycoursepractice.ui.n1
import com.example.mycoursepractice.ui.pct1
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.showIf
import com.example.mycoursepractice.ui.tabHost

class BoardFragment : Fragment() {

    private lateinit var rootView: View

    private var filter: Stage? = null
    private var tasksCollapsed = false
    private var lastVersion = -1

    private val tickCells = mutableMapOf<Stage, Triple<View, TextView, TextView>>()

    private val scroll: ScrollView get() = rootView.findViewById(R.id.scroll)
    private val tickBar: LinearLayout get() = rootView.findViewById(R.id.tickBar)
    private val memberRail: LinearLayout get() = rootView.findViewById(R.id.memberRail)
    private val sections: LinearLayout get() = rootView.findViewById(R.id.sections)
    private val pendingStrip: View get() = rootView.findViewById(R.id.pendingStrip)
    private val pendingText: TextView get() = rootView.findViewById(R.id.pendingText)
    private val filterHint: TextView get() = rootView.findViewById(R.id.filterHint)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_board, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rootView.findViewById<TextView>(R.id.courseLabel).text = Demo.courseLabel
        rootView.findViewById<TextView>(R.id.groupLabel).text = Demo.groupLabel
        rootView.findViewById<TextView>(R.id.planLabel).text = Demo.planLabel
        rootView.findViewById<TextView>(R.id.screenTitle).apply {
            text = Demo.projectName
            setOnClickListener { tabHost()?.openLibrary() }
        }
        rootView.findViewById<View>(R.id.screenTitle).setOnClickListener {
            tabHost()?.openLibrary()
        }
        rootView.findViewById<View>(R.id.settingsLink).setOnClickListener {
            push(ProjectSettingsFragment())
        }
        rootView.findViewById<View>(R.id.addItem).setOnClickListener {
            push(ItemEditFragment.create())
        }
        bindAppSettings(rootView)

        buildTicks()
        rebuild(animate = true)

        pendingStrip.setOnClickListener {
            Demo.pendingForViewer().firstOrNull()?.let { open(it, pendingStrip) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (lastVersion != -1 && lastVersion != Demo.version) {
            rebuild(animate = false)
        }
        lastVersion = Demo.version
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tickCells.clear()
    }

    // ── Stage counters ──────────────────────────────────────────────────

    private fun buildTicks() {
        tickBar.removeAllViews()
        tickCells.clear()
        Stage.values().forEachIndexed { index, stage ->
            val cell = layoutInflater.inflate(R.layout.cell_tick, tickBar, false)
            val rule = cell.findViewById<View>(R.id.tickRule)
            val count = cell.findViewById<TextView>(R.id.tickCount)
            val label = cell.findViewById<TextView>(R.id.tickLabel)
            label.text = stage.label

            if (index == 0) {
                cell.findViewById<View>(R.id.tickDivider).visibility = View.INVISIBLE
            }

            cell.setOnClickListener {
                filter = if (filter == stage) null else stage
                rebuild(animate = true)
                scroll.smoothScrollTo(0, 0)
            }

            tickBar.addView(cell)
            tickCells[stage] = Triple(rule, count, label)
        }
        refreshTicks()
    }

    private fun refreshTicks() {
        Stage.values().forEach { stage ->
            val cell = tickCells[stage] ?: return@forEach
            cell.second.text = Demo.itemsOf(stage).size.toString()
            val selected = filter == stage
            cell.first.setBackgroundColor(
                if (selected) colorOf(R.color.accent) else Color.TRANSPARENT
            )
            cell.second.setTextColor(colorOf(if (selected) R.color.accent else R.color.ink))
            cell.third.setTextColor(colorOf(if (selected) R.color.accent else R.color.muted))
        }
        filterHint.showIf(filter != null)
        filter?.let { filterHint.text = "筛选 · ${it.label}　再点一次取消" }
    }

    // ── Member rail ─────────────────────────────────────────────────────

    private fun buildMembers() {
        memberRail.removeAllViews()
        memberRail.gravity = if (Demo.members.size <= 2) Gravity.START else Gravity.CENTER_VERTICAL
        Demo.members.forEach { member ->
            val cell = layoutInflater.inflate(R.layout.cell_member, memberRail, false)
            if (Demo.members.size <= 2) {
                cell.layoutParams = LinearLayout.LayoutParams(dp(96f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(12f)
                }
            }
            cell.findViewById<TextView>(R.id.avatar).text = member.initial
            cell.findViewById<TextView>(R.id.memberName).text = member.name
            cell.findViewById<TextView>(R.id.memberPct).text = pct1(Demo.ratioOf(member.id))
            memberRail.addView(cell)
        }
    }

    private fun refreshMembers() {
        memberRail.children().forEachIndexed { index, cell ->
            val member = Demo.members.getOrNull(index) ?: return@forEachIndexed
            cell.findViewById<TextView>(R.id.memberPct).text = pct1(Demo.ratioOf(member.id))
        }
    }

    // ── Pending strip ───────────────────────────────────────────────────

    private fun refreshPending() {
        val pending = Demo.pendingForViewer()
        pendingStrip.showIf(pending.isNotEmpty())
        pendingText.text = "待你确认 · ${pending.size}"
    }

    // ── Sections ────────────────────────────────────────────────────────

    private fun rebuild(animate: Boolean) {
        refreshTicks()
        buildMembers()
        refreshPending()

        val scrollY = scroll.scrollY
        sections.removeAllViews()

        sections.addView(requireContext().action("个人全任务总览") { push(TaskOverviewFragment.newInstance()) }.apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_task_tree, 0, 0, 0)
        })
        sections.addView(requireContext().action(if (tasksCollapsed) "▸ 展开项目任务" else "▾ 收起项目任务") {
            tasksCollapsed = !tasksCollapsed
            rebuild(animate = false)
        })
        if (tasksCollapsed) return
        val stages = filter?.let { listOf(it) } ?: Stage.values().toList()
        val rows = mutableListOf<View>()
        var index = 0

        stages.forEach { stage ->
            val items = Demo.itemsOf(stage)
            if (filter == null && items.isEmpty()) return@forEach
            index++
            sections.addView(sectionHeader(index, stage, items.size))
            if (items.isEmpty()) {
                sections.addView(emptyState(stage))
            }
            items.forEach { item ->
                val row = layoutInflater.inflate(R.layout.row_item, sections, false)
                bindRow(row, item)
                sections.addView(row)
                (row.layoutParams as LinearLayout.LayoutParams).apply {
                    setMargins(dp(16f), dp(4f), dp(16f), dp(4f))
                }
                rows.add(row)
            }
        }

        if (animate) {
            Motion.stagger(rows, 22L, 10f, 240L)
        } else {
            scroll.post { scroll.scrollTo(0, scrollY) }
        }
    }

    private fun sectionHeader(index: Int, stage: Stage, count: Int): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(24f), ctx.dp(24f), ctx.dp(24f), ctx.dp(10f))

            addView(TextView(ctx).apply {
                text = String.format("%02d", index)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                letterSpacing = 0.12f
                setTextColor(colorOf(R.color.accent))
                typeface = Fonts.sansMedium
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    ctx.dp(40f), LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })

            addView(TextView(ctx).apply {
                text = stage.label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                letterSpacing = 0.14f
                setTextColor(colorOf(R.color.ink))
                typeface = Fonts.sansMedium
                includeFontPadding = false
            })

            addView(View(ctx).apply {
                setBackgroundColor(colorOf(R.color.line))
                layoutParams = LinearLayout.LayoutParams(0, ctx.dp(1f), 1f).apply {
                    marginStart = ctx.dp(12f)
                    marginEnd = ctx.dp(12f)
                }
            })

            addView(TextView(ctx).apply {
                text = "$count 项"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(colorOf(R.color.muted))
                typeface = Fonts.sans
                includeFontPadding = false
            })
        }
    }

    private fun emptyState(stage: Stage): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(24f), ctx.dp(10f), ctx.dp(24f), ctx.dp(22f))
            addView(TextView(ctx).apply {
                text = "没有${stage.label}的工作项"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Fonts.serif
                setTextColor(colorOf(R.color.ink))
                includeFontPadding = false
            })
            addView(TextView(ctx).apply {
                text = "所有条目都已核对，结算按当前快照生成"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(colorOf(R.color.muted))
                includeFontPadding = false
                setPadding(0, ctx.dp(6f), 0, 0)
            })
        }
    }

    private fun bindRow(row: View, item: WorkItem) {
        row.findViewById<TextView>(R.id.rowNo).text = String.format("%02d", item.no)
        row.findViewById<TextView>(R.id.rowTitle).text = item.title

        val owner = Demo.member(item.ownerId).name
        row.findViewById<TextView>(R.id.rowMeta).text =
            "$owner · ${item.points} 分 · ${Demo.shareSummary(item)}"

        val score = row.findViewById<TextView>(R.id.rowScore)
        val state = row.findViewById<TextView>(R.id.rowState)

        if (item.accepted > 0) {
            score.text = n1(item.confirmedPoints)
            score.setTextColor(colorOf(R.color.ink))
        } else {
            score.text = "—"
            score.setTextColor(colorOf(R.color.line))
        }

        state.text = item.stage.label
        state.background = null
        state.setPadding(0, 0, 0, 0)
        when (item.stage) {
            Stage.DONE -> state.setTextColor(colorOf(R.color.accent))
            Stage.PENDING -> {
                state.setTextColor(colorOf(R.color.accent))
                state.setBackgroundResource(R.drawable.bg_stamp)
                state.setPadding(dp(6f), dp(3f), dp(6f), dp(3f))
            }
            Stage.DOING -> state.setTextColor(colorOf(R.color.ink))
            Stage.NOT_STARTED -> state.setTextColor(colorOf(R.color.muted))
        }

        row.findViewById<TextView>(R.id.rowAffected).showIf(item.affected)
        row.setOnClickListener { open(item, row) }
    }

    private fun open(item: WorkItem, source: View) {
        val name = "item_${item.id}"
        source.transitionName = name
        push(ItemDetailFragment.newInstance(item.id), source, name)
    }
}

private fun ViewGroup.children(): List<View> = (0 until childCount).map { getChildAt(it) }
