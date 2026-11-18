package com.example.mycoursepractice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.*

fun Context.column(padding: Int = 0) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    layoutParams = LinearLayout.LayoutParams(-1, -2)
    setPadding(dp(padding.toFloat()), dp(padding.toFloat()), dp(padding.toFloat()), dp(padding.toFloat()))
}

fun View.rounded(color: Int = R.color.accent_wash, radius: Float = 16f) {
    background = GradientDrawable().apply {
        setColor(context.colorOf(color)); cornerRadius = context.dpf(radius)
    }
    clipToOutline = true
}

fun Context.label(value: String, size: Float = 14f, color: Int = R.color.ink) = TextView(this).apply {
    text = value; textSize = size; setTextColor(colorOf(color))
    setPadding(0, dp(6f), 0, dp(6f))
    layoutParams = LinearLayout.LayoutParams(-1, -2)
}

fun Context.action(value: String, selected: Boolean = false, onClick: () -> Unit) = label(value).apply {
    gravity = Gravity.CENTER
    minHeight = dp(48f)
    setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
    rounded(if (selected) R.color.accent else R.color.accent_wash, 12f)
    setTextColor(colorOf(if (selected) R.color.sheet else R.color.accent))
    isClickable = true; isFocusable = true
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6f); bottomMargin = dp(6f) }
}

fun LinearLayout.choices(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val scroller = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
    val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    labels.forEachIndexed { i, text ->
        row.addView(context.action(text, i == selected) { onSelect(i) }.apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6f) }
            isSelected = i == selected
        })
    }
    scroller.addView(row)
    addView(scroller)
}

/** Stores the actually displayed value, so even interrupted transitions resume smoothly. */
class MetricMotion {
    private val displayed = mutableMapOf<String, Double>()

    fun bind(bar: ProgressBar, key: String, target: Double, onFrame: (Double) -> Unit = {}) {
        var animator: ValueAnimator? = null
        fun paint(value: Double) {
            displayed[key] = value
            bar.progress = (value * 10).toInt()
            onFrame(value)
        }
        fun startTransition() {
            animator?.cancel()
            val from = displayed[key] ?: 0.0
            paint(from)
            animator = ValueAnimator.ofFloat(from.toFloat(), target.toFloat()).apply {
                duration = 560L
                interpolator = Motion.standardCurve
                addUpdateListener { paint((it.animatedValue as Float).toDouble()) }
                this.start()
            }
        }
        bar.progress = ((displayed[key] ?: 0.0) * 10).toInt()
        bar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { startTransition() }
            override fun onViewDetachedFromWindow(v: View) { animator?.cancel(); animator = null }
        })
        if (bar.isAttachedToWindow) startTransition()
    }
}

fun LinearLayout.metric(title: String, percent: Double?, expected: Double? = null,
                        motion: MetricMotion? = null, motionKey: String = title) {
    val value = percent?.coerceIn(0.0, 100.0)
    val caption = context.label("", 12f)
    fun captionAt(current: Double) {
        caption.text = "$title  ${if (value == null) "暂无分配" else pct1(current)}" +
            (expected?.let { " · 期望 ${pct1(it)}" } ?: "")
    }
    captionAt(value ?: 0.0)
    addView(caption)
    addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000; progress = ((value ?: 0.0) * 10).toInt()
        progressTintList = ColorStateList.valueOf(colorOf(R.color.accent))
        progressBackgroundTintList = ColorStateList.valueOf(colorOf(R.color.line))
        layoutParams = LinearLayout.LayoutParams(-1, dp(8f)).apply { bottomMargin = dp(6f) }
        contentDescription = "$title ${value?.let { pct1(it) } ?: "暂无数据"}"
        motion?.bind(this, motionKey, value ?: 0.0, ::captionAt)
    })
    if (expected != null) addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 1000; progress = (expected.coerceIn(0.0, 100.0) * 10).toInt()
        progressTintList = ColorStateList.valueOf(colorOf(R.color.muted))
        progressBackgroundTintList = ColorStateList.valueOf(colorOf(R.color.line))
        layoutParams = LinearLayout.LayoutParams(-1, dp(3f)).apply { bottomMargin = dp(4f) }
        contentDescription = "期望 ${pct1(expected)}"
        motion?.bind(this, "$motionKey:expected", expected.coerceIn(0.0, 100.0))
    })
}

fun LinearLayout.criteriaSection(title: String, project: Project, rows: List<TaskCriterion>, expanded: Boolean = true) {
    val box = context.column(12).apply {
        rounded(R.color.sheet)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f) }
    }
    val content = context.column()
    var open = expanded
    val header = context.action("") { }
    fun paint() {
        header.text = "${if (open) "▾" else "▸"} $title · ${rows.count { it.met }}/${rows.size} 达标"
        header.contentDescription = "${header.text}，${if (open) "点击折叠" else "点击展开"}"
        content.show(open)
    }
    header.setOnClickListener { open = !open; paint() }
    box.addView(header)
    if (rows.isEmpty()) content.addView(context.label("暂无分配给你的细则", 13f, R.color.muted))
    rows.forEach { criterion ->
        content.addView(context.label("${if (criterion.met) "✓ 已达标" else "○ 未达标"} · ${project.nameOf(criterion.ownerId)}\n${criterion.title}\n${criterion.achieved} / ${criterion.target} ${criterion.unit}", 13f,
            if (criterion.met) R.color.accent else R.color.ochre))
    }
    box.addView(content); addView(box); paint()
}

fun LinearLayout.taskInsights(project: Project, item: WorkItem) {
    val rows = TaskInsights.criteria(item)
    addView(context.label(if (item.analysis?.demo == true) "演示解析结果 · 尚需成员验收" else "任务细则 · 演示数据", 12f, R.color.muted))
    metric("整个大项目", TaskInsights.weightedProgress(project.items.map { TaskEntry(project, it) }))
    metric("整个小任务", TaskInsights.progress(item), item.expectedPercent.toDouble())
    metric("我的负责部分", TaskInsights.progress(item, Demo.VIEWER), if (item.shareOf(Demo.VIEWER) > 0) item.expectedPercent.toDouble() else null)
    addView(context.label("深色：细则实现比例；细灰线：期望比例。已验收 ${item.accepted}%，验收后才计入贡献。", 11f, R.color.muted))
    criteriaSection("我的任务细则", project, rows.filter { it.ownerId == Demo.VIEWER })
    criteriaSection("整个任务细则", project, rows, false)
}
