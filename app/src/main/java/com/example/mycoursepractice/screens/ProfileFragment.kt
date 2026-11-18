package com.example.mycoursepractice.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Profile
import com.example.mycoursepractice.data.Session
import com.example.mycoursepractice.ui.Avatar
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.PasswordSheet
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.popBack
import com.example.mycoursepractice.ui.showIf
import java.io.File

/**
 * 个人信息：头像图片、实名、账号名、电话、邮箱、修改密码、退出登录。
 *
 * 头像从系统相册选，落盘到应用私有目录（不联网）；没选就用姓名首字。
 * 与别处一样是一张单：字段左对齐、1dp 线分组、页脚一个主操作。
 */
class ProfileFragment : Fragment() {

    private lateinit var avatarImage: ImageView
    private lateinit var avatarText: TextView
    private lateinit var nameView: TextView
    private lateinit var realField: EditText
    private lateinit var accountField: EditText
    private lateinit var phoneField: EditText
    private lateinit var emailField: EditText
    private lateinit var warn: TextView
    private lateinit var cta: TextView

    private var avatarPath: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePicked(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(view, "个人信息")
        view.findViewById<View>(R.id.appSettings).showIf(false)

        avatarImage = view.findViewById(R.id.avatarImage)
        avatarText = view.findViewById(R.id.avatarBig)
        nameView = view.findViewById(R.id.profileName)
        realField = view.findViewById(R.id.realField)
        accountField = view.findViewById(R.id.accountField)
        phoneField = view.findViewById(R.id.phoneField)
        emailField = view.findViewById(R.id.emailField)
        warn = view.findViewById(R.id.warn)
        cta = view.findViewById(R.id.cta)

        avatarPath = Profile.avatarPath
        accountField.setText(Profile.accountName.ifBlank { Session.username(requireContext()) })
        realField.setText(Profile.realName)
        phoneField.setText(Profile.phone)
        emailField.setText(Profile.email)
        paint()

        view.findViewById<View>(R.id.avatarBox).setOnClickListener { pickPhoto() }

        cta.setOnClickListener { save() }

        view.findViewById<View>(R.id.changePasswordBtn).setOnClickListener {
            PasswordSheet.show(this) {
                Toast.makeText(requireContext(), "密码已更新；下次登录要用新密码", Toast.LENGTH_SHORT).show()
            }
        }
        view.findViewById<View>(R.id.logoutLink).setOnClickListener {
            Session.logout(requireContext())
            activity?.recreate()
        }

        Motion.rise(view, delay = 0L)
    }

    private fun pickPhoto() {
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    /** 选完先缩到 512px 存进私有目录，再刷新界面。 */
    private fun savePicked(uri: Uri) {
        val ctx = requireContext()
        try {
            val bitmap = ctx.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return fail("这张图读不出来，换一张试试", null)

            val file = File(ctx.filesDir, "avatar/me.jpg")
            avatarPath = Avatar.store(ctx, bitmap, file)
            Profile.setAvatar(ctx, avatarPath!!)
            paint()
        } catch (e: Exception) {
            fail("这张图读不出来，换一张试试", null)
        }
    }

    /** 头像、名字、副标题都从这里刷。 */
    private fun paint() {
        val real = realField?.text?.toString().orEmpty().trim()
        val account = accountField?.text?.toString().orEmpty().trim()

        nameView.text = real.ifBlank { account.ifBlank { "还没有名字" } }

        val initial = when {
            real.isNotEmpty() -> real.take(1)
            account.isNotEmpty() -> account.take(1)
            else -> Profile.initial()
        }
        Avatar.bind(requireContext(), avatarImage, avatarText, avatarPath, initial, 64f)
    }

    private fun save() {
        val ctx = requireContext()
        val account = accountField.text.toString().trim()
        val real = realField.text.toString().trim()
        val phone = phoneField.text.toString().trim()
        val email = emailField.text.toString().trim()

        when {
            account.isEmpty() -> return fail("账号名不能为空（登录时用它）", accountField)
            phone.isNotEmpty() && (phone.count { it.isDigit() } < 6) ->
                return fail("电话要填数字，至少 6 位；不填就留空", phoneField)
            email.isNotEmpty() && (!email.contains("@") || !email.contains(".")) ->
                return fail("邮箱里要有 @ 和域名，例如 wsq@abdn.ac.uk", emailField)
        }

        warn.showIf(false)
        Profile.save(ctx, account, real, phone, email)

        Motion.stamp(cta)
        cta.postDelayed({ popBack() }, 160)
    }

    private fun fail(message: String, focus: View?) {
        warn.text = message
        warn.showIf(true)
        focus?.requestFocus()
    }
}
