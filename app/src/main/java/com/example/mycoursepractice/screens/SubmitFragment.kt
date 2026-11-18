package com.example.mycoursepractice.screens

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.data.Evidence
import com.example.mycoursepractice.data.EvidenceKind
import com.example.mycoursepractice.data.Stage
import com.example.mycoursepractice.ui.*
import com.example.mycoursepractice.data.UiRequest
import com.example.mycoursepractice.data.UiServices
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp
import com.example.mycoursepractice.ui.popBack
import com.example.mycoursepractice.ui.sans
import com.example.mycoursepractice.ui.show
import com.example.mycoursepractice.ui.sp
import com.example.mycoursepractice.ui.widget.SegmentBar

class SubmitFragment : Fragment() {

    companion object {
        private const val ARG_ID = "id"

        private val DEMO_FILES = listOf(
            "汇报PPT_v3.pptx" to "4.2 MB · 修改于 11-14 02:11",
            "汇报PPT_草稿.pptx" to "3.8 MB · 修改于 11-12 23:40",
            "演示脚本.docx" to "220 KB · 修改于 11-13 09:05"
        )

        fun newInstance(id: String) = SubmitFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }

    private lateinit var rootView: View
    private lateinit var modeBar: SegmentBar
    private var mode = 0
    private var pickedName: String? = null
    private var pickedMeta: String? = null
    private var submitted = false
    private var analysisRequest: UiRequest? = null

    private val item get() = Demo.item(requireArguments().getString(ARG_ID)!!)
    private val filePanel: LinearLayout get() = rootView.findViewById(R.id.filePanel)
    private val linkPanel: LinearLayout get() = rootView.findViewById(R.id.linkPanel)
    private val textPanel: LinearLayout get() = rootView.findViewById(R.id.textPanel)
    private val cta: TextView get() = rootView.findViewById(R.id.cta)
    private val warnText: TextView get() = rootView.findViewById(R.id.warnText)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_submit, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindBackBar(rootView, "提交成果")
        modeBar = rootView.findViewById(R.id.mode)

        val item = item
        val confirmer = Demo.nameOf(Demo.confirmerFor(item))

        rootView.findViewById<TextView>(R.id.itemLabel).text =
            "工作项 " + String.format("%02d", item.no)
        rootView.findViewById<TextView>(R.id.titleText).text = item.title
        rootView.findViewById<TextView>(R.id.metaText).text = buildString {
            append(item.points)
            append(" 分 · ")
            append(Demo.shareSummary(item))
        }

        cta.text = "提交给 $confirmer 确认"
        rootView.findViewById<TextView>(R.id.footerNote).text =
            "提交后由 $confirmer 核对完成度，状态变为待确认"

        modeBar.configure(
            main = listOf("文件", "链接", "文字"),
            sub = listOf("上传附件", "粘贴地址", "直接写"),
            initial = 0
        ) { selectMode(it) }

        rootView.findViewById<View>(R.id.dropZone).setOnClickListener { pickFile() }
        rootView.findViewById<View>(R.id.pickedClear).setOnClickListener { clearPicked() }
        cta.setOnClickListener { submit() }
    }

    private fun selectMode(index: Int) {
        mode = index
        warnText.show(false)

        val target = when (index) {
            0 -> filePanel
            1 -> linkPanel
            else -> textPanel
        }
        listOf(filePanel, linkPanel, textPanel).forEach { it.show(it === target) }
        Motion.fade(target, 0L, Motion.STANDARD_MS)
    }

    private fun pickFile() {
        val panel = rootView.findViewById<LinearLayout>(R.id.pickerPanel)
        if (panel.childCount == 0) {
            DEMO_FILES.forEachIndexed { index, (name, meta) ->
                panel.addView(pickerRow(name, meta))
                if (index < DEMO_FILES.lastIndex) panel.addView(pickerDivider())
            }
        }
        val opening = panel.visibility != View.VISIBLE
        panel.show(opening)
        if (opening) Motion.rise(panel, 0L, 4f, Motion.STANDARD_MS)
    }

    /** One candidate file inside the inline picker — same paper, no system dialog. */
    private fun pickerRow(name: String, meta: String): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12f), 0, dp(12f))
            isClickable = true
            addView(TextView(requireContext()).apply {
                text = name
                sans(medium = true); sp(14f)
                setTextColor(colorOf(R.color.ink))
            })
            addView(TextView(requireContext()).apply {
                text = meta
                sans(); sp(11f)
                setTextColor(colorOf(R.color.muted))
                setPadding(0, dp(3f), 0, 0)
            })
            setOnClickListener {
                pickedName = name
                pickedMeta = meta
                applyPicked()
            }
        }

    private fun pickerDivider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(colorOf(R.color.line))
    }

    private fun applyPicked() {
        rootView.findViewById<TextView>(R.id.pickedName).text = pickedName
        rootView.findViewById<TextView>(R.id.pickedMeta).text = pickedMeta
        rootView.findViewById<View>(R.id.dropZone).show(false)
        rootView.findViewById<View>(R.id.pickerPanel).show(false)
        val picked = rootView.findViewById<View>(R.id.filePicked)
        picked.show(true)
        Motion.settle(picked)
        warnText.show(false)
    }

    private fun clearPicked() {
        pickedName = null
        pickedMeta = null
        rootView.findViewById<View>(R.id.filePicked).show(false)
        rootView.findViewById<View>(R.id.dropZone).show(true)
    }

    private fun submit() {
        if (submitted) return
        val item = item

        val evidence = when (mode) {
            0 -> {
                val name = pickedName
                if (name == null) return warn("先选一个文件，再提交")
                Evidence(EvidenceKind.FILE, name, "已上传 · " + (pickedMeta ?: ""), 2)
            }

            1 -> {
                val url = rootView.findViewById<EditText>(R.id.linkField).text.toString().trim()
                if (url.length < 6) return warn("粘一个能打开的链接")
                val host = url.substringAfter("://", url).substringBefore("/")
                Evidence(EvidenceKind.LINK, host, "外部链接 · " + url.take(38), 1)
            }

            else -> {
                val body = rootView.findViewById<EditText>(R.id.textField).text.toString().trim()
                if (body.length < 8) return warn("至少写一句能验收的话")
                Evidence(EvidenceKind.TEXT, "文字说明", "${body.length} 字 · 写在应用内", 1)
            }
        }

        submitted = true
        warnText.show(false)
        hideKeyboard()
        setInputsEnabled(false)
        cta.isEnabled = false
        cta.text = "正在解析材料…"
        val panel = rootView.findViewById<LinearLayout>(R.id.analysisPanel)
        panel.removeAllViews()
        panel.show(true)
        val status = requireContext().label("正在读取材料 · 0%", 16f)
        panel.addView(status)
        val progress = android.widget.ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(colorOf(R.color.accent))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(colorOf(R.color.line))
        }
        panel.addView(progress, LinearLayout.LayoutParams(-1, dp(10f)))
        panel.addView(requireContext().label("演示解析 · 深色表示已完成，浅色表示剩余", 12f))
        panel.addView(requireContext().action("取消解析") {
            analysisRequest?.cancel()
            submitted = false
            setInputsEnabled(true)
            cta.isEnabled = true
            cta.text = "重新提交并解析"
            panel.show(false)
        })
        val note = rootView.findViewById<EditText>(R.id.noteField).text.toString()
        analysisRequest = UiServices.repository.analyze(Demo.current.id, item.id, evidence, note, { percent ->
            if (view != null) {
                progress.progress = percent
                status.text = "${if (percent < 40) "读取材料" else if (percent < 85) "对照任务细则" else "生成比例图表"} · $percent%"
            }
        }) { result ->
            if (view != null) {
                result.onSuccess {
                    panel.removeAllViews()
                    panel.addView(requireContext().label("解析完成 · 待成员验收", 20f))
                    panel.taskInsights(Demo.current, item)
                    cta.text = "完成，返回任务详情"
                    cta.isEnabled = true
                    cta.setOnClickListener { popBack() }
                    rootView.findViewById<TextView>(R.id.footerNote).text = "解析结果已同步到任务总览；验收前不增加贡献分"
                }.onFailure {
                    submitted = false
                    setInputsEnabled(true)
                    cta.isEnabled = true
                    cta.text = "重试解析"
                    panel.removeAllViews()
                    panel.addView(requireContext().label(it.message ?: "解析失败，请重试"))
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        fun visit(v: View) {
            v.isEnabled = enabled
            if (v is ViewGroup) for (i in 0 until v.childCount) visit(v.getChildAt(i))
        }
        visit(modeBar)
        visit(rootView.findViewById(R.id.panels))
        rootView.findViewById<View>(R.id.noteField).isEnabled = enabled
    }

    override fun onDestroyView() {
        analysisRequest?.cancel()
        submitted = false
        super.onDestroyView()
    }

    private fun warn(message: String) {
        warnText.text = message
        warnText.show(true)
        Motion.settle(warnText)
    }

    private fun hideKeyboard() {
        val manager = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.hideSoftInputFromWindow(rootView.windowToken, 0)
    }
}
