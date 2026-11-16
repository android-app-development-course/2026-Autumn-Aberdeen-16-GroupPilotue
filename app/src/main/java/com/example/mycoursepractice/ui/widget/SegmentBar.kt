package com.example.mycoursepractice.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.example.mycoursepractice.R
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp

/**
 * Square segmented control: a flat accent block slides under the chosen cell
 * with a 180ms spring. No corners, no shadow — it is a stamp, not a pill.
 */
class SegmentBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val thumb = View(context)
    private val row = LinearLayout(context)
    private val mains = mutableListOf<TextView>()
    private val subs = mutableListOf<TextView>()
    private var count = 0

    var selected: Int = 0
        private set

    private var listener: ((Int) -> Unit)? = null

    init {
        setBackgroundResource(R.drawable.bg_sheet_border)
        thumb.setBackgroundColor(colorOf(R.color.accent))
        addView(thumb, LayoutParams(0, LayoutParams.MATCH_PARENT))
        row.orientation = LinearLayout.HORIZONTAL
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun configure(
        main: List<String>,
        sub: List<String>? = null,
        initial: Int = 0,
        onChange: (Int) -> Unit
    ) {
        row.removeAllViews()
        mains.clear()
        subs.clear()
        count = main.size
        listener = onChange

        main.forEachIndexed { index, label ->
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { select(index) }
            }
            val mainView = TextView(context).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Fonts.sansMedium
                includeFontPadding = false
                gravity = Gravity.CENTER
            }
            cell.addView(mainView)
            mains.add(mainView)

            if (sub != null && index < sub.size) {
                val subView = TextView(context).apply {
                    text = sub[index]
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    letterSpacing = 0.08f
                    typeface = Fonts.sans
                    includeFontPadding = false
                    gravity = Gravity.CENTER
                    setPadding(0, dp(3f), 0, 0)
                }
                cell.addView(subView)
                subs.add(subView)
            }

            row.addView(cell)
        }

        selected = initial
        post { layoutThumb(false) }
        paint()
    }

    fun select(index: Int, notify: Boolean = true) {
        if (index == selected || index !in 0 until count) return
        selected = index
        layoutThumb(true)
        paint()
        if (notify) listener?.invoke(index)
    }

    private fun layoutThumb(animate: Boolean) {
        if (count == 0 || width == 0) return
        val cell = width / count
        thumb.layoutParams = (thumb.layoutParams as LayoutParams).also { it.width = cell }
        thumb.requestLayout()
        val target = (selected * cell).toFloat()
        if (!animate) {
            thumb.translationX = target
            return
        }
        SpringAnimation(thumb, DynamicAnimation.TRANSLATION_X, target).apply {
            spring = SpringForce(target).apply {
                dampingRatio = 0.85f
                stiffness = SpringForce.STIFFNESS_LOW
            }
            setStartValue(thumb.translationX)
            start()
        }
    }

    private fun paint() {
        mains.forEachIndexed { index, view ->
            view.setTextColor(ContextCompat.getColor(context, if (index == selected) R.color.sheet else R.color.ink))
        }
        subs.forEachIndexed { index, view ->
            val color = if (index == selected) R.color.sheet else R.color.muted
            view.setTextColor(colorOf(color))
        }
    }
}
