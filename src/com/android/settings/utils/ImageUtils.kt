/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    fun saveImageToInternalStorage(context: Context, imgUri: Uri, featurePath: String, filePrefix: String): String? {
        return try {
            val inputStream: InputStream = if (imgUri.toString().startsWith("content://com.google.android.apps.photos.contentprovider")) {
                val segments = imgUri.pathSegments
                if (segments.size > 2) {
                    val mediaUriString = URLDecoder.decode(segments[2], StandardCharsets.UTF_8.name())
                    val mediaUri = Uri.parse(mediaUriString)
                    context.contentResolver.openInputStream(mediaUri)!!
                } else {
                    throw FileNotFoundException("Failed to parse Google Photos content URI")
                }
            } else {
                context.contentResolver.openInputStream(imgUri)!!
            }
            
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "${filePrefix}_${timeStamp}.png"
            val directory = File("/sdcard/risingOS/$featurePath")
            
            if (!directory.exists() && !directory.mkdirs()) {
                return null
            }
            
            val files = directory.listFiles { _, name -> name.startsWith(filePrefix) && name.endsWith(".png") }
            files?.forEach { it.delete() }
            
            val file = File(directory, imageFileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}