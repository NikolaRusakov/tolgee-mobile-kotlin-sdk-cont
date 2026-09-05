package io.tolgee.starter.views

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state carries the *inputs* to strings (numbers, names), never resolved strings and never a
 * Context. The Activity resolves them through the wrapped context or [bindTolgee], so a locale or
 * translation change re-renders text without the ViewModel knowing.
 */
data class UiState(
  val progress: Int = 87,
  val passed: Int = 1,
  val total: Int = 10,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

  private val _state = MutableStateFlow(UiState())
  val state: StateFlow<UiState> = _state.asStateFlow()

  /**
   * The exception that proves the rule: text the ViewModel owns because it is also used outside
   * a view (here, the window title). It follows locale and translation changes on its own.
   */
  val title: StateFlow<String> = tolgeeText(app, R.string.app_name)

  fun nextResult() = _state.update { s ->
    s.copy(passed = if (s.passed >= s.total) 1 else s.passed + 1)
  }
}
