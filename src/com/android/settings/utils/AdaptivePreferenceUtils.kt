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
import android.content.res.TypedArray
import android.os.Handler
import android.provider.Settings
import android.util.AttributeSet
import com.android.settings.R
import android.widget.Toast

import com.android.internal.util.android.ThemeUtils

object AdaptivePreferenceUtils {

    private const val overlayThemeTarget = "com.android.systemui"

    @JvmStatic
    fun refreshTheme(context: Context) {
        val themeUtils = ThemeUtils.getInstance(context)
        Toast.makeText(context, context.getString(R.string.reevaluating_theme), Toast.LENGTH_SHORT).show()
        Handler().postDelayed({
            themeUtils.setOverlayEnabled("android.theme.customization.sysui_reevaluate", overlayThemeTarget, overlayThemeTarget)
            themeUtils.setOverlayEnabled("android.theme.customization.sysui_reevaluate", "com.android.system.qs.sysui_reevaluate", overlayThemeTarget)
        }, Toast.LENGTH_SHORT + 500L)
    }

    @JvmStatic
    fun getPosition(context: Context, attrs: AttributeSet?): String? {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AdaptivePreference)
        val positionAttribute = typedArray.getString(R.styleable.AdaptivePreference_position)
        typedArray.recycle()
        return positionAttribute
    }
    
    @JvmStatic
    fun isLineageSettings(context: Context, attrs: AttributeSet?): Boolean {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AdaptivePreference)
        val isLineage = typedArray.getBoolean(R.styleable.AdaptivePreference_isLineageSettings, false)
        typedArray.recycle()
        return isLineage
    }

    @JvmStatic
    fun getLayoutResourceId(context: Context, attrs: AttributeSet?): Int {
        val positionString = getPosition(context, attrs)
        return getLayoutResourceId(context, positionString, false)
    }
    
    @JvmStatic
    fun getSettingsTheme(context: Context): Int {
        /* return Settings.System.getInt(context.contentResolver, "settings_theme_style", 0) */
        return 1
    }
    
    @JvmStatic
    fun getLayoutResourceId(context: Context, positionString: String?, isHomePage: Boolean): Int {
        val settingsTheme = getSettingsTheme(context)
        return getLayoutResourceId(context, settingsTheme, positionString, isHomePage)
    }
    
    private fun getLayoutIdentifier(settingsTheme: Int): String {
        val layoutId = arrayOf("card", "ayan", "card_material", "oos", "card_colorful")
        return layoutId[settingsTheme]
    }
    
    private fun getSoloLayoutIdentifier(settingsTheme: Int): String {
        val layoutId = arrayOf("card", "card_ayan", "card_material", "oos", "card_colorful")
        return layoutId[settingsTheme]
    }
    
    private fun getCustomLayoutIdentifier(settingsTheme: Int): String {
        return if (settingsTheme > 1) "_mt" else ""
    }
    
    @JvmStatic
    fun getLayoutResourceId(context: Context, settingsTheme: Int, positionString: String?, isHomePage: Boolean): Int {
        val position = Position.fromAttribute(positionString)
        val layout = getLayoutIdentifier(settingsTheme)
        
        when {
            positionString != null && positionString == "wellbeing" -> {
                return context.resources.getIdentifier("top_level_preference_wellbeing_$layout", "layout", "com.android.settings")
            }
            positionString != null && positionString == "google" -> {
                return context.resources.getIdentifier("top_level_preference_google_$layout", "layout", "com.android.settings")
            }
        }
        
        if (position == null) {
            return context.resources.getIdentifier("top_level_preference_middle_$layout", "layout", "com.android.settings")
        }
        
        return when (position) {
            Position.TOP -> context.resources.getIdentifier("top_level_preference_top_$layout", "layout", "com.android.settings")
            Position.BOTTOM -> context.resources.getIdentifier("top_level_preference_bottom_$layout", "layout", "com.android.settings")
            Position.MIDDLE -> context.resources.getIdentifier("top_level_preference_middle_$layout", "layout", "com.android.settings")
            Position.SOLO -> context.resources.getIdentifier("top_level_preference_solo_${getSoloLayoutIdentifier(settingsTheme)}", "layout", "com.android.settings")
            Position.NONE -> -1
        }
    }

    @JvmStatic
    fun getSeekBarLayoutResourceId(context: Context, attrs: AttributeSet?): Int {
        val settingsTheme = getSettingsTheme(context)
        val positionString = getPosition(context, attrs)
        val position = Position.fromAttribute(positionString)
        val layout = getCustomLayoutIdentifier(settingsTheme)
        
        if (position == null) {
            return context.resources.getIdentifier("preference_custom_seekbar_middle$layout", "layout", "com.android.settings")
        }
        
        return when (position) {
            Position.TOP -> context.resources.getIdentifier("preference_custom_seekbar_top$layout", "layout", "com.android.settings")
            Position.BOTTOM -> context.resources.getIdentifier("preference_custom_seekbar_bottom$layout", "layout", "com.android.settings")
            Position.MIDDLE -> context.resources.getIdentifier("preference_custom_seekbar_middle$layout", "layout", "com.android.settings")
            Position.SOLO -> R.layout.preference_custom_seekbar_solo
            Position.NONE -> -1
        }
    }
    
    @JvmStatic
    fun getComposeLayoutResourceId(context: Context, attrs: AttributeSet?): Int {
        val settingsTheme = getSettingsTheme(context)
        val positionString = getPosition(context, attrs)
        val position = Position.fromAttribute(positionString)
        val layout = getCustomLayoutIdentifier(settingsTheme)
        
        if (position == null) {
            return context.resources.getIdentifier("preference_compose", "layout", "com.android.settings")
        }
        
        return when (position) {
            Position.TOP -> context.resources.getIdentifier("preference_compose_custom_top$layout", "layout", "com.android.settings")
            Position.BOTTOM -> context.resources.getIdentifier("preference_compose_custom_bottom$layout", "layout", "com.android.settings")
            Position.MIDDLE -> context.resources.getIdentifier("preference_compose_custom_middle$layout", "layout", "com.android.settings")
            Position.SOLO -> context.resources.getIdentifier("preference_compose_custom_solo$layout", "layout", "com.android.settings")
            Position.NONE -> -1
            else -> context.resources.getIdentifier("preference_compose", "layout", "com.android.settings")
        }
    }

    enum class Position {
        TOP,
        MIDDLE,
        BOTTOM,
        SOLO,
        NONE;

        companion object {
            @JvmStatic
            fun fromAttribute(attribute: String?): Position? {
                return if (attribute != null) {
                    when (attribute.lowercase()) {
                        "top" -> TOP
                        "bottom" -> BOTTOM
                        "middle" -> MIDDLE
                        "solo" -> SOLO
                        "none" -> NONE
                        else -> null
                    }
                } else null
            }
        }
    }
}
