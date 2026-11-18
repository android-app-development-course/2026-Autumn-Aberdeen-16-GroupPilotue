package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.*
import com.example.mycoursepractice.ui.*
import java.util.Calendar

/** Account-level overview. Explicit project IDs prevent cross-project task-ID collisions. */
class TaskOverviewFragment : Fragment() {
    companion object {
        fun newInstance() = TaskOverviewFragment().apply {
            arguments = Bundle().apply { putString("origin", Demo.currentId) }
        }
    }
    private lateinit var body: LinearLayout
    private lateinit var controls: LinearLayout
    private lateinit var dock: LinearLayout
    private var request: UiRequest? = null
    private var entries = emptyList<TaskEntry>()
    private var mode = 0
    private var everyone = false
    private var projectId: String? = null
    private var sort = 0
    private var chart = 0
    private var dockOpen = true
    private var anchor = Calendar.getInstance()
    private var selectedDay: String? = null
    private val collapsed = mutableSetOf<String>()
    private val metricMotion = MetricMotion()
    private var renderedMode = -1

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        state?.let {
            mode = it.getInt("mode"); everyone = it.getBoolean("everyone")
            projectId = it.getString("project"); sort = it.getInt("sort"); chart = it.getInt("chart")
            dockOpen = it.getBoolean("dock", true); anchor.timeInMillis = it.getLong("anchor", anchor.timeInMillis)
            selectedDay = it.getString("day"); collapsed.addAll(it.getStringArrayList("collapsed").orEmpty())
        }
    }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putInt("mode", mode); out.putBoolean("everyone", everyone); out.putString("project", projectId)
        out.putInt("sort", sort); out.putInt("chart", chart); out.putBoolean("dock", dockOpen)
        out.putLong("anchor", anchor.timeInMillis); out.putString("day", selectedDay)
        out.putStringArrayList("collapsed", ArrayList(collapsed))
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val ctx = requireContext()
        val shell = ctx.column().apply { setBackgroundColor(colorOf(R.color.paper)) }
        shell.addView(inflater.inflate(R.layout.view_back_bar, shell, false))
        controls = ctx.column(12)
        shell.addView(controls)
        body = ctx.column(16)
        shell.addView(ScrollView(ctx).apply { addView(body); isFillViewport = true }, LinearLayout.LayoutParams(-1, 0, 1f))
        dock = ctx.column(12).apply { rounded(R.color.sheet, 20f); elevation = dp(6f).toFloat() }
        shell.addView(dock)
        return shell
    }
    override fun onViewCreated(view: View, state: Bundle?) { bindBackBar(view, "个人全任务总览") }
    override fun onResume() {
        super.onResume()
        // Returning from a task restores the previous project rather than leaking its global facade.
        val origin = arguments?.getString("origin")
        if (origin == null) Demo.closeProject() else Demo.openProject(origin)
        load()
    }
    private fun load() {
        request?.cancel()
        body.removeAllViews(); body.addView(requireContext().label("正在读取任务…"))
        request = UiServices.repository.tasks { result ->
            if (view == null) return@tasks
            result.onSuccess { entries = it; render() }.onFailure {
                body.removeAllViews(); body.addView(requireContext().label(it.message ?: "加载失败"))
                body.addView(requireContext().action("重试") { load() })
            }
        }
    }
    private fun weekStart(): Calendar = (anchor.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    private fun inPeriod(entry: TaskEntry): Boolean {
        if (mode != 1 && mode != 2) return true
        val date = TaskInsights.dueDate(entry.item.due) ?: return false
        return if (mode == 2) TaskInsights.key(date, "yyyy-MM") == TaskInsights.key(anchor, "yyyy-MM")
        else {
            val start = weekStart()
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
            date.timeInMillis >= start.timeInMillis && date.timeInMillis < end.timeInMillis
        }
    }
    private fun filtered(own: Boolean = false): List<TaskEntry> = entries.filter {
        (projectId == null || it.project.id == projectId) && inPeriod(it) &&
            (if (own || !everyone) it.item.shareOf(Demo.VIEWER) > 0 || it.item.ownerId == Demo.VIEWER else true)
    }
    private fun ordered(rows: List<TaskEntry>): List<TaskEntry> = when (sort) {
        1 -> rows.sortedWith(compareBy({ it.project.name }, { it.item.title }))
        2 -> rows.sortedBy { it.item.stage.ordinal }
        3 -> rows.sortedByDescending { TaskInsights.weight(it.item, Demo.VIEWER) }
        4 -> rows.sortedBy { it.item.title }
        else -> rows.sortedBy { TaskInsights.dueDate(it.item.due)?.timeInMillis ?: Long.MAX_VALUE }
    }
    private fun render() {
        val ctx = requireContext()
        controls.removeAllViews()
        controls.choices(listOf("列表", "周课表", "月历", "森林"), mode) { mode = it; selectedDay = null; render() }
        val filterRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        fun select(labels: List<String>, index: Int, choose: (Int) -> Unit) = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(index)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { choose(position) }
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(48f), 1f)
        }
        filterRow.addView(select(listOf("我的任务", "全部成员"), if (everyone) 1 else 0) {
            if (everyone != (it == 1)) { everyone = it == 1; render() }
        })
        val projects = entries.map { it.project }.distinctBy { it.id }
        val projectIndex = projects.indexOfFirst { it.id == projectId } + 1
        filterRow.addView(select(listOf("全部项目") + projects.map { it.name }, projectIndex) {
            val next = projects.getOrNull(it - 1)?.id
            if (projectId != next) { projectId = next; render() }
        })
        controls.addView(filterRow)
        body.removeAllViews()
        val rows = filtered()
        body.addView(ctx.label("${rows.size} 项任务 · ${if (everyone) "全部成员" else "我的任务"}", 18f))
        if (mode == 1 || mode == 2) periodControls()
        when (mode) {
            0 -> {
                body.choices(listOf("截止日期", "项目", "状态", "工作量", "名称"), sort) { sort = it; render() }
                ordered(rows).forEach { body.addView(taskCard(it)) }
            }
            1 -> weekly(rows)
            2 -> monthly(rows)
            3 -> forest(rows)
        }
        if (rows.isEmpty()) body.addView(ctx.label("当前范围暂无任务，可切换成员、项目或日期。", 14f, R.color.muted))
        if (mode == 1 || mode == 2) {
            val undated = entries.filter { (projectId == null || it.project.id == projectId) &&
                (everyone || it.item.shareOf(Demo.VIEWER) > 0 || it.item.ownerId == Demo.VIEWER) && TaskInsights.dueDate(it.item.due) == null }
            if (undated.isNotEmpty()) {
                body.addView(ctx.label("未设置有效截止日期 · ${undated.size} 项", 16f))
                undated.forEach { body.addView(taskCard(it)) }
            }
        }
        renderDock()
        if (renderedMode != mode) {
            body.animate().cancel()
            body.alpha = 0f
            body.translationY = dp(8f).toFloat()
            body.animate().alpha(1f).translationY(0f).setDuration(240L)
                .setInterpolator(Motion.standardCurve).start()
        }
        renderedMode = mode
    }
    private fun periodControls() {
        val title = if (mode == 2) TaskInsights.key(anchor, "yyyy 年 MM 月") else {
            val end = (weekStart().clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
            "${TaskInsights.key(weekStart(), "MM-dd")} — ${TaskInsights.key(end, "MM-dd")}" }
        body.addView(requireContext().label(title, 20f))
        body.choices(listOf("‹ 上${if (mode == 1) "周" else "月"}", "今天", "下${if (mode == 1) "周" else "月"} ›"), -1) {
            if (it == 1) anchor = Calendar.getInstance()
            else anchor.add(if (mode == 1) Calendar.WEEK_OF_YEAR else Calendar.MONTH, if (it == 0) -1 else 1)
            selectedDay = null; render()
        }
    }
    private fun taskCard(entry: TaskEntry, compact: Boolean = false): View {
        val ctx = requireContext()
        val item = entry.item
        return ctx.column(12).apply {
            rounded(R.color.sheet)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6f); bottomMargin = dp(4f) }
            addView(ctx.label(item.title, if (compact) 13f else 16f))
            addView(ctx.label("${entry.project.name}\n${item.stage.label} · DDL ${item.due.ifBlank { "未设置" }}", 11f, R.color.muted))
            if (!compact) {
                metric("小任务", TaskInsights.progress(item), motion = metricMotion, motionKey = "task:${entry.project.id}:${item.id}")
                metric("我的部分", TaskInsights.progress(item, Demo.VIEWER), motion = metricMotion, motionKey = "mine:${entry.project.id}:${item.id}")
            }
            isClickable = true; isFocusable = true
            contentDescription = "${entry.project.name}，${item.title}，${item.stage.label}，截止 ${item.due}，查看详情"
            setOnClickListener { Demo.openProject(entry.project.id); push(ItemDetailFragment.newInstance(item.id)) }
        }
    }
    private fun weekly(rows: List<TaskEntry>) {
        val ctx = requireContext()
        body.addView(ctx.label("周一至周日 · 横向滑动查看整周", 12f, R.color.muted))
        val timetable = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        days.forEachIndexed { index, label ->
            val date = (weekStart().clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            val dayRows = rows.filter { TaskInsights.dueDate(it.item.due)?.let { date -> TaskInsights.key(date) } == TaskInsights.key(date) }
            timetable.addView(ctx.column(6).apply {
                rounded(R.color.accent_wash)
                layoutParams = LinearLayout.LayoutParams(dp(146f), -2).apply { marginEnd = dp(6f) }
                addView(ctx.label("$label\n${TaskInsights.key(date, "MM-dd")}", 15f))
                if (dayRows.isEmpty()) addView(ctx.label("无截止任务", 12f, R.color.muted))
                ordered(dayRows).forEach { addView(taskCard(it, true)) }
            })
        }
        body.addView(HorizontalScrollView(ctx).apply { addView(timetable) })
    }
    private fun monthly(rows: List<TaskEntry>) {
        val ctx = requireContext()
        val start = (anchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val offset = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val count = start.getActualMaximum(Calendar.DAY_OF_MONTH)
        val labels = LinearLayout(ctx)
        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
            labels.addView(ctx.label(it, 12f).apply { gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        }
        body.addView(labels)
        for (week in 0 until (offset + count + 6) / 7) {
            val row = LinearLayout(ctx)
            for (column in 0..6) {
                val day = week * 7 + column - offset + 1
                val date = (start.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day.coerceIn(1, count)) }
                val key = TaskInsights.key(date)
                val tasks = rows.count { TaskInsights.dueDate(it.item.due)?.let { TaskInsights.key(it) } == key }
                val valid = day in 1..count
                val cell = ctx.action(if (valid) "$day\n${if (tasks > 0) "${tasks}项" else "·"}" else "", key == selectedDay) {
                    selectedDay = if (selectedDay == key) null else key; render()
                }.apply {
                    isEnabled = valid; alpha = if (valid) 1f else 0f
                    textSize = 12f; setPadding(0, dp(6f), 0, dp(6f))
                    layoutParams = LinearLayout.LayoutParams(0, dp(58f), 1f).apply { setMargins(dp(1f), dp(2f), dp(1f), dp(2f)) }
                    contentDescription = "$key，$tasks 项截止任务"
                }
                row.addView(cell)
            }
            body.addView(row)
        }
        body.addView(ctx.label(selectedDay?.let { "$it 的任务 · 再点日期查看整月" } ?: "整月任务 · 点击日期筛选", 14f))
        val selected = rows.filter { selectedDay == null || TaskInsights.dueDate(it.item.due)?.let { d -> TaskInsights.key(d) } == selectedDay }
        ordered(selected).forEach { body.addView(taskCard(it)) }
        if (selected.isEmpty()) body.addView(ctx.label("这一天没有截止任务", 13f, R.color.muted))
    }
    private fun forest(rows: List<TaskEntry>) {
        val ctx = requireContext()
        rows.groupBy { TaskInsights.dueDate(it.item.due)?.let { d -> TaskInsights.key(d, "yyyy-MM") } ?: "未设置日期" }
            .toSortedMap().forEach { (month, monthRows) ->
                val monthKey = "month:$month"
                body.addView(ctx.action("${if (monthKey in collapsed) "▸" else "▾"} $month · ${monthRows.size} 项") { toggle(monthKey) })
                if (monthKey !in collapsed) monthRows.groupBy { it.project.id }.forEach { (pid, projectRows) ->
                    val key = "$month/$pid"
                    val branch = ctx.column(12)
                    branch.rounded(R.color.accent_wash)
                    branch.addView(ctx.action("├ ${if (key in collapsed) "▸" else "▾"} ${projectRows.first().project.name}") { toggle(key) })
                    branch.metric("大项目细则实现", TaskInsights.weightedProgress(entries.filter { it.project.id == pid }),
                        motion = metricMotion, motionKey = "project:$month:$pid")
                    if (key !in collapsed) ordered(projectRows).forEach { branch.addView(taskCard(it)) }
                    body.addView(branch)
                }
            }
    }
    private fun toggle(key: String) { if (!collapsed.add(key)) collapsed.remove(key); render() }

    private fun renderDock() {
        val ctx = requireContext()
        val mine = filtered(own = true)
        val progress = TaskInsights.weightedProgress(mine, Demo.VIEWER)
        dock.removeAllViews()
        dock.addView(ctx.action("${if (dockOpen) "▾" else "▴"} 我的工作量 · ${mine.size} 项 · ${progress?.let { pct1(it) } ?: "暂无分配"}") {
            dockOpen = !dockOpen; renderDock()
        })
        if (!dockOpen) return
        val charts = ctx.column()
        var metricIndex = 0
        // Chart slots morph from the visible value of the previous chart, including rapid taps.
        fun chartMetric(title: String, value: Double?, expected: Double? = null) {
            charts.metric(title, value, expected, metricMotion, "dock:${metricIndex++}")
        }
        charts.choices(listOf("完成情况", "项目分布", "截止分布"), chart) { chart = it; renderDock() }
        val total = mine.sumOf { TaskInsights.weight(it.item, Demo.VIEWER) }
        when (chart) {
            0 -> {
                chartMetric("我的细则实现", progress, TaskInsights.expected(mine, Demo.VIEWER))
                Stage.values().forEach { stage ->
                    val weight = mine.filter { it.item.stage == stage }.sumOf { TaskInsights.weight(it.item, Demo.VIEWER) }
                    chartMetric("${stage.label} · ${n1(weight)} 分值", if (total > 0) weight / total * 100 else null)
                }
            }
            1 -> mine.groupBy { it.project.id }.forEach { (_, group) ->
                val weight = group.sumOf { TaskInsights.weight(it.item, Demo.VIEWER) }
                chartMetric("${group.first().project.name} · ${n1(weight)} 分值", if (total > 0) weight / total * 100 else null)
            }
            else -> {
                val open = mine.filter { it.item.stage != Stage.DONE }
                val openWeight = open.sumOf { TaskInsights.weight(it.item, Demo.VIEWER) }
                open.groupBy { TaskInsights.dueDate(it.item.due)?.let { d -> TaskInsights.key(d, if (mode == 1 || mode == 2) "MM-dd" else "yyyy-MM") } ?: "未定日期" }
                    .toSortedMap().forEach { (date, group) ->
                        val weight = group.sumOf { TaskInsights.weight(it.item, Demo.VIEWER) }
                        chartMetric("$date · ${n1(weight)} 分值", if (openWeight > 0) weight / openWeight * 100 else null)
                    }
                if (open.isEmpty()) charts.addView(ctx.label("当前范围没有未完成任务", 13f))
            }
        }
        charts.addView(ctx.label("演示数据 · 工作量＝任务分值×个人分摊。跟随项目与周/月范围；日历点选不改变月汇总。细则实现不等于已验收贡献。", 11f, R.color.muted))
        dock.addView(ScrollView(ctx).apply { addView(charts) }, LinearLayout.LayoutParams(-1,
            (resources.displayMetrics.heightPixels * 0.25f).toInt().coerceAtMost(dp(190f))))
    }
    override fun onDestroyView() { request?.cancel(); super.onDestroyView() }
}
