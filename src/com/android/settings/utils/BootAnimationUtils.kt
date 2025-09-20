/*
 * Copyright (C) 2024 risingOS
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
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.AnimatedImageDrawable
import android.os.SystemProperties
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.regex.Pattern

object BootAnimationUtils {

    private const val TAG = "BootAnimationUtils"
    private const val DEFAULT_FRAME_DURATION = 1000 / 30

    private val BOOT_ANIMATION_FILES = arrayOf(
        "/product/media/bootanimation_rising.zip",
        "/product/media/bootanimation_cyberpunk.zip",
        "/product/media/bootanimation_google.zip",
        "/product/media/bootanimation_google_monet.zip",
        "/product/media/bootanimation_valorant.zip",
        "/data/misc/bootanim/bootanimation.zip"
    )

    fun getBootAnimationFrames(context: Context): AnimationDrawable {
        val animationDrawable = AnimationDrawable()
        val selectedBootAnimation = getSelectedBootAnimation()
        if (selectedBootAnimation != null) {
            val bootAnimationFile = File(selectedBootAnimation)
            if (bootAnimationFile.exists()) {
                try {
                    ZipFile(bootAnimationFile).use { zipFile ->
                        val frameDuration = getFrameDuration(zipFile)
                        val partCount = getPartCount(zipFile)
                        if (partCount == 0) {
                            val trimData = loadTrimData(zipFile, "part0")
                            loadFramesFromPart(context, zipFile, animationDrawable, "part0", frameDuration, trimData)
                        } else {
                            for (i in 0 until partCount) {
                                val partName = "part$i"
                                val trimData = loadTrimData(zipFile, partName)
                                loadFramesFromPart(context, zipFile, animationDrawable, partName, frameDuration, trimData)
                            }
                        }
                        animationDrawable.setOneShot(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading boot animation frames", e)
                }
            }
        }
        return animationDrawable
    }

    fun getBootAnimStyle(): Int {
        return SystemProperties.getInt("persist.sys.bootanimation_style", 0)
    }

    fun getSelectedBootAnimation(): String? {
        val style = getBootAnimStyle()
        return if (style >= 0 && style < BOOT_ANIMATION_FILES.size) {
            BOOT_ANIMATION_FILES[style]
        } else null
    }

    private fun getFrameDuration(zipFile: ZipFile): Int {
        try {
            val descEntry = zipFile.getEntry("desc.txt")
            if (descEntry != null) {
                val inputStream = zipFile.getInputStream(descEntry)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val line = reader.readLine()
                if (line != null) {
                    val parts = line.split(" ")
                    if (parts.size >= 3) {
                        val fps = parts[2].toInt()
                        return 1000 / fps
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading desc.txt", e)
        }
        return DEFAULT_FRAME_DURATION
    }

    private fun getPartCount(zipFile: ZipFile): Int {
        var partCount = 0
        try {
            val descEntry = zipFile.getEntry("desc.txt")
            if (descEntry != null) {
                val inputStream = zipFile.getInputStream(descEntry)
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("part")) {
                        partCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading desc.txt", e)
        }
        return partCount - 1
    }

    private fun loadTrimData(zipFile: ZipFile, partName: String): List<Rect> {
        val trimRects = ArrayList<Rect>()
        try {
            val trimEntry = zipFile.getEntry("$partName/trim.txt")
            if (trimEntry != null) {
                val inputStream = zipFile.getInputStream(trimEntry)
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("[x+]".toRegex())
                    if (parts.size == 4) {
                        val width = parts[0].toInt()
                        val height = parts[1].toInt()
                        val x = parts[2].toInt()
                        val y = parts[3].toInt()
                        trimRects.add(Rect(x, y, x + width, y + height))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trim data", e)
        }
        return trimRects
    }

    private fun loadFramesFromPart(
        context: Context, zipFile: ZipFile,
        animationDrawable: AnimationDrawable, partName: String, frameDuration: Int, trimData: List<Rect>
    ) {
        try {
            val pngPattern = Pattern.compile("$partName/.*\\.png$")
            val jpgPattern = Pattern.compile("$partName/.*\\.jpg$")
            val entries: Enumeration<out ZipEntry> = zipFile.entries()
            var frameIndex = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryName = entry.name
                if (pngPattern.matcher(entryName).matches() || jpgPattern.matcher(entryName).matches()) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        var bitmap = BitmapFactory.decodeStream(inputStream)
                        if (frameIndex < trimData.size) {
                            val trimRect = trimData[frameIndex]
                            val adjustedWidth = minOf(trimRect.width(), bitmap.width - trimRect.left)
                            val adjustedHeight = minOf(trimRect.height(), bitmap.height - trimRect.top)
                            if (adjustedWidth > 0 && adjustedHeight > 0) {
                                bitmap = Bitmap.createBitmap(bitmap, trimRect.left, trimRect.top, adjustedWidth, adjustedHeight)
                            } else {
                                //Log.w(TAG, "Trim rectangle exceeds bitmap dimensions, skipping trim for frame $frameIndex")
                            }
                        }
                        val frame: Drawable = BitmapDrawable(context.resources, bitmap)
                        animationDrawable.addFrame(frame, frameDuration)
                        frameIndex++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading frames from $partName", e)
        }
    }
}