package com.example.mycoursepractice.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Profile

/**
 * 修改密码：从「个人信息」页的按钮弹出的底部面板。
 *
 * 两个输入框仍在这张单上，但不占个人信息页的版面——
 * 那页只留一个按钮，点开才面对密码这件事。
 */
object PasswordSheet {

    fun show(host: Fragment, onSaved: () -> Unit = {}) {
        val ctx = host.requireContext()
        val dialog = Dialog(ctx)
        val root = LayoutInflater.from(ctx).inflate(R.layout.dialog_password, null)

        val newPass = root.findViewById<EditText>(R.id.newPass)
        val repeatPass = root.findViewById<EditText>(R.id.repeatPass)
        val warn = root.findViewById<TextView>(R.id.passWarn)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.35f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        fun fail(message: String, focus: View?) {
            warn.text = message
            warn.showIf(true)
            focus?.requestFocus()
        }

        // 一开始打字就把上一次的报错收掉，别让旧提示挂着。
        val clearWarn = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                warn.showIf(false)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        newPass.addTextChangedListener(clearWarn)
        repeatPass.addTextChangedListener(clearWarn)

        root.findViewById<View>(R.id.closeLink).setOnClickListener { dialog.dismiss() }
        root.findViewById<View>(R.id.cancelBtn).setOnClickListener { dialog.dismiss() }

        root.findViewById<View>(R.id.saveBtn).setOnClickListener {
            val p1 = newPass.text.toString()
            val p2 = repeatPass.text.toString()
            when {
                p1.length < 6 -> return@setOnClickListener fail("密码至少 6 位", newPass)
                p1 != p2 -> return@setOnClickListener fail("两次输入的密码不一样", repeatPass)
            }
            Profile.setPassword(ctx, p1)
            dialog.dismiss()
            onSaved()
        }

        dialog.show()

        root.translationY = ctx.dp(24f).toFloat()
        root.alpha = 0f
        root.animate().translationY(0f).alpha(1f)
            .setDuration(Motion.STANDARD_MS.toLong())
            .setInterpolator(Motion.standardCurve)
            .start()
    }
}
