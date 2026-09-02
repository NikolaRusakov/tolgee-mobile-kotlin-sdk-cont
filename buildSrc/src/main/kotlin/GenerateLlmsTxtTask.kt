import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Turns a Dokka Markdown (GFM) publication into `llms.txt` and `llms-full.txt`
 * (see https://llmstxt.org) so that LLM coding agents can discover and read the
 * API reference without scraping HTML.
 *
 * - `llms.txt` is a compact index: one line per module, package and type, each with the
 *   first sentence of its KDoc, linking to the Markdown page.
 * - `llms-full.txt` concatenates every generated Markdown page (types and members) so an
 *   agent can load the whole reference in one request.
 *
 * The task only reads the generated Markdown tree, so it has no dependency on Dokka classes
 * and stays configuration-cache friendly.
 */
@CacheableTask
abstract class GenerateLlmsTxtTask : DefaultTask() {

    /** Root of the Dokka Markdown publication (the directory containing the top-level `index.md`). */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val markdownDir: DirectoryProperty

    /**
     * URL prefix under which [markdownDir] is published, e.g. `https://tolgee.github.io/tolgee-mobile-kotlin-sdk/markdown/`.
     * Leave empty to emit links relative to the directory that holds `llms.txt`.
     */
    @get:Input
    abstract val markdownBaseUrl: Property<String>

    /** Title used for the `# H1` of `llms.txt`. */
    @get:Input
    abstract val siteTitle: Property<String>

    /** One-paragraph description used for the `> blockquote` of `llms.txt`. */
    @get:Input
    abstract val siteDescription: Property<String>

    /** Free-form Markdown appended after the description (usage notes, coordinates, key entry points). */
    @get:Input
    abstract val notes: Property<String>

    /** Links listed under the `## Optional` section: display title to URL. */
    @get:Input
    abstract val optionalLinks: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        markdownBaseUrl.convention("")
        notes.convention("")
        optionalLinks.convention(emptyMap())
    }

    private enum class Kind { ROOT, MODULE, PACKAGE, TYPE, MEMBER }

    private data class Page(
        val relativePath: String,
        val title: String,
        val summary: String,
        val content: String,
        val kind: Kind,
    ) {
        val dir: String get() = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
        val module: String get() = relativePath.substringBefore('/')
    }

    @TaskAction
    fun generate() {
        val root = markdownDir.get().asFile
        val out = outputDir.get().asFile
        out.mkdirs()

        val pages = root.walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .map { parse(root, it) }
            // Outer types before their nested types, then alphabetically.
            .sortedWith(compareBy({ it.kind }, { it.relativePath.count { c -> c == '/' } }, { it.relativePath }))
            .toList()

        File(out, "llms.txt").writeText(renderIndex(pages))
        File(out, "llms-full.txt").writeText(renderFull(pages))
        logger.lifecycle("Generated llms.txt and llms-full.txt from ${pages.size} Markdown pages into ${out.absolutePath}")
    }

    /**
     * Dokka's multi-module Markdown layout is `index.md` (all modules), `<module>/index.md`,
     * `<module>/<module>/<package>/index.md`, `<...>/<Type>/index.md` and `<...>/<Type>/<member>.md`.
     * Depth alone is ambiguous (nested types, the doubled module directory), so pages are classified
     * by what Dokka writes into them.
     */
    private fun parse(root: File, file: File): Page {
        val relative = file.relativeTo(root).invariantSeparatorsPath
        val segments = relative.split('/')
        val content = file.readText()
        val lines = content.lines()
        val heading = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()?.let(::stripMarkdown)
        val isIndex = segments.last() == "index.md"
        val kind = when {
            segments.size == 1 -> Kind.ROOT
            !isIndex -> Kind.MEMBER
            segments.size == 2 -> Kind.MODULE
            heading == PACKAGE_HEADING || lines.any { it.startsWith("## Packages") } -> Kind.PACKAGE
            else -> Kind.TYPE
        }
        val title = when (kind) {
            Kind.ROOT -> "All modules"
            Kind.MODULE, Kind.PACKAGE -> segments[segments.size - 2]
            else -> heading ?: segments.last().removeSuffix(".md")
        }
        return Page(
            relativePath = relative,
            title = title,
            summary = if (kind == Kind.TYPE || kind == Kind.MEMBER) extractSummary(lines) else "",
            content = content,
            kind = kind,
        )
    }

    /**
     * A type or member page starts with a breadcrumb line, the `# Title`, one signature block per
     * platform (`[android]\` followed by the declaration) and then the KDoc body. The first prose
     * paragraph of that body is the best one-line description available without re-parsing KDoc.
     */
    private fun extractSummary(lines: List<String>): String {
        var afterTitle = false
        var inCode = false
        for (raw in lines) {
            val line = raw.trim()
            if (!afterTitle) {
                afterTitle = line.startsWith("# ")
                continue
            }
            if (line.startsWith("```")) {
                inCode = !inCode
                continue
            }
            if (inCode || line.isEmpty()) continue
            // Stop at the first section heading: everything below is member tables, not the description.
            if (line.startsWith("## ") || line.startsWith("#### ")) break
            if (isSignatureOrNoise(line)) continue
            val prose = stripMarkdown(line)
            if (prose.isBlank()) continue
            return firstSentence(prose)
        }
        return ""
    }

    private fun isSignatureOrNoise(line: String): Boolean {
        if (line.startsWith("//") || line.startsWith("|") || line.startsWith("[") || line.startsWith("!") ||
            line.startsWith(">") || line.startsWith("@") || line.startsWith("\\") || line.endsWith("\\")
        ) return true
        return DECLARATION_KEYWORDS.any { line.startsWith(it) }
    }

    private fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("""!\[[^\]]*]\([^)]*\)"""), "")
            .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("\\", "")
            .trim()
    }

    private fun firstSentence(text: String): String {
        val cut = Regex("""(?<=[.!?])\s""").split(text, limit = 2).first().trim()
        return if (cut.length > 240) cut.take(237).trimEnd() + "..." else cut
    }

    private fun link(page: Page): String {
        val base = markdownBaseUrl.get()
        return if (base.isEmpty()) page.relativePath else base.trimEnd('/') + "/" + page.relativePath
    }

    private fun renderIndex(pages: List<Page>): String = buildString {
        appendLine("# ${siteTitle.get()}")
        appendLine()
        appendLine("> ${siteDescription.get().trim().replace('\n', ' ')}")
        appendLine()
        val extra = notes.get().trim()
        if (extra.isNotEmpty()) {
            appendLine(extra)
            appendLine()
        }
        appendLine(
            "Every link below is a Markdown page generated from KDoc; type pages list their members with signatures. " +
                "The complete reference in one file is `llms-full.txt` next to this file."
        )
        appendLine()

        val modules = pages.filter { it.kind == Kind.MODULE }
        if (modules.isNotEmpty()) {
            appendLine("## Modules")
            appendLine()
            modules.forEach { appendLine(entry(it)) }
            appendLine()
        }

        val packages = pages.filter { it.kind == Kind.PACKAGE }
        val packageDirs = packages.map { it.dir }
        val typesByPackage = pages.filter { it.kind == Kind.TYPE }.groupBy { type ->
            packageDirs.filter { type.dir.startsWith("$it/") }.maxByOrNull { it.length }
        }
        packages.forEach { pkg ->
            appendLine("## ${pkg.module}: ${pkg.title}")
            appendLine()
            appendLine(entry(pkg, label = "Package ${pkg.title}"))
            typesByPackage[pkg.dir].orEmpty().forEach { appendLine(entry(it, indent = "  ")) }
            appendLine()
        }
        typesByPackage[null].orEmpty().takeIf { it.isNotEmpty() }?.let { orphans ->
            appendLine("## Other types")
            appendLine()
            orphans.forEach { appendLine(entry(it)) }
            appendLine()
        }

        val optional = optionalLinks.get()
        if (optional.isNotEmpty()) {
            appendLine("## Optional")
            appendLine()
            optional.forEach { (title, url) -> appendLine("- [$title]($url)") }
            appendLine()
        }
    }

    private fun entry(page: Page, label: String = page.title, indent: String = ""): String {
        val summary = page.summary.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
        return "$indent- [$label](${link(page)})$summary"
    }

    private fun renderFull(pages: List<Page>): String = buildString {
        appendLine("# ${siteTitle.get()} - full API reference")
        appendLine()
        appendLine("> ${siteDescription.get().trim().replace('\n', ' ')}")
        appendLine()
        appendLine(
            "This file concatenates every Markdown page of the API reference (modules, packages, types, then members). " +
                "Each page starts with a `Source:` line naming its URL, so you can cite or fetch it individually."
        )
        appendLine()
        pages.forEach { page ->
            appendLine("----")
            appendLine()
            appendLine("Source: ${link(page)}")
            appendLine()
            appendLine(page.content.trimEnd())
            appendLine()
        }
    }

    private companion object {
        const val PACKAGE_HEADING = "Package-level declarations"
        val DECLARATION_KEYWORDS = listOf(
            "open ", "abstract ", "sealed ", "data ", "enum ", "annotation ", "value ", "inline ", "expect ", "actual ",
            "class ", "interface ", "object ", "fun ", "val ", "var ", "typealias ", "constructor", "companion ",
            "suspend ", "operator ", "infix ", "override ", "protected ", "internal ", "public ", "private ",
        )
    }
}
