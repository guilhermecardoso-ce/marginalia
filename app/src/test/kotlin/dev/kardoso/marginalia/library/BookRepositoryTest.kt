package dev.kardoso.marginalia.library

import dev.kardoso.marginalia.FakeBookDao
import dev.kardoso.marginalia.core.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class BookRepositoryTest {

    private var now = 1_000L
    private val dao = FakeBookDao()
    private val repository = BookRepository(dao, Clock { now })

    private fun newBook(uri: String = "content://livro.epub", title: String = "Ética") = NewBook(
        uri = uri,
        title = title,
        author = "Bento de Espinosa",
        format = BookFormat.EPUB,
        mediaType = "application/epub+zip",
        coverPath = null,
    )

    @Test
    fun `adicionar o mesmo URI duas vezes não duplica o livro`() = runTest {
        val first = repository.add(newBook())
        val second = repository.add(newBook())

        assertIs<AddBookResult.Added>(first)
        assertIs<AddBookResult.AlreadyInLibrary>(second)
        assertEquals(first.id, second.id)
        assertEquals(1, repository.books.first().size)
    }

    @Test
    fun `livro novo começa sem posição e não iniciado`() = runTest {
        val id = repository.add(newBook()).id
        val book = repository.get(id)!!

        assertNull(book.locatorJson)
        assertEquals(0.0, book.progression)
        assertEquals(false, book.isStarted)
        assertEquals(1_000L, book.addedAt)
    }

    @Test
    fun `título e autor são limpos`() = runTest {
        val id = repository.add(newBook(title = "  Meditações  ").copy(author = "   ")).id
        val book = repository.get(id)!!

        assertEquals("Meditações", book.title)
        assertNull(book.author)
    }

    @Test
    fun `salvar progresso guarda o locator e limita o progresso entre 0 e 1`() = runTest {
        val id = repository.add(newBook()).id
        now = 5_000L

        repository.saveProgress(id, """{"href":"cap3.xhtml"}""", 1.7)
        var book = repository.get(id)!!
        assertEquals("""{"href":"cap3.xhtml"}""", book.locatorJson)
        assertEquals(1.0, book.progression)
        assertEquals(5_000L, book.lastOpenedAt)
        assertTrue(book.isFinished)

        repository.saveProgress(id, """{"href":"cap1.xhtml"}""", null)
        book = repository.get(id)!!
        assertEquals(0.0, book.progression)

        repository.saveProgress(id, """{"href":"cap2.xhtml"}""", Double.NaN)
        assertEquals(0.0, repository.get(id)!!.progression)
    }

    @Test
    fun `lista vem do mais recente para o mais antigo`() = runTest {
        val a = repository.add(newBook(uri = "content://a", title = "A")).id
        now = 2_000L
        val b = repository.add(newBook(uri = "content://b", title = "B")).id
        now = 3_000L
        repository.markOpened(a)

        val order = repository.books.first().map { it.id }
        assertEquals(listOf(a, b), order)
    }

    @Test
    fun `remover apaga o livro`() = runTest {
        val id = repository.add(newBook()).id
        repository.delete(id)
        assertNull(repository.get(id))
    }
}
