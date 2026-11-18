package com.example.mycoursepractice.data

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * 本机个人信息：实名、账号名、电话、邮箱、密码、头像图片。
 *
 * 演示版全部存在这台设备上，不联网。头像支持从系统相册选一张图，
 * 图片会被缩到 512px 存在应用私有目录里，路径记在 prefs。
 * 接后端时，把 load/save 换成接口请求、头像换成上传后拿到的 URL 即可。
 */
object Profile {

    private const val PREF = "gp_profile"
    private const val KEY_ACCOUNT = "account"
    private const val KEY_REAL = "real_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASS = "password"
    private const val KEY_AVATAR = "avatar_path"

    var accountName: String = ""
        private set
    var realName: String = ""
        private set
    var phone: String = ""
        private set
    var email: String = ""
        private set

    /** null = 没设置过头像，界面回退到「姓名首字」。 */
    var avatarPath: String? = null
        private set

    /** null = 还没改过密码；登录时只有改过才校验。 */
    var password: String? = null
        private set

    private var loaded = false

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        accountName = p.getString(KEY_ACCOUNT, "").orEmpty()
        realName = p.getString(KEY_REAL, "").orEmpty()
        phone = p.getString(KEY_PHONE, "").orEmpty()
        email = p.getString(KEY_EMAIL, "").orEmpty()
        avatarPath = p.getString(KEY_AVATAR, null)?.takeIf { File(it).exists() }
        password = p.getString(KEY_PASS, null)
        if (accountName.isBlank()) accountName = Session.username(ctx)
        if (accountName.isNotBlank()) Demo.renameViewer(accountName)
    }

    fun save(ctx: Context, account: String, real: String, phone: String, email: String) {
        accountName = account.trim()
        realName = real.trim()
        this.phone = phone.trim()
        this.email = email.trim()
        prefs(ctx).edit()
            .putString(KEY_ACCOUNT, accountName)
            .putString(KEY_REAL, realName)
            .putString(KEY_PHONE, this.phone)
            .putString(KEY_EMAIL, this.email)
            .apply()
        Demo.renameViewer(displayName())
    }

    fun setPassword(ctx: Context, value: String) {
        password = value
        prefs(ctx).edit().putString(KEY_PASS, value).apply()
    }

    fun setAvatar(ctx: Context, path: String) {
        avatarPath = path
        prefs(ctx).edit().putString(KEY_AVATAR, path).apply()
    }

    fun clearAvatar(ctx: Context) {
        avatarPath = null
        prefs(ctx).edit().remove(KEY_AVATAR).apply()
    }

    /** 头像上那个字：优先实名首字，其次账号名首字。 */
    fun initial(): String {
        val source = realName.ifBlank { accountName }.trim()
        return if (source.isEmpty()) "我" else source.take(1)
    }

    /** 界面上显示的名字：优先实名，其次账号名。 */
    fun displayName(): String = realName.ifBlank { accountName }
}
