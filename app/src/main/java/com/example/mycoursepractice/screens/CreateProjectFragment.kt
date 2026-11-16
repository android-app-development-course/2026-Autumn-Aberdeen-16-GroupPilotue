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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Course
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.showIf
import com.example.mycoursepractice.ui.tabHost
import com.example.mycoursepractice.ui.widget.SegmentBar

/** 创建项目：课程 → 名称 → 模板 → 总分 → 是否需确认。一页填完，不搞三步。 */
class CreateProjectFragment : Fragment() {

    private lateinit var root: View
    private var selectedCourse: Course? = null
    private var addingCourse = false
    private var template = 0
    private var requireApproval = false

    private val chips: LinearLayout get() = root.findViewById(R.id.courseChips)
    private val newCourseBox: View get() = root.findViewById(R.id.newCourseBox)
    private val nameField: EditText get() = root.findViewById(R.id.nameField)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_create_project, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(root, "创建项目")

        buildChips()
        buildTemplates()
        bindApproval()

        root.findViewById<View>(R.id.cta).setOnClickListener { create() }
        nameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = clearWarn()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        })
    }

    private fun buildChips() {
        chips.removeAllViews()
        Demo.courses.forEach { course ->
            chips.addView(chip(course.code, selectedCourse?.id == course.id) {
                selectedCourse = course
                addingCourse = false
                newCourseBox.showIf(false)
                if (nameField.text.isNullOrBlank()) nameField.setText("${course.name}小组作业")
                buildChips()
            })
        }
        chips.addView(chip("+ 新课程", addingCourse) {
            addingCourse = true
            selectedCourse = null
            newCourseBox.showIf(true)
            buildChips()
            root.findViewById<EditText>(R.id.courseCode).requestFocus()
        })
    }

    private fun chip(label: String, selected: Boolean, onClick: () -> Unit): TextView =
        TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            setTextColor(colorOf(if (selected) R.color.sheet else R.color.ink))
            isClickable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dp(8f) }
        }

    private fun buildTemplates() {
        val bar = root.findViewById<SegmentBar>(R.id.templateBar)
        val hints = listOf(
            "空白：只建项目，工作项稍后逐个加",
            "按周：4 个工作项，各占 25%",
            "按交付物：4 个工作项，20 / 30 / 30 / 20%"
        )
        bar.configure(
            main = listOf("空白", "按周", "按交付物"),
            sub = listOf("稍后加项", "4 项均分", "按比重"),
            initial = 0
        ) { index ->
            template = index
            root.findViewById<TextView>(R.id.templateHint).text = hints[index]
        }
    }

    private fun bindApproval() {
        val sw = root.findViewById<TextView>(R.id.approvalSwitch)
        fun paint() {
            sw.text = if (requireApproval) "开" else "关"
            sw.setBackgroundResource(if (requireApproval) R.drawable.bg_stamp_filled else R.drawable.bg_chip)
            sw.setTextColor(colorOf(if (requireApproval) R.color.sheet else R.color.muted))
            sw.gravity = Gravity.CENTER
        }
        paint()
        root.findViewById<View>(R.id.approvalRow).setOnClickListener {
            requireApproval = !requireApproval
            paint()
            Motion.stamp(sw)
        }
    }

    private fun create() {
        val name = nameField.text.toString().trim()
        if (name.isEmpty()) {
            warn("先写项目名称")
            return
        }

        val course = selectedCourse ?: run {
            if (!addingCourse) {
                warn("选一门课程")
                return
            }
            val code = root.findViewById<EditText>(R.id.courseCode).text.toString().trim()
            val cname = root.findViewById<EditText>(R.id.courseName).text.toString().trim()
            if (code.isEmpty() || cname.isEmpty()) {
                warn("新课需要一个代码和一个名称")
                return
            }
            Demo.addCourse(code, cname, "2026 秋")
        }

        val points = root.findViewById<EditText>(R.id.pointsField).text.toString().toDoubleOrNull()
        if (points == null || points <= 0 || points > 1000) {
            warn("总分要在 1–1000 之间")
            return
        }

        val templateKey = listOf("空白", "按周", "按交付物")[template]
        val project = Demo.createProject(course.id, name, templateKey, points, requireApproval)
        Motion.stamp(root.findViewById(R.id.cta))
        tabHost()?.openProject(project.id)
    }

    private fun warn(text: String) {
        root.findViewById<TextView>(R.id.footerNote).text = text
        root.findViewById<TextView>(R.id.footerNote).setTextColor(colorOf(R.color.ochre))
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun clearWarn() {
        root.findViewById<TextView>(R.id.footerNote).apply {
            text = "创建后可生成邀请码，发给组员"
            setTextColor(colorOf(R.color.muted))
        }
    }
}
