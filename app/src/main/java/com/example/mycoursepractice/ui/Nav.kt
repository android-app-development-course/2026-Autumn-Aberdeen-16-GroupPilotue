package com.example.mycoursepractice.ui

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mycoursepractice.R

/** Push a screen; optional shared element for the container transform. */
fun Fragment.push(target: Fragment, sharedView: View? = null, sharedName: String? = null) {
    val tx = parentFragmentManager.beginTransaction()
        .setReorderingAllowed(true)
    if (sharedView != null && sharedName != null) {
        tx.addSharedElement(sharedView, sharedName)
    }
    tx.replace(R.id.container, target)
        .addToBackStack(sharedName ?: target.javaClass.simpleName)
        .commit()
}

fun Fragment.popBack() {
    parentFragmentManager.popBackStack()
}

/** Bottom bar visibility is owned by the activity; screens ask for it here. */
fun Fragment.setTabsVisible(visible: Boolean) {
    (activity as? TabHost)?.setTabsVisible(visible)
}

fun Fragment.tabHost(): TabHost? = activity as? TabHost

/**
 * Two levels: 项目库 (no tabs) and 项目 (the four tabs).
 * The activity owns which one is showing, so screens only ask.
 */
interface TabHost {
    fun setTabsVisible(visible: Boolean)
    fun openTab(index: Int)
    fun openLibrary()
    fun openProject(projectId: String)
}

/** Wires the shared back bar: label + pop + app-wide 设置. */
fun Fragment.bindBackBar(root: View, label: String) {
    root.findViewById<TextView>(R.id.backLabel)?.text = label
    root.findViewById<View>(R.id.backBar)?.setOnClickListener { popBack() }
    root.findViewById<View>(R.id.appSettings)?.setOnClickListener { SettingsSheet.show(this) }
}

/** Tab 页 masthead 右上角的同一个入口；有返回箭头的页面顺带接上「回项目库」。 */
fun Fragment.bindAppSettings(root: View) {
    root.findViewById<TextView>(R.id.groupLabel)?.apply {
        text = com.example.mycoursepractice.data.Demo.groupLabel + " ›"
        minHeight = context.dp(48f)
        isFocusable = true
        contentDescription = "小组总览与成员管理"
        setOnClickListener { push(com.example.mycoursepractice.screens.GroupOverviewFragment()) }
    }
    root.findViewById<View>(R.id.appSettings)?.setOnClickListener { SettingsSheet.show(this) }
    root.findViewById<View>(R.id.libraryBack)?.setOnClickListener { tabHost()?.openLibrary() }
}
