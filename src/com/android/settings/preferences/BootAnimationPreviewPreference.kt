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
package com.android.settings.preferences

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar

import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.settings.R
import com.android.settings.utils.BootAnimationUtils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BootAnimationPreviewPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private var mImageView: ImageView? = null
    private var mLoadingSpinner: ProgressBar? = null
    private var mCurrentTask: Future<*>? = null
    private val mExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        layoutResource = R.layout.preference_bootanimation_preview
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mImageView = holder.findViewById(R.id.bootanimation_preview_image) as? ImageView
        mLoadingSpinner = holder.findViewById(R.id.bootanimation_loading_spinner) as? ProgressBar
        loadBootAnimationPreview()
    }

    fun loadBootAnimationPreview() {
        mCurrentTask?.cancel(true)
        
        val bootAnimStyle = BootAnimationUtils.getBootAnimStyle()
        if (bootAnimStyle == 2 || bootAnimStyle == 3) {
            mImageView?.let { imageView ->
                val drawable = context.getDrawable(
                    if (bootAnimStyle == 2) R.drawable.google_gemini else R.drawable.google_monet
                )
                imageView.setImageDrawable(drawable)
            }
        } else {
            mCurrentTask = mExecutor.submit(LoadPreviewTask())
        }
    }

    private inner class LoadPreviewTask : Runnable {
        @Volatile
        private var isCancelled = false
        
        fun cancel() {
            isCancelled = true
        }

        override fun run() {
            if (isCancelled) return
            
            // Show loading spinner on UI thread
            mImageView?.post {
                mImageView?.visibility = View.GONE
                mLoadingSpinner?.visibility = View.VISIBLE
            }
            
            val originalDrawable = BootAnimationUtils.getBootAnimationFrames(context)
            if (originalDrawable == null || isCancelled) {
                // Hide loading spinner on UI thread
                mImageView?.post {
                    mLoadingSpinner?.visibility = View.GONE
                    mImageView?.visibility = View.VISIBLE
                }
                return
            }
            
            val fixedDrawable = AnimationDrawable()
            for (i in 0 until originalDrawable.numberOfFrames) {
                if (isCancelled) return
                val frame = originalDrawable.getFrame(i)
                var duration = originalDrawable.getDuration(i)
                if (duration < 16) { // 16 ms is around 60fps
                    duration = 1000 / 60 // Set to 60fps as a fallback
                }
                fixedDrawable.addFrame(frame, duration)
            }
            fixedDrawable.isOneShot = false // Ensure the animation loops
            
            if (isCancelled) return
            
            // Update UI on main thread
            mImageView?.post {
                mLoadingSpinner?.visibility = View.GONE
                mImageView?.let { imageView ->
                    imageView.visibility = View.VISIBLE
                    imageView.setImageDrawable(fixedDrawable)
                    fixedDrawable.start()
                }
            }
        }
    }
}
