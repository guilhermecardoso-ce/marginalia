package dev.kardoso.marginalia.core.ui

import android.graphics.BitmapFactory
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kardoso.marginalia.core.theme.Literata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tons sóbrios para capas sem imagem; escolhidos pelo título, sempre o mesmo para o mesmo livro. */
private val PlaceholderTones = listOf(
    Color(0xFF2F3A33),
    Color(0xFF4A4038),
    Color(0xFF2E4555),
    Color(0xFF5C5040),
    Color(0xFF6B5D4D),
    Color(0xFF3F3A4A),
)

@Composable
fun BookCover(
    title: String,
    coverPath: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    var image by remember(coverPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(coverPath) {
        image = coverPath?.let { path ->
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
        }
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
