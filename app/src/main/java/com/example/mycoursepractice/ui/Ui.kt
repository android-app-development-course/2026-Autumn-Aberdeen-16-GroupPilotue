package com.example.mycoursepractice.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import java.util.Locale
import kotlin.math.roundToInt

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()
fun Context.dpf(value: Float): Float = value * resources.displayMetrics.density
fun View.dp(value: Float): Int = context.dp(value)
fun Context.colorOf(id: Int): Int = ContextCompat.getColor(this, id)
fun View.colorOf(id: Int): Int = context.colorOf(id)

/** Same shortcuts, for call sites inside a Fragment where there is no View receiver. */
fun Fragment.dp(value: Float): Int = requireContext().dp(value)
fun Fragment.colorOf(id: Int): Int = requireContext().colorOf(id)

object Fonts {
    val serif: Typeface by lazy { Typeface.create("serif", Typeface.NORMAL) }
    val serifBold: Typeface by lazy { Typeface.create("serif", Typeface.BOLD) }
    val sans: Typeface by lazy { Typeface.create("sans-serif", Typeface.NORMAL) }
    val sansMedium: Typeface by lazy { Typeface.create("sans-serif-medium", Typeface.NORMAL) }
}

fun TextView.serif(bold: Boolean = false): TextView = apply {
    typeface = if (bold) Fonts.serifBold else Fonts.serif
}

fun TextView.sans(medium: Boolean = false): TextView = apply {
    typeface = if (medium) Fonts.sansMedium else Fonts.sans
}

fun TextView.track(em: Float): TextView = apply { letterSpacing = em }
fun TextView.tnum(): TextView = apply { fontFeatureSettings = "tnum" }
fun TextView.sp(size: Float): TextView = apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, size) }
fun TextView.inkColor(colorRes: Int): TextView = apply { setTextColor(ContextCompat.getColor(context, colorRes)) }

fun View.show(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.showIf(condition: Boolean) = show(condition)

/** 1dp rule. */
fun Context.hairline(colorRes: Int = R.color.line): View = View(this).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f))
    setBackgroundColor(ContextCompat.getColor(this@hairline, colorRes))
}

fun ViewGroup.hairline(colorRes: Int = R.color.line) {
    addView(context.hairline(colorRes))
}

fun n1(value: Double): String = String.format(Locale.US, "%.1f", value)

fun pct1(value: Double): String = String.format(Locale.US, "%.1f%%", value)

/** Rolling numeral: 600ms, emphasized curve. */
fun TextView.roll(
    from: Double,
    to: Double,
    duration: Long = 600L,
    format: (Double) -> String = { n1(it) }
) {
    ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
        this.duration = duration
        interpolator = Motion.emphasisCurve
        addUpdateListener { text = format((animatedValue as Float).toDouble()) }
        start()
    }
}

fun Context.sp(size: Float): Int = (size * resources.displayMetrics.scaledDensity).roundToInt()
