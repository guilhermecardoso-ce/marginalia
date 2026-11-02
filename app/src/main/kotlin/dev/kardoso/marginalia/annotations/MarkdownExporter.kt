package dev.kardoso.marginalia.annotations

import java.text.Normalizer
import java.time.LocalDate

data class ExportOptions(
    /** Destaques sem nota. */
    val highlights: Boolean = true,
    /** Destaques com nota (trecho + nota). */
    val notes: Boolean = true,
    val pageNumbers: Boolean = true,
)

/**
 * Gera o Markdown das anotações, compatível com Obsidian: frontmatter YAML, um título por livro,
 * uma seção por capítulo, cada trecho como citação e a nota logo abaixo.
 */
object MarkdownExporter {

    fun export(
        title: String,
        author: String?,
        highlights: List<Highlight>,
        options: ExportOptions,
        date: LocalDate,
    ): String = buildString {
        appendLine("---")
        appendLine("title: ${yamlScalar(title)}")
        if (!author.isNullOrBlank()) appendLine("author: ${yamlScalar(author)}")
        appendLine("tags: [livro, marginalia]")
        appendLine("exported: $date")
        appendLine("---")
        appendLine()
        appendLine("# ${title.trim()}")

        val selected = select(highlights, options)
        var currentChapter: String? = null
        var first = true
        for (highlight in selected) {
            val chapter = highlight.chapter?.trim()?.ifEmpty { null }
            if (first || chapter != currentChapter) {
                if (chapter != null) {
                    appendLine()
                    appendLine("## $chapter")
                }
                currentChapter = chapter
                first = false
            }
            appendLine()
            highlight.text.trim().lines().forEach { line ->
                appendLine(if (line.isBlank()) ">" else "> ${line.trim()}")
            }
            if (options.pageNumbers && highlight.position != null) {
                appendLine("> — p. ${highlight.position}")
            }
            if (highlight.hasNote) {
                appendLine()
                appendLine(highlight.note!!.trim())
            }
        }
    }

    fun select(highlights: List<Highlight>, options: ExportOptions): List<Highlight> =
        highlights
            .filter { if (it.hasNote) options.notes else options.highlights }
            .sortedWith(compareBy({ it.progression }, { it.id }))

    fun fileName(title: String): String {
        val slug = Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(80)
            .trimEnd('-')
        return "${slug.ifEmpty { "anotacoes" }}.md"
    }

    private fun yamlScalar(value: String): String {
        val text = value.trim()
        val needsQuotes = text.isEmpty() ||
            text.first() in "!&*-?{}[]|>'\"%@`#," ||
            text.contains(": ") || text.contains(" #") || text.endsWith(":") ||
            text.lowercase() in setOf("true", "false", "null", "yes", "no", "~")
        if (!needsQuotes) return text
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
