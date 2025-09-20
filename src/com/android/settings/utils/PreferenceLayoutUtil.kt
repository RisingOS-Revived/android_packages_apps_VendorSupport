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
import androidx.preference.Preference
import android.provider.Settings
import com.android.internal.util.android.Utils

import com.android.settings.R

object PreferenceLayoutUtil {

    private var extraPreferenceOrder = -153

    private const val PACKAGE_WELLBEING = "com.google.android.apps.wellbeing"
    private const val PACKAGE_GOOGLE_SERVICES = "com.google.android.gms"

    private val topPreferences = setOf(
        "top_level_network",
        "top_level_apps",
        "top_level_accessibility",
        "top_level_emergency",
        "top_level_display"
    )

    private val middlePreferences = setOf(
        "top_level_battery",
        "top_level_security",
        "top_level_privacy",
        "top_level_storage",
        "top_level_notifications",
        "top_level_communal",
        "top_level_safety_center",
        "top_level_accounts"
    )

    private val bottomPreferences = setOf(
        "top_level_connected_devices",
        "top_level_sound",
        "top_level_wallpaper",
        "top_level_location"
    )
    
    private val EXCLUDE_LIST = setOf(
        "top_level_crdroid"
    )

    fun updateStartOrder(startingOrder: Int) {
        extraPreferenceOrder = startingOrder
    }

    fun setUpPreferenceLayout(preference: Preference, context: Context) {
        val showHomePageShowcase = Settings.System.getInt(
            context.contentResolver, "settings_homepage_showcase", 0) != 0
        val showAvatarCard = Settings.System.getInt(
            context.contentResolver, "show_avatar_card_on_homepage", 0) != 0
        val key = preference.key
        if (EXCLUDE_LIST.contains(key)) {
            return
        }
        val isWellbeingInstalled = Utils.isPackageInstalled(context, PACKAGE_WELLBEING)
        val isGoogleServiceInstalled = Utils.isPackageInstalled(context, PACKAGE_GOOGLE_SERVICES)
        when (key) {
            "top_level_wellbeing" -> {
                if (isWellbeingInstalled) {
                    setPreferenceLayout(preference, context, "wellbeing", true)
                }
            }
            "top_level_google" -> {
                if (isGoogleServiceInstalled) {
                    setPreferenceLayout(preference, context, "google", true)
                }
            }
            "top_level_system" -> {
                setPreferenceLayout(preference, context, if (showHomePageShowcase) "bottom" else "middle", true)
            }
            "top_level_about_device" -> {
                preference.layoutResource = if (showHomePageShowcase)
                    R.layout.top_level_preference_about
                else
                    AdaptivePreferenceUtils.getLayoutResourceId(context, "bottom", true)
                preference.order = if (showHomePageShowcase) -151 else 11
            }
            "top_level_usercard" -> {
                if (showAvatarCard) {
                    preference.isVisible = true
                    preference.layoutResource = R.layout.top_level_usercard
                    preference.order = -152
                } else {
                    preference.isVisible = false
                }
            }
            else -> {
                // Handle other preferences (e.g., OEM parts)
                when {
                    topPreferences.contains(key) -> {
                        setPreferenceLayout(preference, context, "top", true)
                        if (key == "top_level_display") {
                            preference.order = -150
                        }
                    }
                    middlePreferences.contains(key) -> {
                        setPreferenceLayout(preference, context, "middle", true)
                    }
                    bottomPreferences.contains(key) -> {
                        setPreferenceLayout(preference, context, "bottom", true)
                        if (key == "top_level_wallpaper") {
                            preference.order = -140
                        }
                    }
                    else -> {
                        val order = extraPreferenceOrder - 1
                        updateStartOrder(order)
                        preference.order = order
                        setPreferenceLayout(preference, context, "solo", true)
                    }
                }
            }
        }
    }

    private fun setPreferenceLayout(preference: Preference, context: Context, layoutType: String, useAdaptive: Boolean) {
        preference.layoutResource = AdaptivePreferenceUtils.getLayoutResourceId(context, layoutType, useAdaptive)
    }
}