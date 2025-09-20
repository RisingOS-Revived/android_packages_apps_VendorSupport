/*
 * Copyright (c) 2025 Rising Revived Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.preferences.rainbow

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder

class RainbowPreferenceCategory(context: Context, attrs: AttributeSet?) : PreferenceCategory(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // Find the title text view
        val titleView = holder.findViewById(android.R.id.title) as? TextView

        // Replace with RainbowTextView safely
        replaceWithRainbowTextView(titleView)
    }

    companion object {
        /**
         * Replaces the title TextView with a RainbowTextView to apply rainbow effect.
         */
        private fun replaceWithRainbowTextView(titleView: TextView?) {
            // Check if titleView or its parent is null to avoid crashes
            if (titleView == null || titleView.parent == null) {
                return // Exit safely to prevent crash
            }

            // Check if it's already a RainbowTextView to avoid redundant replacement
            if (titleView !is RainbowTextView) {
                val context = titleView.context
                val rainbowTextView = RainbowTextView(context)

                // Copy properties from the original TextView
                rainbowTextView.id = titleView.id
                rainbowTextView.text = titleView.text
                rainbowTextView.setTextAppearance(context, android.R.style.TextAppearance_Medium)
                rainbowTextView.ellipsize = titleView.ellipsize
                rainbowTextView.gravity = titleView.gravity
                rainbowTextView.isSingleLine = titleView.isSingleLine

                // Copy layout parameters
                val lp = titleView.layoutParams
                rainbowTextView.layoutParams = lp

                // Get the parent view safely
                val parent = titleView.parent as? ViewGroup
                if (parent != null) { // Extra check for safety
                    val index = parent.indexOfChild(titleView)
                    parent.removeView(titleView)
                    parent.addView(rainbowTextView, index)
                }
            }
        }
    }
}
