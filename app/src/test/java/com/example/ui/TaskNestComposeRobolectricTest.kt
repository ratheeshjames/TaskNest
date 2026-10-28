package com.example.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.domain.usecase.NaturalLanguageDateParser
import com.example.ui.components.ExpressiveEmptyState
import com.example.ui.components.MorphingCheckbox
import com.example.ui.components.QuickAddBar
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.TaskNestExpressiveTheme
import com.example.ui.theme.UiDensityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskNestComposeRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `app_name resource matches TaskNest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("TaskNest", context.getString(R.string.app_name))
  }

  @Test
  fun `QuickAddBar parses natural language chips and submits task`() {
    val parser = NaturalLanguageDateParser()
    var query by mutableStateOf("")
    var submittedText: String? = null

    composeTestRule.setContent {
      TaskNestExpressiveTheme(
        themeMode = AppThemeMode.LIGHT,
        dynamicColor = false,
        accentPreset = AccentPreset.EMERALD,
        densityMode = UiDensityMode.COMFORTABLE,
        fontScaleMultiplier = 1.0f
      ) {
        QuickAddBar(
          text = query,
          onTextChange = { query = it },
          onQuickSubmit = {
            submittedText = query
          },
          onOpenFullEditor = {},
          parser = parser
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_add_input")
      .performTextInput("Buy espresso beans tomorrow 9am !high #errands")
    composeTestRule.waitForIdle()

    // Verify natural-language assist chips appear in the QuickAddBar
    composeTestRule.onNodeWithText("High").assertIsDisplayed()
    composeTestRule.onNodeWithText("#errands").assertIsDisplayed()

    composeTestRule.onNodeWithTag("quick_add_submit_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Buy espresso beans tomorrow 9am !high #errands", submittedText)
  }

  @Test
  fun `MorphingCheckbox toggles completion state on click and ExpressiveEmptyState renders`() {
    var checked by mutableStateOf(false)

    composeTestRule.setContent {
      TaskNestExpressiveTheme {
        MorphingCheckbox(
          checked = checked,
          onCheckedChange = { checked = it },
          modifier = Modifier.testTag("test_morph_checkbox")
        )
        if (!checked) {
          ExpressiveEmptyState(
            icon = Icons.Filled.CheckCircle,
            title = "All Clear for Today",
            subtitle = "Add your first task using the Quick-Add bar below."
          )
        } else {
          Text("Task Completed!")
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("All Clear for Today").assertIsDisplayed()

    composeTestRule.onNodeWithTag("test_morph_checkbox").performClick()
    composeTestRule.waitForIdle()

    assertTrue(checked)
    composeTestRule.onNodeWithText("Task Completed!").assertIsDisplayed()
  }
}
