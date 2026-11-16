package com.example.mycoursepractice.ui

import android.content.Context
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import com.example.mycoursepractice.R
import com.google.android.material.transition.MaterialContainerTransform

/**
 * One motion vocabulary for the whole app.
 *
 * 200ms / standard   small state changes (tabs, stamps, segments)
 * 340ms / emphasized page-to-page morphs (container transform)
 * 40ms  stagger step for list entrances
 */
object Motion {

    const val STANDARD_MS = 200L
    const val EMPHASIS_MS = 340L
    const val STAGGER_MS = 40L

    val standardCurve = PathInterpolator(0.2f, 0f, 0f, 1f)
    val emphasisCurve = PathInterpolator(0.2f, 0f, 0f, 1f)

    /** Content entering a page: 8dp rise + fade. */
    fun rise(view: View, delay: Long = 0L, distanceDp: Float = 8f, duration: Long = 220L) {
        val d = view.context.dpf(distanceDp)
        view.alpha = 0f
        view.translationY = d
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(emphasisCurve)
            .start()
    }

    fun fade(view: View, delay: Long = 0L, duration: Long = 220L) {
        view.alpha = 0f
        view.animate().alpha(1f).setStartDelay(delay).setDuration(duration)
            .setInterpolator(standardCurve).start()
    }

    fun stagger(views: List<View>, step: Long = STAGGER_MS, distanceDp: Float = 8f, duration: Long = 220L) {
        views.forEachIndexed { index, view -> rise(view, index * step, distanceDp, duration) }
    }

    /** Stamp lands: 120ms, slight overshoot. */
    fun stamp(view: View) {
        view.alpha = 0f
        view.scaleX = 1.12f
        view.scaleY = 1.12f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(120L)
            .setInterpolator(standardCurve)
            .start()
    }

    /** Evidence box: 0.94 -> 1 with fade, 240ms. */
    fun settle(view: View) {
        view.alpha = 0f
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(240L)
            .setInterpolator(emphasisCurve)
            .start()
    }

    fun containerTransform(context: Context): MaterialContainerTransform =
        MaterialContainerTransform().apply {
            drawingViewId = R.id.container
            duration = EMPHASIS_MS
            interpolator = emphasisCurve
            setAllContainerColors(ContextCompat.getColor(context, R.color.paper))
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        }
}
