/*
 * Copyright (C) 2023 The risingOS Android Project
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

package com.android.settings.preferences.ui

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen

import android.content.Context
import android.provider.Settings
import android.util.Log

import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.UsageProgressBarPreference
import com.android.settingslib.widget.LayoutPreference
import com.android.settingslib.widget.FooterPreference

import com.android.settings.spa.preference.ComposePreference
import com.android.settings.fuelgauge.batteryusage.PowerGaugePreference
import com.android.settings.widget.CardPreference
import com.android.settings.preferences.*

import java.util.ArrayList
import java.util.Arrays

import com.android.settings.R

import com.android.settings.utils.AdaptivePreferenceUtils

object PreferenceUtils {
    private const val TAG = "PreferenceUtils"

    @JvmStatic
    fun setupExtraPreferences(
        topPrefs: List<String>,
        middlePrefs: List<String>,
        bottomPrefs: List<String>,
        soloPrefs: List<String>,
        screen: PreferenceGroup,
        context: Context
    ) {
        setupExtraPreferences(topPrefs, middlePrefs, bottomPrefs, soloPrefs, screen, context, false)
    }

    @JvmStatic
    fun setupExtraPreferences(
        topPrefs: List<String>,
        middlePrefs: List<String>,
        bottomPrefs: List<String>,
        soloPrefs: List<String>,
        screen: PreferenceGroup?,
        context: Context,
        forceThemeMiddle: Boolean
    ) {
        if (screen == null || 
            (topPrefs.isEmpty() && middlePrefs.isEmpty() 
            && bottomPrefs.isEmpty() && soloPrefs.isEmpty())) {
            return
        }
        val allPreferences = getAllPreferences(screen)
        for (preference in allPreferences) {
            val key = preference.key
            if (key != null) {
                val layoutResource = getLayoutResourceForKey(context, key, topPrefs, middlePrefs, bottomPrefs, soloPrefs, forceThemeMiddle)
                if (layoutResource != 0 && !getExcludedPrefClass().contains(preference.javaClass)) {
                    preference.layoutResource = layoutResource
                    val bottomLayout = AdaptivePreferenceUtils.getLayoutResourceId(context, "bottom", false)
                    if (forceThemeMiddle && 
                        layoutResource == bottomLayout
                        && bottomPrefs.size == 1) {
                        preference.order = 1001
                    }
                }
            }
        }
    }
    
    private fun getExcludedPrefClass(): List<Class<*>> {
        return listOf(
            CardPreference::class.java,
            FooterPreference::class.java,
            IllustrationPreference::class.java,
            LayoutPreference::class.java,
            PowerGaugePreference::class.java,
            PreferenceCategory::class.java,
            UsageProgressBarPreference::class.java,
            ComposePreference::class.java,
            CustomSeekBarPreference::class.java,
            SystemSettingSeekBarPreference::class.java,
            SecureSettingSeekBarPreference::class.java
        )
    }

    private fun getLayoutResourceForKey(
        context: Context, 
        key: String, 
        topPrefs: List<String>, 
        middlePrefs: List<String>,
        bottomPrefs: List<String>, 
        soloPrefs: List<String>,
        forceThemeMiddle: Boolean
    ): Int {
        return when {
            topPrefs.isNotEmpty() && topPrefs.contains(key) -> {
                AdaptivePreferenceUtils.getLayoutResourceId(context, "top", false)
            }
            middlePrefs.isNotEmpty() && middlePrefs.contains(key) -> {
                AdaptivePreferenceUtils.getLayoutResourceId(context, "middle", false)
            }
            bottomPrefs.isNotEmpty() && bottomPrefs.contains(key) -> {
                AdaptivePreferenceUtils.getLayoutResourceId(context, "bottom", false)
            }
            soloPrefs.isNotEmpty() && soloPrefs.contains(key) -> {
                AdaptivePreferenceUtils.getLayoutResourceId(context, "solo", false)
            }
            forceThemeMiddle && middlePrefs.isEmpty() -> {
                AdaptivePreferenceUtils.getLayoutResourceId(context, "middle", false)
            }
            else -> 0
        }
    }

    @JvmStatic
    fun setLayoutResources(context: Context, preferences: List<Preference>) {
        var minOrder = Int.MAX_VALUE
        var maxOrder = Int.MIN_VALUE

        for (preference in preferences) {
            if (preference.isVisible) {
                val order = preference.order
                if (order < minOrder) {
                    minOrder = order
                }
                if (order > maxOrder) {
                    maxOrder = order
                }
            }
        }

        for (preference in preferences) {
            if (preference.isVisible && !getExcludedPrefClass().contains(preference.javaClass)) {
                val order = preference.order
                when (order) {
                    minOrder -> {
                        preference.layoutResource = AdaptivePreferenceUtils.getLayoutResourceId(context, "top", false)
                    }
                    maxOrder -> {
                        preference.layoutResource = AdaptivePreferenceUtils.getLayoutResourceId(context, "bottom", false)
                    }
                    else -> {
                        preference.layoutResource = AdaptivePreferenceUtils.getLayoutResourceId(context, "middle", false)
                    }
                }
            }
        }
    }

    private fun logPreferenceKey(preference: Preference) {
        val key = preference.key
        if (key != null) {
            Log.d(TAG, "Preference Key: $key")
        }
    }

    @JvmStatic
    fun getAllPreferences(preferenceGroup: PreferenceGroup): List<Preference> {
        val preferences = ArrayList<Preference>()
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference.isVisible) {
                preferences.add(preference)
                if (preference is PreferenceGroup) {
                    preferences.addAll(getAllPreferences(preference))
                }
                logPreferenceKey(preference)
            }
        }
        return preferences
    }
    
    @JvmStatic
    fun hideEmptyCategory(category: PreferenceCategory?, screen: PreferenceScreen) {
        if (category != null && category.preferenceCount == 0) {
            screen.removePreference(category)
        }
    }
}
