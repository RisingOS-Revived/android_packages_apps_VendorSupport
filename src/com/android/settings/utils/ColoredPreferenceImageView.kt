/*
 * Copyright (C) 2023-2024 The risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View

import com.android.internal.widget.PreferenceImageView

import kotlin.random.Random

class ColoredPreferenceImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : PreferenceImageView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val COLOR_MAP = arrayOf(
            "#007aff", "#2fb151", "#fb7c47", "#fa7d4d", "#fbb404", "#e13e39"
        )
    }

    init {
        setRandomBgTint()
    }

    private fun setRandomBgTint() {
        val random = Random
        val randomColor = COLOR_MAP[random.nextInt(COLOR_MAP.size)]
        val colorInt = Color.parseColor(randomColor)
        backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var finalWidthMeasureSpec = widthMeasureSpec
        var finalHeightMeasureSpec = heightMeasureSpec
        
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        if (widthMode == View.MeasureSpec.AT_MOST || widthMode == View.MeasureSpec.UNSPECIFIED) {
            val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
            val maxWidth = maxWidth
            if (maxWidth != Integer.MAX_VALUE && (maxWidth < widthSize || widthMode == View.MeasureSpec.UNSPECIFIED)) {
                finalWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST)
            }
        }
        
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode == View.MeasureSpec.AT_MOST || heightMode == View.MeasureSpec.UNSPECIFIED) {
            val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
            val maxHeight = maxHeight
            if (maxHeight != Integer.MAX_VALUE && (maxHeight < heightSize || heightMode == View.MeasureSpec.UNSPECIFIED)) {
                finalHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            }
        }
        
        super.onMeasure(finalWidthMeasureSpec, finalHeightMeasureSpec)
    }
}