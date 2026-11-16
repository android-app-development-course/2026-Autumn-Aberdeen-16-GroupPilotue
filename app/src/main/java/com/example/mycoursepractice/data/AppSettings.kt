package com.example.mycoursepractice.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 本机偏好：显示用名字 + 三档字号。
 *
 * 字号走 Configuration.fontScale，由 MainActivity.attachBaseContext 应用，
 * 所以整个应用（含对话框）里的 sp 都会按同一比例缩放。
 * 名字直接改 Demo.me，成员栏、结算单、确认记录用的是同一个对象。
 */
object AppSettings {

    const val SMALL = 0.88f
    const val MEDIUM = 1f
    const val LARGE = 1.15f

    private const val PREFS = "gp_settings"
    private const val KEY_NAME = "user_name"
    private const val KEY_SCALE = "font_scale"

    var fontScale: Float = MEDIUM
        private set

    var userName: String = "何嘉俊"
        private set

    private var loaded = false

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 在 attachBaseContext 里调用：读一次磁盘，并把名字同步给 Demo。 */
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        fontScale = p.getFloat(KEY_SCALE, MEDIUM)
        userName = p.getString(KEY_NAME, null) ?: "何嘉俊"
        Demo.renameViewer(userName)
    }

    fun save(ctx: Context, name: String, scale: Float) {
        val clean = name.trim().ifEmpty { "我" }
        userName = clean
        fontScale = scale
        prefs(ctx).edit()
            .putString(KEY_NAME, clean)
            .putFloat(KEY_SCALE, scale)
            .apply()
        Demo.renameViewer(clean)
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
