package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindBackBar
import com.example.mycoursepractice.ui.show

/** 证据定位 — a document page with the submitted passage marked. */
class EvidenceFragment : Fragment() {

    companion object {
        private const val ARG_ID = "itemId"

        fun newInstance(itemId: String) = EvidenceFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, itemId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_evidence, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val item = Demo.item(requireArguments().getString(ARG_ID) ?: "w08")
        val evidence = item.evidence

        bindBackBar(view, "证据定位")
        val isPage = evidence?.kind == com.example.mycoursepractice.data.EvidenceKind.FILE
        val stamp = view.findViewById<TextView>(R.id.trailingStamp)
        stamp.text = if (isPage) "第 2 页" else "第 3 个转场"
        stamp.show(true)

        view.findViewById<TextView>(R.id.kicker).text = buildString {
            append(evidence?.title ?: "动效演示.mp4")
            append(" · ")
            append(evidence?.meta ?: "00:42 · 6 个转场")
        }
        view.findViewById<TextView>(R.id.titleText).text =
            if (isPage) "第 2 页 · 提交要求" else "第 3 个转场"
        view.findViewById<TextView>(R.id.metaText).text =
            if (isPage) "提交前须上传到 Moodle · 截止 12:00 (UK)" else "00:18–00:27 · 提交时标记的片段"
        view.findViewById<TextView>(R.id.caption).text =
            if (isPage) "高亮处为材料里读出的原文 · 打开原文件可跳到同一页"
            else "高亮处为提交时标记的位置 · 打开原文件可跳回同一时间码"

        view.findViewById<View>(R.id.openLink).setOnClickListener {
            Toast.makeText(requireContext(), "演示环境：未接入原文件", Toast.LENGTH_SHORT).show()
        }

        val page = view.findViewById<ImageView>(R.id.pageImage)
        val highlight = view.findViewById<View>(R.id.highlight)
        val place = {
            val w = page.width
            val h = page.height
            if (w > 0 && h > 0) {
                val lp = highlight.layoutParams as FrameLayout.LayoutParams
                lp.leftMargin = (w * 0.0726f).toInt()
                lp.topMargin = (h * 0.639f).toInt()
                lp.width = (w * (0.927f - 0.0726f)).toInt()
                lp.height = (h * (0.714f - 0.639f)).toInt()
                highlight.layoutParams = lp
            }
        }
        page.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> place() }
        page.post {
            place()
            Motion.settle(highlight)
        }
    }
}
