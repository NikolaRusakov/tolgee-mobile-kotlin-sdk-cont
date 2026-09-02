import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.dokka.gradle.formats.DokkaFormatPlugin
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask

plugins {
    alias(libs.plugins.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.atomicfu) apply false
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.cocoapods) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktorfit) apply false
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.vanniktech.publish) apply false
    alias(libs.plugins.versions)
}

// Ignore API on demo projects
apiValidation {
    ignoredProjects.add("demo")
    ignoredProjects.add("multiplatform-compose")
    ignoredProjects.add("exampleandroid")
    ignoredProjects.add("examplejetpack")
}

dependencies {
    dokka(project(":core"))
    dokka(project(":compose"))
    dokka(project(":gradle-plugin"))
//    dokka(project(":compiler-plugin"))
}

// Force new atomicfu version, compose uses 0.23.2
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("atomicfu")) {
                useVersion(libs.versions.atomicfu.get())
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dokka {
    moduleName.set("Tolgee Mobile Kotlin SDK")
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }
    // Configured here only: the root project governs the aggregated publication.
    pluginsConfiguration.html {
        // Must be named logo-icon.svg — Dokka hardcodes that path for the nav logo and the favicon.
        customAssets.from("docs/logo-icon.svg")
        customStyleSheets.from("docs/tolgee.css")
        homepageLink.set("https://tolgee.io/")
        footerMessage.set("© 2021-2026 Tolgee s.r.o. All rights reserved")
    }
}

// ---------------------------------------------------------------------------------------------
// LLM-friendly API reference: Markdown (GFM) twin of the HTML docs plus llms.txt / llms-full.txt
// (https://llmstxt.org). `./gradlew dokkaSite` assembles build/dokka/site, which the wiki
// workflow publishes to GitHub Pages (see AGENTS.md, "API documentation"):
//   /                 HTML reference (unchanged)
//   /markdown/**.md   one Markdown page per module, package, type and member
//   /llms.txt         index of the Markdown pages with one-line KDoc summaries
//   /llms-full.txt    every Markdown page concatenated, for agents that want the whole API at once
// ---------------------------------------------------------------------------------------------

/**
 * Dokka 2.0.0's Gradle plugin only registers the HTML and Javadoc formats, but the Markdown
 * renderer still ships as the `gfm-plugin` engine plugin. This is the approach Dokka's own
 * plugin-gfm README recommends for DGP v2: a DokkaFormatPlugin registering a `markdown` publication
 * (tasks `dokkaGenerateModuleMarkdown` / `dokkaGeneratePublicationMarkdown`, output
 * `build/dokka/markdown`). Remove once Dokka ships an `org.jetbrains.dokka-gfm` plugin id.
 */
@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("gfm-plugin"))
        }
        // Like DokkaHtmlPlugin: every module advertises the aggregation plugins on its shared
        // "api only" classpath, and the aggregating publication picks them up from its module deps.
        listOf("gfm-template-processing-plugin", "all-modules-page-plugin").forEach { module ->
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokkaExtension.dokkaEngineVersion.map { v -> project.dependencies.create("org.jetbrains.dokka:$module:$v") }
            )
        }
    }
}

allprojects {
    plugins.withId("org.jetbrains.dokka") {
        apply<DokkaMarkdownPlugin>()
    }
}

val dokkaLlmsTxt by tasks.registering(GenerateLlmsTxtTask::class) {
    group = "documentation"
    description = "Generates llms.txt and llms-full.txt from the Dokka Markdown publication."
    markdownDir.set(tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationMarkdown").flatMap { it.outputDirectory })
    // Absolute URL of the published /markdown directory; CI passes the GitHub Pages URL.
    markdownBaseUrl.set(providers.gradleProperty("dokka.llms.markdownBaseUrl").orElse("markdown/"))
    siteTitle.set("Tolgee Mobile Kotlin SDK API reference")
    siteDescription.set(
        "KDoc API reference of the Tolgee Mobile Kotlin SDK (version $libVersion): over-the-air translation " +
            "updates for Android Views, Jetpack Compose and Compose Multiplatform apps, backed by the Tolgee " +
            "Platform content delivery CDN."
    )
    notes.set(
        """
        Maven coordinates: `io.tolgee.mobile-kotlin-sdk:core:$libVersion` (Views / plain Kotlin) and
        `io.tolgee.mobile-kotlin-sdk:compose:$libVersion` (Jetpack Compose, Compose Multiplatform; depends on core).

        Where to start:
        - `io.tolgee.Tolgee` - singleton entry point: `Tolgee.init { contentDelivery { url = ... } }`, `t()`, `tFlow()`, `setLocale()`, `changeFlow`.
        - `io.tolgee.Tolgee.Config.Builder` / `ContentDelivery.Builder` - every init option (url, path, storage, formatter, availableLocales, defaultLanguage).
        - `io.tolgee.TolgeeAndroid` - Android-only overloads taking `R.string` / `R.plurals` ids, `preload()`, `retranslate()`.
        - `io.tolgee.TolgeeContextWrapper` - wraps an Activity context so `getString()` and XML layouts resolve through Tolgee.
        - `io.tolgee.stringResource` / `pluralStringResource` (compose module) - drop-in replacements for the Compose resource composables.

        Task-oriented guides (install, migrate an app, troubleshoot) live in the documentation site: https://docs.tolgee.io/android-sdk (Markdown index: https://docs.tolgee.io/llms.txt).
        """.trimIndent()
    )
    optionalLinks.set(
        linkedMapOf(
            "Android SDK documentation (guides, install, migration recipes)" to "https://docs.tolgee.io/android-sdk",
            "Tolgee documentation index for LLMs (llms.txt)" to "https://docs.tolgee.io/llms.txt",
            "Source code and demo apps" to "https://github.com/tolgee/tolgee-mobile-kotlin-sdk",
            "Releases on Maven Central" to "https://central.sonatype.com/search?namespace=io.tolgee.mobile-kotlin-sdk",
        )
    )
    outputDir.set(layout.buildDirectory.dir("dokka/llms"))
}

val dokkaSite by tasks.registering(Sync::class) {
    group = "documentation"
    description = "Assembles the publishable API docs site: HTML reference, Markdown mirror under /markdown and llms.txt files."
    from(tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml").flatMap { it.outputDirectory })
    from(tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationMarkdown").flatMap { it.outputDirectory }) {
        into("markdown")
    }
    from(dokkaLlmsTxt)
    into(layout.buildDirectory.dir("dokka/site"))
}

tasks.withType<DependencyUpdatesTask> {
    outputFormatter {
        val updatable = this.outdated.dependencies
        val markdown = if (updatable.isEmpty()) {
            buildString {
                append("### Dependencies up-to-date")
                appendLine()
                appendLine()
                appendLine("Everything up-to-date")
                appendLine()
                appendLine("### Gradle Version")
                appendLine()
                appendLine("**Current version:** ${this@outputFormatter.gradle.running.version}")
                appendLine("**Latest version:** ${this@outputFormatter.gradle.current.version}")
            }
        } else {
            buildString {
                append("## Updatable dependencies (${updatable.size})")
                appendLine()
                appendLine()
                append('|')
                append("Group")
                append('|')
                append("Module")
                append('|')
                append("Used Version")
                append('|')
                append("Available Version")
                append('|')
                appendLine()
                append('|')
                repeat(2) {
                    append("---")
                    append('|')
                }
                repeat(2) {
                    append(":-:")
                    append('|')
                }
                updatable.forEach { dependency ->
                    appendLine()
                    append('|')
                    append(dependency.group ?: ' ')
                    append('|')
                    append(dependency.name ?: ' ')
                    append('|')
                    append(dependency.version ?: ' ')
                    append('|')
                    append(dependency.available.release ?: dependency.available.milestone ?: ' ')
                    append('|')
                }
                appendLine()
                appendLine()
                appendLine("### Gradle Version")
                appendLine()
                appendLine("**Current version:** ${this@outputFormatter.gradle.running.version}")
                appendLine("**Latest version:** ${this@outputFormatter.gradle.current.version}")
            }
        }
        val outputFile = layout.buildDirectory.file("dependencyUpdates/report.md").get().asFile
        try {
            if (outputFile.exists()) {
                outputFile.delete()
            }
        } catch (ignored: Throwable) { }
        try {
            outputFile.parentFile?.mkdirs()
        } catch (ignored: Throwable) { }
        try {
            outputFile.writeText(markdown)
        } catch (ignored: Throwable) { }
    }
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}