package com.example.mycoursepractice.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Invitation
import com.example.mycoursepractice.data.JoinResult
import com.example.mycoursepractice.data.Project
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.showIf
import com.example.mycoursepractice.ui.tabHost

/**
 * 加入项目：输码 → 预览 → 加入。
 * 已过期 / 已在组内 / 找不到，各自给一句人话，不弹系统对话框。
 */
class JoinProjectFragment : Fragment() {

    private lateinit var root: View
    private var invitation: Invitation? = null
    private var joinedProject: Project? = null

    private val codeField: EditText get() = root.findViewById(R.id.codeField)
    private val previewBox: View get() = root.findViewById(R.id.previewBox)
    private val warnText: TextView get() = root.findViewById(R.id.warnText)
    private val cta: TextView get() = root.findViewById(R.id.cta)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_join, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(root, "加入项目")

        val prefill = arguments?.getString(ARG_CODE)
        if (!prefill.isNullOrEmpty()) {
            codeField.setText(prefill)
            lookup()
        } else {
            Demo.pendingInvitations().firstOrNull()?.let { inv ->
                val fill = root.findViewById<TextView>(R.id.inviteFill)
                fill.text = "用 ${inv.inviterName} 的邀请码 ${inv.code} 填入 ›"
                fill.showIf(true)
                fill.setOnClickListener {
                    codeField.setText(inv.code)
                    codeField.setSelection(inv.code.length)
                    lookup()
                }
            }
        }

        cta.setOnClickListener {
            if (previewBox.visibility == View.VISIBLE) join() else lookup()
        }

        codeField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                previewBox.showIf(false)
                warnText.showIf(false)
                cta.text = "查找项目"
            }

            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        })
    }

    private fun lookup() {
        val code = codeField.text.toString().trim()
        if (code.length < 4) {
            warn("邀请码是 6 位，再检查一下")
            return
        }
        when (val result = Demo.joinByCode(code)) {
            is JoinResult.Ok -> showPreview(result.project)
            JoinResult.NotFound -> warn("没有找到这个邀请码")
            JoinResult.Expired -> warn("这个邀请码已过期，请让发起人重新生成")
            is JoinResult.AlreadyIn -> {
                warn("你已经在「${result.name}」里了")
                prefillJoined(result.name)
            }
        }
    }

    private fun showPreview(project: Project) {
        invitation = Demo.invitationFor(codeField.text.toString())
        joinedProject = project
        previewBox.showIf(true)
        warnText.showIf(false)
        root.findViewById<View>(R.id.inviteFill).showIf(false)
        cta.text = "加入项目"

        root.findViewById<TextView>(R.id.previewName).text = project.name
        root.findViewById<TextView>(R.id.previewCourse).text =
            "${Demo.course(project.courseId).label} · ${project.groupLabel}"

        val facts = root.findViewById<LinearLayout>(R.id.previewFacts)
        facts.removeAllViews()
        val inviter = invitation?.inviterName ?: project.members.firstOrNull()?.name ?: "—"
        facts.addView(fact("发起人", inviter))
        facts.addView(fact("成员", "${project.members.size} 人"))
        facts.addView(fact("已建工作项", "${project.items.size} 个"))
        facts.addView(fact("加入方式", if (project.requireApproval) "需要发起人确认" else "直接加入"))

        Motion.settle(previewBox)
    }

    private fun fact(label: String, value: String): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6f), 0, 0)
        }
        row.addView(TextView(requireContext()).apply {
            text = label
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(colorOf(R.color.muted))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(requireContext()).apply {
            text = value
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(colorOf(R.color.ink))
        })
        return row
    }

    private fun join() {
        val project = joinedProject ?: return
        val inv = invitation
        val target = if (inv != null) Demo.acceptInvitation(inv) else project
        Motion.stamp(root.findViewById(R.id.cta))
        tabHost()?.openProject(target.id)
    }

    private fun prefillJoined(name: String) {
        joinedProject = Demo.projects.firstOrNull { it.name == name }
        cta.text = "打开项目"
        cta.setOnClickListener {
            joinedProject?.let { tabHost()?.openProject(it.id) }
        }
    }

    private fun warn(text: String) {
        previewBox.showIf(false)
        warnText.showIf(true)
        warnText.text = text
        Motion.rise(warnText, 0L, 6f)
        codeField.requestFocus()
    }

    companion object {
        private const val ARG_CODE = "code"

        fun newInstance(code: String? = null): JoinProjectFragment =
            JoinProjectFragment().apply {
                arguments = Bundle().apply { putString(ARG_CODE, code) }
            }
    }
}
