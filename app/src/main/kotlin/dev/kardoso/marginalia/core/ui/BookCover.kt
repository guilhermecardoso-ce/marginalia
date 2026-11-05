package dev.kardoso.marginalia.core.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kardoso.marginalia.core.theme.Literata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val COVER_CACHE_ENTRIES = 32

/** Tons sóbrios para capas sem imagem; escolhidos pelo título, sempre o mesmo para o mesmo livro. */
private val PlaceholderTones = listOf(
    Color(0xFF2F3A33),
    Color(0xFF4A4038),
    Color(0xFF2E4555),
    Color(0xFF5C5040),
    Color(0xFF6B5D4D),
    Color(0xFF3F3A4A),
)

private val CoverCache = LruCache<String, ImageBitmap>(COVER_CACHE_ENTRIES)

private fun coverKey(path: String, heightPx: Int) = "$path@$heightPx"

private fun cachedCover(path: String?, heightPx: Int): ImageBitmap? =
    path?.let { CoverCache.get(coverKey(it, heightPx)) }

/** Decodifica só o necessário para a altura exibida: uma miniatura não precisa da capa inteira na memória. */
private suspend fun loadCover(path: String, heightPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = coverSampleSize(bounds.outHeight, heightPx)
        }
        BitmapFactory.decodeFile(path, options)?.asImageBitmap()?.also {
            CoverCache.put(coverKey(path, heightPx), it)
        }
    }.getOrNull()
}

/** Maior potência de 2 que ainda deixa a imagem com pelo menos a altura pedida. */
internal fun coverSampleSize(sourceHeight: Int, targetHeight: Int): Int {
    var sample = 1
    while (targetHeight > 0 && sourceHeight / (sample * 2) >= targetHeight) sample *= 2
    return sample
}

@Composable
fun BookCover(
    title: String,
    coverPath: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val heightPx = with(LocalDensity.current) { height.roundToPx() }
    var image by remember(coverPath, heightPx) { mutableStateOf(cachedCover(coverPath, heightPx)) }
    LaunchedEffect(coverPath, heightPx) {
        if (image == null && coverPath != null) image = loadCover(coverPath, heightPx)
    }
    val shape = RoundedCornerShape(if (width > 56.dp) 4.dp else 3.dp)
    val loaded = image
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(width, height).clip(shape),
        )
    } else {
        val tone = PlaceholderTones[(title.hashCode() and Int.MAX_VALUE) % PlaceholderTones.size]
        Box(
            modifier = modifier
                .size(width, height)
                .clip(shape)
                .background(tone)
                .padding(if (width > 56.dp) 8.dp else 5.dp),
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = Literata,
                    fontSize = if (width > 56.dp) 10.sp else 7.sp,
                    lineHeight = if (width > 56.dp) 12.sp else 8.5.sp,
                    color = Color(0xFFF3EEE3),
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 5,
            )
        }
    }
}
