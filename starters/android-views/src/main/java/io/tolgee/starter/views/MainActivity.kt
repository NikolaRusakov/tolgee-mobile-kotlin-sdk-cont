package io.tolgee.starter.views

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.tolgee.starter.views.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * One screen that exercises every kind of string: static (layout), parameterised, plural, array,
 * and one owned by the ViewModel. Nothing here handles locale changes explicitly; BaseActivity and
 * the bind helpers do.
 */
class MainActivity : BaseActivity() {

  private val viewModel: MainViewModel by viewModels()
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // text_static is translated right here, during inflation, by the wrapped context.
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.buttonNextResult.setOnClickListener { viewModel.nextResult() }
    binding.buttonEn.setOnClickListener { switchLocale("en") }
    binding.buttonCs.setOnClickListener { switchLocale("cs") }
    binding.buttonFr.setOnClickListener { switchLocale("fr") }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.state.collect { render(it) } }
        launch { viewModel.title.collect { title = it } }
      }
    }
  }

  /** Called on every state change. Rebinding is cheap: each helper cancels its previous collector. */
  private fun render(state: UiState) {
    // %1$s
    binding.textParameterized.bindTolgee(R.string.percentage_placeholder, state.progress.toString())
    // "%3$s: %1$d test out of %2$d ok" -> quantity fills %1$d, then total, then the label.
    binding.textPlural.bindTolgeePlural(R.plurals.plr_test_placeholder_2, state.passed, state.total, "Tests")
    binding.textArray.bindTolgeeArray(R.array.array_test)
  }
}
