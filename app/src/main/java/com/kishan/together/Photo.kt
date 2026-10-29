package com.kishan.together

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

private const val SIZE = 256

/** Crops a picked image to a small square JPEG and returns it as base64, ready to store in the database. */
fun encodePhoto(ctx: Context, uri: Uri): String? = runCatching {
    val src = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val s = minOf(info.size.width, info.size.height) / SIZE
            if (s > 1) d.setTargetSampleSize(s)
        }
    } else {
        ctx.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })!!
        }
    }
    val side = minOf(src.width, src.height)
    val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val out = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
    val bytes = ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
    Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrNull()

fun decodePhoto(b64: String): ImageBitmap? = runCatching {
    val b = Base64.decode(b64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(b, 0, b.size).asImageBitmap()
}.getOrNull()
