package dev.kardoso.marginalia.annotations

import dev.kardoso.marginalia.FakeAnnotationDao
import dev.kardoso.marginalia.core.Clock
import dev.kardoso.marginalia.core.theme.HighlightColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AnnotationRepositoryTest {

    private val dao = FakeAnnotationDao()
    private val repository = AnnotationRepository(dao, Clock { 42L })

    private fun point(position: Int?, progression: Double) = BookPoint(
        locatorJson = """{"href":"cap.xhtml","locations":{"position":$position}}""",
        chapter = "3. Programar é construir teoria",
        position = position,
        progression = progression,
    )

    @Test
    fun `destaque guarda trecho limpo, cor e nota normalizada`() = runTest {
        val id = repository.addHighlight(1, point(47, 0.4), "  nenhum comentário recupera \n", HighlightColor.VERDE, note = "   ")
        val highlight = repository.highlight(id)!!

        assertEquals("nenhum comentário recupera", highlight.text)
        assertEquals(HighlightColor.VERDE, highlight.color)
        assertNull(highlight.note)
        assertFalse(highlight.hasNote)
        assertEquals(47, highlight.position)
        assertEquals(42L, highlight.createdAt)
    }

    @Test
    fun `nota vazia apaga a nota`() = runTest {
        val id = repository.addHighlight(1, point(47, 0.4), "trecho", HighlightColor.AMARELO, note = "Comparar com Polanyi.")
        assertTrue(repository.highlight(id)!!.hasNote)

        repository.setNote(id, "  ")
        assertNull(repository.highlight(id)!!.note)

        repository.setNote(id, "  Nova nota  ")
        assertEquals("Nova nota", repository.highlight(id)!!.note)
    }

    @Test
    fun `destaques de um livro vêm na ordem do texto`() = runTest {
        repository.addHighlight(1, point(90, 0.9), "depois", HighlightColor.AZUL)
        repository.addHighlight(1, point(10, 0.1), "antes", HighlightColor.ROSA)
        repository.addHighlight(2, point(5, 0.05), "outro livro", HighlightColor.ROSA)

        val texts = repository.highlights(1).first().map { it.text }
        assertEquals(listOf("antes", "depois"), texts)
    }

    @Test
    fun `mudar cor e excluir destaque`() = runTest {
        val id = repository.addHighlight(1, point(1, 0.0), "trecho", HighlightColor.AMARELO)
        repository.setColor(id, HighlightColor.ROSA)
        assertEquals(HighlightColor.ROSA, repository.highlight(id)!!.color)

        repository.deleteHighlight(id)
        assertNull(repository.highlight(id))
    }

    @Test
    fun `marcador alterna na mesma página`() = runTest {
        val page = point(47, 0.40)

        assertTrue(repository.toggleBookmark(1, page, repository.bookmarks(1).first()))
        assertEquals(1, repository.bookmarks(1).first().size)

        assertFalse(repository.toggleBookmark(1, page, repository.bookmarks(1).first()))
        assertEquals(0, repository.bookmarks(1).first().size)
    }

    @Test
    fun `marcador sem posição compara pelo progresso`() = runTest {
        repository.toggleBookmark(1, point(null, 0.5000), emptyList())
        val bookmarks = repository.bookmarks(1).first()

        assertEquals(bookmarks.first(), AnnotationRepository.findBookmarkAt(point(null, 0.5002), bookmarks))
        assertNull(AnnotationRepository.findBookmarkAt(point(null, 0.51), bookmarks))
        assertNull(AnnotationRepository.findBookmarkAt(point(48, 0.5), listOf(bookmarks.first().copy(position = 47))))
    }
}
