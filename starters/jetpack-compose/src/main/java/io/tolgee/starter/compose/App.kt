package io.tolgee.starter.compose

import android.app.Application
import io.tolgee.Tolgee
import io.tolgee.storage.TolgeeStorageProviderAndroid

/** Identical to the Views starter: Tolgee is configured once, before any composition. */
class App : Application() {

  override fun onCreate() {
    super.onCreate()

    Tolgee.init {
      contentDelivery {
        // Content Delivery format "Android SDK" -> <cdn>/<languageTag>.json; SDK defaults match it.
        url = BuildConfig.TOLGEE_CDN_URL
        storage = TolgeeStorageProviderAndroid(this@App, BuildConfig.VERSION_CODE)
        availableLocaleTags("en", "cs", "fr", "sv")
      }
      defaultLanguage("en")
    }
  }
}
