package io.tolgee.starter.views

import android.app.Application
import io.tolgee.Tolgee
import io.tolgee.storage.TolgeeStorageProviderAndroid

/**
 * The only place Tolgee is configured. Runs before any Activity exists, which is what lets
 * [io.tolgee.TolgeeContextWrapper] intercept resource lookups from the first screen on.
 */
class App : Application() {

  override fun onCreate() {
    super.onCreate()

    Tolgee.init {
      contentDelivery {
        // Content Delivery created with format "Android SDK": serves <cdn>/<languageTag>.json with
        // Android-style placeholders. The SDK defaults (path = "$it.json", Formatter.Sprintf) match it,
        // so nothing else is configured here.
        url = BuildConfig.TOLGEE_CDN_URL

        // Persistent cache under filesDir, keyed by version code: a new app version starts clean.
        storage = TolgeeStorageProviderAndroid(this@App, BuildConfig.VERSION_CODE)

        // Locales published in the Content Delivery. Enables BCP 47 fallback (en-US -> en)
        // and preloadAll() even when the delivery has no manifest.json.
        availableLocaleTags("en", "cs", "fr", "sv")
      }

      // Used when the device locale is not among the published ones.
      defaultLanguage("en")
    }
  }
}
