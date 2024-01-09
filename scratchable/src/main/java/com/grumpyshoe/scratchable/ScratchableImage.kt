package com.grumpyshoe.scratchable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * based on https://gist.github.com/ardakazanci/22290e6c4f69dd5274e3edf9690c118d from ardakazanci/ScratchEffect.kt
 *
 */

@Composable
fun ScratchableImage(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int,
    eraserRadius: Dp = 50.dp,
    onTouch: () -> Unit
) {

    // Log.d("fdsfds", "rrr recompose id: $id; image:$image")

    val context = LocalContext.current
    var touched by remember { mutableStateOf(false) }
    var touchPoints by remember { mutableStateOf(listOf<Offset>()) }
    var scratchBitmapInit by remember { mutableStateOf<ImageBitmap?>(null) }
    var scratchBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val imageBitmap: ImageBitmap = context.resources.getDrawable(image, context.theme).let { drawable ->

        if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
    }.asImageBitmap()

    LaunchedEffect(componentSize) {
        val scratchView = createGradientScratchLayer(componentSize)
        scratchBitmap = scratchView
        scratchBitmapInit = scratchView
    }

    LaunchedEffect(imageBitmap) {
        scratchBitmap = scratchBitmapInit
        touchPoints = emptyList()
    }

    LaunchedEffect(touched) {
        if (touched) {
            onTouch()
        }
    }



    // Scratch Image
    Image(
        bitmap = imageBitmap,
        contentDescription = "Scratch Image",
        modifier = modifier
            .onGloballyPositioned {
                componentSize = it.size
            }
            .drawWithContent {
                drawContent()
                scratchBitmap?.let {
                    drawImage(
                        image = it,
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    touchPoints = touchPoints + change.position
                    scratchBitmap?.let {
                        val modifiedBitmap = erase(it.asAndroidBitmap(), touchPoints, eraserRadius.toPx())
                        scratchBitmap = modifiedBitmap.asImageBitmap()
                    }

                }
            }
            .pointerInput(Unit) {                //<-- Suspends block
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            onTouch()
                        }
                    }
                }
            }
    )
}

fun createGradientScratchLayer(size: IntSize): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val colors = intArrayOf(Color.DKGRAY, Color.GRAY, Color.DKGRAY)
    val positions = floatArrayOf(0f, 0.5f, 1f)

    val gradient = LinearGradient(
        0f,
        0f,
        size.width.toFloat(),
        size.height.toFloat(),
        colors,
        positions,
        Shader.TileMode.CLAMP
    )
    val paint = Paint()
    paint.setShader(gradient)

    canvas.drawPaint(paint)

    return bitmap.asImageBitmap()
}
//}

// Brush Effect for Erase
private fun erase(bitmap: Bitmap, touchPoints: List<Offset>, radius: Float): Bitmap {

    val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
        strokeWidth = radius
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val canvas = Canvas(bitmap)
    val path = Path()

    touchPoints.forEachIndexed { index, point ->
        val adjustedX = point.x
        val adjustedY = point.y
        if (index == 0) {
            path.moveTo(adjustedX, adjustedY)
        } else {
            path.lineTo(adjustedX, adjustedY)
        }
    }

    canvas.drawPath(path, paint)

    return bitmap
}