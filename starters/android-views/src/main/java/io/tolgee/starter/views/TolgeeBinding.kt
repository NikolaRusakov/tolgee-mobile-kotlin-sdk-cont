package io.tolgee.starter.views

import android.app.Application
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import io.tolgee.Tolgee
import io.tolgee.TolgeeAndroid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.WeakHashMap

/*
 * Views + ViewModel reactivity for Tolgee.
 *
 * Compose gets reactivity for free from `io.tolgee.stringResource`. Views do not: translations arrive
 * asynchronously and change on locale switch, so every string set from code needs something that
 * re-reads it. The SDK exposes two reactive surfaces, `tFlow` and `changeFlow`; these helpers wrap them
 * so screen code never touches either directly.
 *
 * Copy this file into your app as-is.
 */

/** One live binding per TextView; rebinding (e.g. on every render) cancels the previous collector. */
private val activeBindings = WeakHashMap<TextView, Job>()

/**
 * Keeps this TextView in sync with a Tolgee translation for as long as its view tree is STARTED.
 * Falls back to the bundled resource when Tolgee is not initialized (previews, tests).
 */
fun TextView.bindTolgee(@StringRes id: Int, vararg formatArgs: Any) =
  bindFlow(fallback = { context.getString(id, *formatArgs) }) { tolgee ->
    tolgee.tFlow(context, id, *formatArgs)
  }

/**
 * Plural variant. `quantity` selects the CLDR form and is also passed as the first format
 * argument (`%1$d`), exactly like `Resources.getQuantityString(id, quantity, quantity, *formatArgs)`
 * and the SDK's own `tPlural`. Pass only the remaining arguments in [formatArgs].
 */
fun TextView.bindTolgeePlural(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any) =
  bindFlow(fallback = { resources.getQuantityString(id, quantity, quantity, *formatArgs) }) { tolgee ->
    tolgee.tPluralFlow(resources, id, quantity, *formatArgs)
  }

/** String-array variant, joined for display. */
fun TextView.bindTolgeeArray(@ArrayRes id: Int, separator: CharSequence = ", ") =
  bindFlow(fallback = { resources.getStringArray(id).joinToString(separator) }) { tolgee ->
    tolgee.tArrayFlow(resources, id).map { it.joinToString(separator) }
  }

private fun TextView.bindFlow(fallback: () -> String, source: (TolgeeAndroid) -> Flow<String>) {
  activeBindings.remove(this)?.cancel()
  val tolgee = Tolgee.instanceOrNull
  val owner = findViewTreeLifecycleOwner()
  if (tolgee == null || owner == null) {
    text = fallback()
    return
  }
  activeBindings[this] = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
      source(tolgee).collect { text = it }
    }
  }
}

/**
 * For ViewModels that want to own display text: a StateFlow that follows locale and translation
 * changes. Uses the application context, which needs no [io.tolgee.TolgeeContextWrapper].
 *
 * Prefer keeping resource ids + arguments in UI state and binding in the view layer; use this only
 * where the ViewModel genuinely must produce the string (e.g. shared with a notification).
 */
fun ViewModel.tolgeeText(app: Application, @StringRes id: Int, vararg formatArgs: Any): StateFlow<String> {
  val initial = app.getString(id, *formatArgs)
  val flow = Tolgee.instanceOrNull?.tFlow(app, id, *formatArgs) ?: flowOf(initial)
  return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}
