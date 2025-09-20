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
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.preference.internal.PreferenceImageView
import com.android.settings.R
import java.util.*

class DynamicIconPreferenceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PreferenceImageView(context, attrs, defStyle) {

    private val maxWidth = dpToPx(context, 52)
    private val maxHeight = dpToPx(context, 52)

    private val colorMap = arrayOf(
        "#007aff", "#2fb151", "#fb7c47",
        "#fa7d4d", "#fbb404", "#e13e39"
    )

    private lateinit var styleMap: Map<String, StyleAttributes>

    init {
        initializeStyleMap(context)
    }

    private fun initializeStyleMap(context: Context) {
        val resources = context.resources
        
        styleMap = hashMapOf(
            "0" to StyleAttributes(
                dpToPx(context, 6),
                dpToPx(context, 40), dpToPx(context, 40), null, R.color.top_level_preference_text_color_primary,
                null, ImageView.ScaleType.CENTER_INSIDE
            ),
            "1" to StyleAttributes(
                resources.getDimensionPixelSize(R.dimen.top_level_icon_padding),
                dpToPx(context, 48), dpToPx(context, 48),
                R.drawable.custom_surface_color, R.color.top_level_preference_icon_tint, null, null
            ),
            "2" to StyleAttributes(
                resources.getDimensionPixelSize(R.dimen.top_level_icon_padding),
                dpToPx(context, 48), dpToPx(context, 48),
                R.drawable.custom_surface_color_rounded, R.color.top_level_preference_icon_tint, null, null
            ),
            "3" to StyleAttributes(
                resources.getDimensionPixelSize(R.dimen.top_level_icon_padding),
                dpToPx(context, 48), dpToPx(context, 48),
                R.drawable.custom_surface_color_oos, R.color.top_level_preference_text_color_primary, null, null
            ),
            "4" to StyleAttributes(
                resources.getDimensionPixelSize(R.dimen.top_level_icon_padding),
                dpToPx(context, 48), dpToPx(context, 48),
                R.drawable.custom_surface_color_rounded, Color.WHITE, getRandomColor(), null
            )
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateIconStyle()
    }

    private fun updateIconStyle() {
        val styleValue = getSettingsIconStyle().toString()
        val attributes = styleMap[styleValue]
        attributes?.let { applyStyle(it) }
    }

    private fun getSettingsIconStyle(): Int {
        return Settings.System.getIntForUser(
            context.contentResolver, "settings_icon_style", 0, android.os.UserHandle.USER_CURRENT
        )
    }

    private fun applyStyle(attributes: StyleAttributes) {
        setPadding(attributes.padding, attributes.padding, attributes.padding, attributes.padding)
        val layoutParams = getLayoutParams()
        layoutParams.width = attributes.width
        layoutParams.height = attributes.height
        setLayoutParams(layoutParams)
        
        attributes.background?.let { backgroundRes ->
            val background = ContextCompat.getDrawable(context, backgroundRes)
            setBackground(background)
        }

        if (attributes.tint is Int) {
            val color = attributes.tint
            try {
                imageTintList = ContextCompat.getColorStateList(context, color)
            } catch (e: Resources.NotFoundException) {
                imageTintList = ColorStateList.valueOf(color)
            }
        }
        attributes.bgTint?.let { bgColor ->
            if (bgColor is Int) {
                backgroundTintList = ColorStateList.valueOf(bgColor)
            }
        }
    }

    private fun getRandomColor(): Int {
        val random = Random()
        val colorString = colorMap[random.nextInt(colorMap.size)]
        return Color.parseColor(colorString)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private data class StyleAttributes(
        val padding: Int,
        val width: Int,
        val height: Int,
        val background: Int?,
        val tint: Any?,
        val bgTint: Any?,
        val scaleType: ImageView.ScaleType?
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        var widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        
        if (widthMode == View.MeasureSpec.AT_MOST || widthMode == View.MeasureSpec.EXACTLY) {
            widthSize = minOf(widthSize, maxWidth)
        }
        if (heightMode == View.MeasureSpec.AT_MOST || heightMode == View.MeasureSpec.EXACTLY) {
            heightSize = minOf(heightSize, maxHeight)
        }
        setMeasuredDimension(widthSize, heightSize)
    }
}