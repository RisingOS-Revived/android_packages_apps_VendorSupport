/*
 * Copyright (C) 2024-2025 Lunaris AOSP
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
package com.android.settings.preferences

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView

import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.settings.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WallpaperPreviewPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var mLockPreview: ImageView? = null
    private var mHomePreview: ImageView? = null
    private var mLockLabel: TextView? = null
    private var mHomeLabel: TextView? = null
    private var mApplyButton: MaterialButton? = null
    private var mLockCard: MaterialCardView? = null
    private var mHomeCard: MaterialCardView? = null

    private var mExecutor: ExecutorService? = Executors.newSingleThreadExecutor()
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mWallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)

    private var mLockWallpaper: Bitmap? = null
    private var mHomeWallpaper: Bitmap? = null

    init {
        layoutResource = R.layout.preference_wallpaper_preview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        mLockCard = holder.findViewById(R.id.lock_wallpaper_card) as? MaterialCardView
        mHomeCard = holder.findViewById(R.id.home_wallpaper_card) as? MaterialCardView
        mLockPreview = holder.findViewById(R.id.lock_wallpaper_preview) as? ImageView
        mHomePreview = holder.findViewById(R.id.home_wallpaper_preview) as? ImageView
        mLockLabel = holder.findViewById(R.id.lock_wallpaper_label) as? TextView
        mHomeLabel = holder.findViewById(R.id.home_wallpaper_label) as? TextView
        mApplyButton = holder.findViewById(R.id.apply_now_button) as? MaterialButton

        mApplyButton?.setOnClickListener { applyNewWallpaper() }

        loadWallpaperPreviews()
    }

    private fun loadWallpaperPreviews() {
        if (mExecutor == null || mExecutor?.isShutdown == true) {
            mExecutor = Executors.newSingleThreadExecutor()
        }

        mExecutor?.execute {
            try {
                val lockDrawable = mWallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                if (lockDrawable is BitmapDrawable) {
                    mLockWallpaper = lockDrawable.bitmap
                } else {
                    val systemDrawable = mWallpaperManager.drawable
                    if (systemDrawable is BitmapDrawable) {
                        mLockWallpaper = systemDrawable.bitmap
                    }
                }

                val homeDrawable = mWallpaperManager.drawable
                if (homeDrawable is BitmapDrawable) {
                    mHomeWallpaper = homeDrawable.bitmap
                }

                mHandler.post { updatePreviewImages() }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePreviewImages() {
        mLockPreview?.let { preview ->
            mLockWallpaper?.let {
                preview.setImageBitmap(it)
                preview.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

        mHomePreview?.let { preview ->
            mHomeWallpaper?.let {
                preview.setImageBitmap(it)
                preview.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    private fun applyNewWallpaper() {
        val context = context ?: return

        mApplyButton?.let {
            it.isEnabled = false
            it.setText(R.string.lock_glymps_applying)
        }

        val intent = Intent().apply {
            setClassName(
                "com.android.systemui",
                "com.android.systemui.lockglymps.LockGlympsService"
            )
            action = "APPLY_NOW"
        }
        context.startService(intent)

        mHandler.postDelayed({
            mApplyButton?.let {
                it.isEnabled = true
                it.setText(R.string.lock_glymps_apply_now)
            }
            mHandler.postDelayed({ loadWallpaperPreviews() }, 1000)
        }, 2000)
    }

    fun refreshPreviews() {
        loadWallpaperPreviews()
    }

    override fun onDetached() {
        super.onDetached()
        mExecutor?.let {
            if (!it.isShutdown) {
                it.shutdown()
            }
        }
        mExecutor = null

        mLockWallpaper?.let {
            if (!it.isRecycled) {
                mLockWallpaper = null
            }
        }
        mHomeWallpaper?.let {
            if (!it.isRecycled) {
                mHomeWallpaper = null
            }
        }
    }
}
