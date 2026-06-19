package io.tolgee.demo.exampleandroid

import android.app.Application
import de.comahe.i18n4k.createLocale
import io.tolgee.Tolgee
import io.tolgee.storage.TolgeeStorageProviderAndroid

class MyApplication : Application() {

  override fun onCreate() {
    super.onCreate()

    val locales = resources.getStringArray(R.array.locale_values)
    val availableLocales = locales.map {
      val (language, countryOr, script) = it.replace("-r", "-").split("-".toRegex(), 2)
        .padWithNulls(3)
      val country = (script ?: "").ifEmpty { countryOr.orEmpty() }
      createLocale(language.orEmpty(), script, country)
    }

    Tolgee.init {
      contentDelivery {
        url = "https://cdn.tolg.ee/dbbedc13592d9ea9945332d83c1dc800"
        path = { "$it.json" }
        storage = TolgeeStorageProviderAndroid(this@MyApplication, BuildConfig.VERSION_CODE)
        availableLocales(availableLocales)
      }
      defaultLanguage("cs")
    }
  }
}

inline fun <reified E> List<E>.padWithNulls(limit: Int): List<E?> {
  if (this.size >= limit) {
    return this
  }
  return this.toMutableList() + Array<E?>(limit - this.size) { null }
}