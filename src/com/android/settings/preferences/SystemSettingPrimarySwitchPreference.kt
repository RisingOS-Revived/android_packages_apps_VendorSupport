/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.preferences

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet

import androidx.preference.PreferenceDataStore

import com.android.settings.utils.AdaptivePreferenceUtils
import com.android.settingslib.PrimarySwitchPreference

class SystemSettingPrimarySwitchPreference : PrimarySwitchPreference {

    private var mDefaultValue = false

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
        preferenceDataStore = DataStore()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        if (defaultValue is Boolean) {
            mDefaultValue = defaultValue
        }
    }

    override fun onAttached() {
        super.onAttached()
        isChecked = Settings.System.getIntForUser(context.contentResolver,
                key, if (mDefaultValue) 1 else 0, UserHandle.USER_CURRENT) != 0
    }

    fun loadValue() {
        isChecked = Settings.System.getIntForUser(context.contentResolver,
                key, if (mDefaultValue) 1 else 0, UserHandle.USER_CURRENT) != 0
    }

    private inner class DataStore : PreferenceDataStore() {
        override fun putBoolean(key: String, value: Boolean) {
            Settings.System.putIntForUser(context.contentResolver,
                    key, if (value) 1 else 0, UserHandle.USER_CURRENT)
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return Settings.System.getIntForUser(context.contentResolver,
                    key, if (defaultValue) 1 else 0, UserHandle.USER_CURRENT) != 0
        }
    }
}
