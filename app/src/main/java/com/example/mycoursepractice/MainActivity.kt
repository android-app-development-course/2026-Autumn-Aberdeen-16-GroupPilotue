package com.example.mycoursepractice

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.mycoursepractice.data.AppSettings
import com.example.mycoursepractice.data.Demo
import com.example.mycoursepractice.screens.BoardFragment
import com.example.mycoursepractice.screens.InboxFragment
import com.example.mycoursepractice.screens.LibraryFragment
import com.example.mycoursepractice.screens.MaterialsFragment
import com.example.mycoursepractice.screens.ReportFragment
import com.example.mycoursepractice.ui.Fonts
import com.example.mycoursepractice.ui.Motion
import com.example.mycoursepractice.ui.TabHost
import com.example.mycoursepractice.ui.colorOf
import com.example.mycoursepractice.ui.dp

class MainActivity : AppCompatActivity(), TabHost {

    private enum class Mode { LIBRARY, PROJECT }

    private lateinit var root: View
    private lateinit var container: FrameLayout
    private lateinit var tabShell: View
    private lateinit var tabFrame: FrameLayout
    private lateinit var tabsRow: LinearLayout
    private lateinit var tabIndicator: View

    private val tabLabels = listOf("板", "收件箱", "结算", "材料")
    private val tabViews = mutableListOf<TextView>()
    private var currentTab = 0
    private var bottomInset = 0
    private var tabsVisible = true
    private var mode = Mode.LIBRARY

    /**
     * 字号档位通过 fontScale 生效：所有 sp 一起缩放，dp 不动。
     * 换档位后 recreate()，这里会重新包一层 context。
     */
    override fun attachBaseContext(newBase: Context) {
        AppSettings.load(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = AppSettings.fontScale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        container = findViewById(R.id.container)
        tabShell = findViewById(R.id.tabShell)
        tabFrame = findViewById(R.id.tabFrame)
        tabsRow = findViewById(R.id.tabs)
        tabIndicator = findViewById(R.id.tabIndicator)

        WindowInsetsControllerCompat(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        buildTabs()

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            container.updatePadding(top = bars.top)
            bottomInset = bars.bottom
            applyBottomInset()
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, LibraryFragment())
                .commit()
        } else if (Demo.currentId != null) {
            mode = Mode.PROJECT
        }

        supportFragmentManager.addOnBackStackChangedListener { updateChrome() }

        onBackPressedDispatcher.addCallback(this) {
            when {
                supportFragmentManager.backStackEntryCount > 0 ->
                    supportFragmentManager.popBackStack()

                mode == Mode.PROJECT -> showLibrary()

                else -> finish()
            }
        }

        updateChrome()
    }

    // ── Tabs ────────────────────────────────────────────────────────────

    private fun buildTabs() {
        tabsRow.removeAllViews()
        tabViews.clear()
        tabLabels.forEachIndexed { index, label ->
            val tv = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                letterSpacing = 0.06f
                typeface = Fonts.sansMedium
                includeFontPadding = false
                isClickable = true
                isFocusable = true
                setOnClickListener { openTab(index) }
            }
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            tabsRow.addView(tv)
            tabViews.add(tv)
        }
        tabFrame.post { positionIndicator(false) }
        paintTabs()
    }

    private fun paintTabs() {
        tabViews.forEachIndexed { index, tv ->
            tv.setTextColor(colorOf(if (index == currentTab) R.color.accent else R.color.muted))
        }
    }

    private fun positionIndicator(animate: Boolean) {
        val width = tabFrame.width
        if (width == 0 || tabViews.isEmpty()) return
        val cell = width / tabViews.size
        tabIndicator.layoutParams = (tabIndicator.layoutParams as FrameLayout.LayoutParams)
            .also { it.width = cell }
        tabIndicator.requestLayout()
        val target = (currentTab * cell).toFloat()
        if (animate) {
            tabIndicator.animate().translationX(target)
                .setDuration(Motion.STANDARD_MS.toLong())
                .setInterpolator(Motion.standardCurve)
                .start()
        } else {
            tabIndicator.translationX = target
        }
    }

    override fun openTab(index: Int) {
        if (mode != Mode.PROJECT) return
        if (supportFragmentManager.backStackEntryCount > 0) return
        if (index == currentTab) return
        currentTab = index
        paintTabs()
        positionIndicator(true)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, createTab(index))
            .commit()
    }

    private fun createTab(index: Int): Fragment = when (index) {
        0 -> BoardFragment()
        1 -> InboxFragment()
        2 -> ReportFragment()
        else -> MaterialsFragment()
    }

    // ── Levels ──────────────────────────────────────────────────────────

    override fun openLibrary() {
        showLibrary()
    }

    private fun showLibrary() {
        if (mode == Mode.LIBRARY) return
        Demo.closeProject()
        mode = Mode.LIBRARY
        currentTab = 0
        tabsVisible = true
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, LibraryFragment())
            .commit()
        updateChrome()
    }

    override fun openProject(projectId: String) {
        Demo.openProject(projectId)
        mode = Mode.PROJECT
        currentTab = 0
        tabsVisible = true
        paintTabs()
        positionIndicator(false)
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, BoardFragment())
            .commit()
        updateChrome()
    }

    // ── Chrome ──────────────────────────────────────────────────────────

    override fun setTabsVisible(visible: Boolean) {
        tabsVisible = visible
        updateChrome()
    }

    private fun updateChrome() {
        val shouldShow = mode == Mode.PROJECT &&
            supportFragmentManager.backStackEntryCount == 0 &&
            tabsVisible
        val showing = tabShell.visibility == View.VISIBLE

        if (shouldShow && showing) {
            applyBottomInset()
            return
        }
        if (shouldShow) {
            tabShell.visibility = View.VISIBLE
            tabShell.alpha = 0f
            tabShell.translationY = dp(8f).toFloat()
            tabShell.animate().alpha(1f).translationY(0f)
                .setDuration(Motion.STANDARD_MS.toLong())
                .setInterpolator(Motion.standardCurve)
                .start()
        } else if (showing) {
            tabShell.animate().alpha(0f).translationY(dp(8f).toFloat())
                .setDuration(Motion.STANDARD_MS.toLong())
                .setInterpolator(Motion.standardCurve)
                .withEndAction {
                    if (mode == Mode.LIBRARY ||
                        supportFragmentManager.backStackEntryCount > 0 ||
                        !tabsVisible
                    ) {
                        tabShell.visibility = View.GONE
                        applyBottomInset()
                    }
                }
                .start()
            return
        }
        applyBottomInset()
    }

    private fun applyBottomInset() {
        val visible = tabShell.visibility == View.VISIBLE
        container.updatePadding(bottom = if (visible) 0 else bottomInset)
        tabShell.updatePadding(bottom = if (visible) bottomInset else 0)
    }
}
