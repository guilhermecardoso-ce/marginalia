package dev.kardoso.marginalia.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CoverSampleSizeTest {

    @Test
    fun `reduz sem ficar menor que a altura exibida`() {
        assertEquals(1, coverSampleSize(sourceHeight = 480, targetHeight = 300))
        assertEquals(2, coverSampleSize(sourceHeight = 480, targetHeight = 216))
        assertEquals(2, coverSampleSize(sourceHeight = 480, targetHeight = 240))
    }

    @Test
    fun `imagem menor que o alvo ou alvo inválido não reduz`() {
        assertEquals(1, coverSampleSize(sourceHeight = 100, targetHeight = 216))
        assertEquals(1, coverSampleSize(sourceHeight = 480, targetHeight = 0))
    }
}
