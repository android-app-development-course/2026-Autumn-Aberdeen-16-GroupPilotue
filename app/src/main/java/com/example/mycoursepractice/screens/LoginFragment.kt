package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.MainActivity
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Profile
import com.example.mycoursepractice.data.Session
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.showIf

/**
 * 登录：用户名 + 密码 → 项目库。
 *
 * 没有账号服务：只把"已登录 + 用户名"存在本机（Session），密码不校验真伪，
 * 但要求 6 位以上，免得演示时一眼看出是假的输入框。
 * 注册按钮先给一条说明，不做页面。
 */
class LoginFragment : Fragment() {

    private lateinit var root: View

    private val userField: EditText get() = root.findViewById(R.id.userField)
    private val passField: EditText get() = root.findViewById(R.id.passField)
    private val warn: TextView get() = root.findViewById(R.id.warnText)
    private val cta: TextView get() = root.findViewById(R.id.cta)
    private val footerNote: TextView get() = root.findViewById(R.id.footerNote)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_login, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Session.username(requireContext()).takeIf { it.isNotEmpty() }?.let { userField.setText(it) }

        cta.setOnClickListener { attemptLogin() }

        passField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        root.findViewById<View>(R.id.registerBtn).setOnClickListener { showRegisterNote() }

        // 一开始打字就把上一次的提示收掉，别留一条过期报错
        val clearWarn = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                if (warn.visibility == View.VISIBLE) warn.showIf(false)
            }
        }
        userField.addTextChangedListener(clearWarn)
        passField.addTextChangedListener(clearWarn)

        // 入场：整页淡入，与页面切换同一套节奏
        root.alpha = 0f
        root.animate().alpha(1f)
            .setDuration(Motion.STANDARD_MS.toLong() + 60)
            .setInterpolator(Motion.standardCurve)
            .start()
    }

    private fun attemptLogin() {
        val name = userField.text.toString().trim()
        val password = passField.text.toString()

        when {
            name.isEmpty() -> {
                warn("请填用户名")
                userField.requestFocus()
            }

            password.length < 6 -> {
                warn("密码至少 6 位（演示不校验真实密码）")
                passField.requestFocus()
            }

            // 改过密码的账号要按新密码登录；没改过的仍然随便填。
            Profile.password != null && name.equals(Profile.accountName, ignoreCase = true) &&
                password != Profile.password -> {
                warn("这个账号改过密码，请输入新密码")
                passField.requestFocus()
            }

            else -> {
                warn.showIf(false)
                val ctx = requireContext()
                if (Profile.password == null) Profile.setPassword(ctx, password)
                Session.login(ctx, name)
                Profile.save(
                    ctx,
                    account = if (Profile.accountName.isBlank()) name else Profile.accountName,
                    real = Profile.realName,
                    phone = Profile.phone,
                    email = Profile.email
                )
                Motion.stamp(cta)
                (activity as? MainActivity)?.onLoggedIn()
            }
        }
    }

    /** 注册暂不实现：把页脚那行换成一句赭色说明，4 秒后恢复。 */
    private fun showRegisterNote() {
        footerNote.text = "注册还没开放：正式版会用课程邮箱验证，先随便填个用户名登录。"
        footerNote.setTextColor(requireContext().colorOf(R.color.ochre))
        footerNote.removeCallbacks(restoreNote)
        footerNote.postDelayed(restoreNote, 4000)
    }

    private val restoreNote = Runnable {
        if (!isAdded) return@Runnable
        footerNote.text = "注册还没开放，账号只保存在这台设备上"
        footerNote.setTextColor(requireContext().colorOf(R.color.ink_40))
    }

    private fun warn(message: String) {
        warn.text = message
        warn.showIf(true)
    }
}
