package com.example.mycoursepractice.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 本机偏好：只剩三档字号。
 *
 * 字号走 Configuration.fontScale，由 MainActivity.attachBaseContext 应用，
 * 所以整个应用（含对话框）里的 sp 都会按同一比例缩放。
 * 显示名字与其它资料在 [Profile] 里。
 */
object AppSettings {

    const val SMALL = 0.88f
    const val MEDIUM = 1f
    const val LARGE = 1.15f

    private const val PREFS = "gp_settings"
    private const val KEY_SCALE = "font_scale"

    var fontScale: Float = MEDIUM
        private set

    private var loaded = false

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 在 attachBaseContext 里调用：读一次磁盘。 */
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        fontScale = prefs(ctx).getFloat(KEY_SCALE, MEDIUM)
    }

    fun saveScale(ctx: Context, scale: Float) {
        fontScale = scale
        prefs(ctx).edit().putFloat(KEY_SCALE, scale).apply()
    }

    fun indexOf(scale: Float): Int = when {
        scale > 1.05f -> 0
        scale < 0.95f -> 2
        else -> 1
    }

    fun scaleAt(index: Int): Float = when (index) {
        0 -> LARGE
        2 -> SMALL
        else -> MEDIUM
    }

    fun labelOf(scale: Float): String = when (indexOf(scale)) {
        0 -> "大"
        2 -> "小"
        else -> "中"
    }
}
