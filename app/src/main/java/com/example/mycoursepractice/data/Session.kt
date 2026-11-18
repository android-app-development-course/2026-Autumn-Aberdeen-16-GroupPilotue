package com.example.mycoursepractice.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 演示用的登录态。
 *
 * 没有账号服务，也不校验密码：只把"已登录 + 用户名"存在本机，
 * 用来决定启动时先进登录页还是直接进项目库。真要接后端时，
 * 把这里换成 token 的存取即可，界面不用动。
 */
object Session {

    private const val PREF = "gp_session"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_USER = "username"

    fun isLoggedIn(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_LOGGED_IN, false)

    fun username(ctx: Context): String = prefs(ctx).getString(KEY_USER, "").orEmpty()

    fun login(ctx: Context, name: String) {
        prefs(ctx).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER, name)
            .apply()
    }

    fun logout(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_LOGGED_IN)
            .remove(KEY_USER)
            .apply()
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
