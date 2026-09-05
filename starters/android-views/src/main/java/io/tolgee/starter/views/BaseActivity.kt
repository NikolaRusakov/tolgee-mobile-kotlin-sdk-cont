package io.tolgee.starter.views

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.tolgee.Tolgee
import io.tolgee.TolgeeContextWrapper
import kotlinx.coroutines.launch

/**
 * Everything an Activity needs for Tolgee, in one place. Extend this (or move the three overrides
 * into your existing base class). Works the same with AppCompatActivity.
 */
abstract class BaseActivity : ComponentActivity() {

  /**
   * Routes getString / getText / getQuantityString / getStringArray and layout inflation
   * (android:text, android:hint, android:contentDescription) through Tolgee, with automatic
   * fallback to the bundled resource. Returns a plain wrapper when Tolgee is not initialized.
   */
  override fun attachBaseContext(newBase: Context?) {
    super.attachBaseContext(TolgeeContextWrapper.wrap(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val tolgee = Tolgee.instanceOrNull ?: return
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        tolgee.changeFlow.collect {
          // Re-apply translations to views the inflater translated, in place, without recreate().
          tolgee.retranslate(this@BaseActivity)
          // Anything set imperatively (toolbar title, menu items, notifications) is re-applied here.
          onTranslationsChanged()
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    // Fetches the current locale (cache first, then CDN) and emits changeFlow when data arrives.
    Tolgee.instanceOrNull?.preload(this)
  }

  /**
   * Called on the main thread after each translation or locale change.
   * Views bound with [bindTolgee] update on their own; override only for strings you set by hand.
   */
  protected open fun onTranslationsChanged() {}

  /** Switches locale and loads it. Bound views and changeFlow collectors update afterwards. */
  protected fun switchLocale(tag: String) {
    val tolgee = Tolgee.instanceOrNull ?: return
    tolgee.setLocale(tag)
    tolgee.preload(this)
  }
}
