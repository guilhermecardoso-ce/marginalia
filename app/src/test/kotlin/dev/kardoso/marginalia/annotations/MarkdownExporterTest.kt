package dev.kardoso.marginalia.annotations

import dev.kardoso.marginalia.core.theme.HighlightColor
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownExporterTest {

    private val date = LocalDate.of(2026, 11, 1)

    private fun highlight(
        id: Long,
        text: String,
        chapter: String?,
        position: Int?,
        progression: Double,
        note: String? = null,
    ) = Highlight(
        id = id,
        bookId = 1,
        locatorJson = "{}",
        text = text,
        chapter = chapter,
        position = position,
        progression = progression,
        color = HighlightColor.AMARELO,
        note = note,
        createdAt = 0,
    )

    private val sample = listOf(
        highlight(3, "Ter a teoria de um programa é saber explicar suas escolhas", "3. Programar é construir teoria", 47, 0.41, note = "Comparar com conhecimento tácito em Polanyi."),
        highlight(1, "Lemos código para reconstruir intenções.", "2. Ler código como texto", 23, 0.20),
        highlight(2, "nenhum comentário recupera", "3. Programar é construir teoria", 47, 0.40),
    )

    @Test
    fun `gera frontmatter, título, capítulos em ordem e notas`() {
        val markdown = MarkdownExporter.export("Notas sobre o método", "Ana Ferraz", sample, ExportOptions(), date)

        val expected = """
            ---
            title: Notas sobre o método
            author: Ana Ferraz
            tags: [livro, marginalia]
            exported: 2026-11-01
            ---

            # Notas sobre o método

            ## 2. Ler código como texto

            > Lemos código para reconstruir intenções.
            > — p. 23

            ## 3. Programar é construir teoria

            > nenhum comentário recupera
            > — p. 47

            > Ter a teoria de um programa é saber explicar suas escolhas
            > — p. 47

            Comparar com conhecimento tácito em Polanyi.

        """.trimIndent()
        assertEquals(expected, markdown)
    }

    @Test
    fun `opções filtram destaques, notas e páginas`() {
        val onlyNotes = MarkdownExporter.export("Livro", null, sample, ExportOptions(highlights = false, pageNumbers = false), date)
        assertFalse(onlyNotes.contains("nenhum comentário recupera"))
        assertTrue(onlyNotes.contains("Comparar com conhecimento tácito"))
        assertFalse(onlyNotes.contains("— p."))
        assertFalse(onlyNotes.contains("author:"))

        val onlyHighlights = MarkdownExporter.select(sample, ExportOptions(notes = false))
        assertEquals(listOf(1L, 2L), onlyHighlights.map { it.id })
    }

    @Test
    fun `trecho com várias linhas vira uma citação contínua`() {
        val markdown = MarkdownExporter.export(
            "Livro",
            null,
            listOf(highlight(1, "primeira linha\n\nsegunda linha", null, null, 0.1)),
            ExportOptions(),
            date,
        )
        assertTrue(markdown.contains("> primeira linha\n>\n> segunda linha\n"))
    }

    @Test
    fun `títulos com caracteres especiais ficam entre aspas no YAML`() {
        val markdown = MarkdownExporter.export("Ética: demonstrada \"à maneira\" geométrica", null, emptyList(), ExportOptions(), date)
        assertTrue(markdown.contains("title: \"Ética: demonstrada \\\"à maneira\\\" geométrica\"\n"))
    }

    @Test
    fun `nome do arquivo sem acentos e em minúsculas`() {
        assertEquals("notas-sobre-o-metodo.md", MarkdownExporter.fileName("Notas sobre o método"))
        assertEquals("critica-da-razao-pura.md", MarkdownExporter.fileName("Crítica da Razão Pura!"))
        assertEquals("anotacoes.md", MarkdownExporter.fileName("???"))
    }
}
