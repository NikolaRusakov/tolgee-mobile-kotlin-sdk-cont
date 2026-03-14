package io.tolgee.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlValue
import io.tolgee.model.TolgeeKey
import nl.adaptivity.xmlutil.serialization.XML

// ---------------------------------------------------------------------------
// XML data-class model
// ---------------------------------------------------------------------------

/**
 * Root <resources> element.
 * xmlutil maps child elements whose tag names differ by choosing them via
 * polymorphic / manual descriptors, so we collect all three kinds into
 * separate lists and merge them afterward.
 */
@Serializable
@SerialName("resources")
internal data class XmlResources(
  @SerialName("string")
  val strings: List<XmlString> = emptyList(),

  @SerialName("string-array")
  val stringArrays: List<XmlStringArray> = emptyList(),

  @SerialName("plurals")
  val plurals: List<XmlPlurals> = emptyList(),
)

/** <string name="key">value</string> */
@Serializable
@SerialName("string")
internal data class XmlString(
  @SerialName("name")
  val name: String,

  /** The text content of the element. */
  @XmlValue(true)
  val value: String = "",
)

/** <string-array name="key"><item>…</item>…</string-array> */
@Serializable
@SerialName("string-array")
internal data class XmlStringArray(
  @SerialName("name")
  val name: String,

  @SerialName("item")
  val items: List<XmlItem> = emptyList(),
)

/** <plurals name="key"><item quantity="one">…</item>…</plurals> */
@Serializable
@SerialName("plurals")
internal data class XmlPlurals(
  @SerialName("name")
  val name: String,

  @SerialName("item")
  val items: List<XmlPluralItem> = emptyList(),
)

/** Plain <item> inside a string-array. */
@Serializable
@SerialName("item")
internal data class XmlItem(
  @XmlValue(true)
  val value: String = "",
)

/** Quantity-keyed <item quantity="one|few|many|other|…"> inside a plurals. */
@Serializable
@SerialName("item")
internal data class XmlPluralItem(
  @SerialName("quantity")
  val quantity: String,

  @XmlValue(true)
  val value: String = "",
)

// ---------------------------------------------------------------------------
// Decoder
// ---------------------------------------------------------------------------

/**
 * Decodes an Android `res/values/strings.xml` file into a list of [TolgeeKey]
 * objects, using the `io.github.pdvrieze.xmlutil:serialization` library.
 *
 * The XML format contains three resource kinds:
 *  - `<string>`       → [TolgeeKey.Data.Text]
 *  - `<string-array>` → [TolgeeKey.Data.Array]
 *  - `<plurals>`      → [TolgeeKey.Data.Plural]
 *
 * @param xmlContent   Raw XML string (UTF-8 text of the file).
 * @param languageCode The BCP-47 language tag to use as the map key inside
 *                     [TolgeeKey.translations] (e.g. `"cs"`, `"en"`, `"de"`).
 *                     Defaults to `"default"`.
 * @return             A list of [TolgeeKey] ready to be used by the Tolgee SDK.
 */
fun decodeAndroidXml(
  xmlContent: String,
  languageCode: String = "default",
): List<TolgeeKey> {
  val xml = XML {
    // Be lenient about unknown attributes (e.g. `formatted="false"`)
//    unknownChildHandler = { _, _, _, _, _ -> }
  }

  val resources: XmlResources = xml.decodeFromString(XmlResources.serializer(), xmlContent)

  return buildList {
    // ── <string> ──────────────────────────────────────────────────────
    resources.strings.forEach { s ->
      add(
        TolgeeKey(
          keyName = s.name,
          translations = mapOf(languageCode to TolgeeKey.Data.Text(s.value)),
        )
      )
    }

    // ── <string-array> ────────────────────────────────────────────────
    resources.stringArrays.forEach { arr ->
      add(
        TolgeeKey(
          keyName = arr.name,
          translations = mapOf(
            languageCode to TolgeeKey.Data.Array(arr.items.map { it.value })
          ),
        )
      )
    }

    // ── <plurals> ─────────────────────────────────────────────────────
    resources.plurals.forEach { p ->
      add(
        TolgeeKey(
          keyName = p.name,
          translations = mapOf(
            languageCode to TolgeeKey.Data.Plural(
              plurals = p.items.associate { it.quantity to it.value }
            )
          ),
        )
      )
    }
  }
}

// ---------------------------------------------------------------------------
// Convenience overload – read from a java.io.File / java.io.InputStream
// ---------------------------------------------------------------------------

/**
 * Reads an XML file from [path] and delegates to [decodeAndroidXml].
 */
fun decodeAndroidXmlFile(
  content: String,
  languageCode: String = "default",
): List<TolgeeKey> =
  decodeAndroidXml(content, languageCode)

internal fun String.parseAndroidXml(
//  content: String,
  languageCode: String = "default",
): List<TolgeeKey> {
  return decodeAndroidXmlFile(
    this,
    languageCode
  )
}
