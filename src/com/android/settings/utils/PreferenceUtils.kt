/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.utils

import androidx.preference.Preference
import androidx.preference.PreferenceGroup

import com.android.settings.preferences.GlobalSettingPrimarySwitchPreference
import com.android.settings.preferences.SecureSettingPrimarySwitchPreference
import com.android.settings.preferences.SystemSettingPrimarySwitchPreference

object PreferenceUtils {

    fun reloadCustomPrimarySwitches(group: PreferenceGroup) {
        reloadCustomPrimarySwitches(getAllPreferences(group))
    }

    fun reloadCustomPrimarySwitches(prefs: List<Preference>) {
        for (p in prefs) {
            when (p) {
                is SecureSettingPrimarySwitchPreference -> p.loadValue()
                is SystemSettingPrimarySwitchPreference -> p.loadValue()
                is GlobalSettingPrimarySwitchPreference -> p.loadValue()
            }
        }
    }

    fun getAllPreferences(group: PreferenceGroup): List<Preference> {
        val preferences = mutableListOf<Preference>()
        for (i in 0 until group.preferenceCount) {
            val pref = group.getPreference(i)
            preferences.add(pref)
            if (pref is PreferenceGroup) {
                preferences.addAll(getAllPreferences(pref))
            }
        }
        return preferences
    }
}
