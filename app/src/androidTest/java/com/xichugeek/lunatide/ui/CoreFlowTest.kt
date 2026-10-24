package com.xichugeek.lunatide.ui

import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.xichugeek.lunatide.MainActivity
import com.xichugeek.lunatide.LunatideApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

class CoreFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun freshOnboardingStartCalendarAndAutosavedFlow(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as LunatideApp
        withContext(Dispatchers.Main) { app.appLock.erase() }
        withTimeout(10_000) { app.appLock.state.first { it.erased } }
        withContext(Dispatchers.Main) { app.appLock.acknowledgeErase() }
        if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("zh-CN")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("跳过").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("跳过").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("例假来了").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("例假来了").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("经期第 1 天", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("日历").performClick()
            compose.onNodeWithContentDescription("经期第 1 天", substring = true).assertExists().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("经量").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("中", useUnmergedTree = true).performClick()
            compose.onNodeWithText("关闭").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("经量").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithContentDescription("经期第 1 天", substring = true).assert(hasContentDescription("中", substring = true))
        }
    }
}
