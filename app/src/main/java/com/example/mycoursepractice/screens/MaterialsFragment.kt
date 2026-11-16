package com.example.mycoursepractice.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.bindAppSettings
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.push
import com.example.mycoursepractice.ui.show

/** 材料 — the parsed course documents that requirements are traced back to. */
class MaterialsFragment : Fragment() {

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_materials, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rootView.findViewById<TextView>(R.id.courseLabel).text = Demo.courseLabel
        rootView.findViewById<TextView>(R.id.groupLabel).text = Demo.groupLabel
        rootView.findViewById<TextView>(R.id.countLabel).text = "2 份文件 · 1 份已解析"
        bindAppSettings(rootView)

        buildParsed()
        buildFailed()

        rootView.findViewById<View>(R.id.addMaterial).setOnClickListener {
            Toast.makeText(requireContext(), "演示环境：上传未接入", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildParsed() {
        val list = rootView.findViewById<LinearLayout>(R.id.materialRows)
        val row = layoutInflater.inflate(R.layout.row_material, list, false)
        row.findViewById<TextView>(R.id.materialKind).apply {
            text = "PDF"
            setTextColor(colorOf(R.color.accent))
        }
        row.findViewById<TextView>(R.id.materialName).text = "项目简介.pdf"
        row.findViewById<TextView>(R.id.materialMeta).text = "12 页 · 11-02 上传 · 解析出 4 条要求"
        row.findViewById<TextView>(R.id.materialStatus).apply {
            text = "已解析"
            setTextColor(colorOf(R.color.accent))
        }
        row.findViewById<View>(R.id.materialRow).setOnClickListener {
            push(RequirementFragment())
        }
        list.addView(row)
        list.addView(divider())
        Motion.rise(row, 0L, 6f, 240L)
    }

    private fun buildFailed() {
        val list = rootView.findViewById<LinearLayout>(R.id.materialRows)
        val row = layoutInflater.inflate(R.layout.row_material, list, false)
        row.findViewById<TextView>(R.id.materialKind).apply {
            text = "DOCX"
            setTextColor(colorOf(R.color.ochre))
        }
        row.findViewById<TextView>(R.id.materialName).text = "小组计划.docx"
        row.findViewById<TextView>(R.id.materialMeta).text = "11-13 上传 · 9 页 · 未提取到文字层"
        row.findViewById<TextView>(R.id.materialStatus).apply {
            text = "解析失败"
            setTextColor(colorOf(R.color.ochre))
        }
        row.findViewById<ImageView>(R.id.materialChevron).show(false)

        val error = row.findViewById<View>(R.id.materialError)
        row.findViewById<View>(R.id.materialRow).setOnClickListener {
            error.show(error.visibility != View.VISIBLE)
            if (error.visibility == View.VISIBLE) Motion.rise(error, 0L, 6f, 220L)
        }

        val retry = row.findViewById<TextView>(R.id.materialRetry)
        retry.setOnClickListener {
            retry.text = "重试中…"
            retry.postDelayed({
                if (!isAdded) return@postDelayed
                retry.text = "仍然失败 · 请转成 PDF"
            }, 1100)
        }

        list.addView(row)
        Motion.rise(row, 60L, 6f, 240L)
    }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(colorOf(R.color.line))
    }
}
