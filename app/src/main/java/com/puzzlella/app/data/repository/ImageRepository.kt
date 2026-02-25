package com.puzzlella.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageRepository(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, "puzzle_images").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("Cannot read image from URI")

        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    suspend fun saveThumbnail(imagePath: String): String = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeFile(imagePath)
        val scale = 200f / maxOf(original.width, original.height)
        val thumbnail = Bitmap.createScaledBitmap(
            original,
            (original.width * scale).toInt(),
            (original.height * scale).toInt(),
            true
        )

        val fileName = "thumb_${UUID.randomUUID()}.jpg"
        val file = File(thumbnailsDir, fileName)
        FileOutputStream(file).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        thumbnail.recycle()
        file.absolutePath
    }

    fun loadBitmap(path: String): Bitmap? =
        BitmapFactory.decodeFile(path)

    fun getSampleImages(): List<String> {
        val assetManager = context.assets
        return try {
            assetManager.list("samples")?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun copySampleToStorage(assetName: String): String = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        context.assets.open("samples/$assetName").use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    }
}
