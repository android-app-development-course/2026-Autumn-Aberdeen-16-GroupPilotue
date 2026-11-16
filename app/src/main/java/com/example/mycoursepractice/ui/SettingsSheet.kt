package com.example.mycoursepractice.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.AppSettings
import com.example.mycoursepractice.ui.widget.SegmentBar

/**
 * 应用级设置：显示名字 + 三档字号。
 * 方角、纸面、一条 1dp 顶线 —— 和别处一样，是一张单，不是一张卡片。
 */
object SettingsSheet {

    fun show(host: Fragment) {
        val ctx = host.requireContext()
        val dialog = Dialog(ctx)
        val root = LayoutInflater.from(ctx).inflate(R.layout.dialog_settings, null)

        val nameField = root.findViewById<EditText>(R.id.nameField)
        val nameHint = root.findViewById<TextView>(R.id.nameHint)
        val fontBar = root.findViewById<SegmentBar>(R.id.fontBar)
        val fontHint = root.findViewById<TextView>(R.id.fontHint)

        nameField.setText(AppSettings.userName)
        nameField.setSelection(nameField.text.length)

        var index = AppSettings.indexOf(AppSettings.fontScale)
        fontBar.configure(
            main = listOf("大", "中", "小"),
            sub = listOf("1.15×", "1.00×", "0.88×"),
            initial = index
        ) { i ->
            index = i
            fontHint.text = hintFor(i)
        }
        fontHint.text = hintFor(index)

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.35f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        root.findViewById<View>(R.id.closeLink).setOnClickListener { dialog.dismiss() }
        root.findViewById<View>(R.id.cancelBtn).setOnClickListener { dialog.dismiss() }

        root.findViewById<View>(R.id.saveBtn).setOnClickListener {
            val name = nameField.text.toString().trim()
            if (name.isEmpty()) {
                nameHint.text = "名字不能为空"
                nameHint.setTextColor(ctx.colorOf(R.color.ochre))
                nameField.requestFocus()
                return@setOnClickListener
            }
            val scale = AppSettings.scaleAt(index)
            val scaleChanged = scale != AppSettings.fontScale
            AppSettings.save(ctx, name, scale)
            dialog.dismiss()
            if (scaleChanged) host.activity?.recreate()
        }

        dialog.show()

        // 从下往上抬 24dp 入场的 200ms，与底部 tab 同一套节奏
        root.translationY = ctx.dp(24f).toFloat()
        root.alpha = 0f
        root.animate().translationY(0f).alpha(1f)
            .setDuration(Motion.STANDARD_MS.toLong())
            .setInterpolator(Motion.standardCurve)
            .start()
    }

    private fun hintFor(index: Int): String = when (index) {
        0 -> "大：比当前设计放大约 15%，列表行会变高"
        2 -> "小：比当前设计缩小约 12%"
        else -> "中档是当前的设计尺寸"
    }
}
