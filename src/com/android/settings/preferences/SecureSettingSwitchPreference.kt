/*
 * Copyright (C) 2016-2018 crDroid Android Project
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
import android.provider.Settings
import android.os.UserHandle
import android.util.AttributeSet

import lineageos.preference.SelfRemovingSwitchPreference

import com.android.settings.utils.AdaptivePreferenceUtils

import lineageos.providers.LineageSettings

class SecureSettingSwitchPreference : SelfRemovingSwitchPreference {

    private var isLineageSettings: Boolean = false

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }
    
    private fun init(context: Context, attrs: AttributeSet?) {
        isLineageSettings = AdaptivePreferenceUtils.isLineageSettings(context, attrs)
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }

    override fun isPersisted(): Boolean {
        return if (isLineageSettings) {
            LineageSettings.Secure.getString(context.contentResolver, key) != null
        } else {
            Settings.Secure.getString(context.contentResolver, key) != null
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (isLineageSettings) {
            LineageSettings.Secure.putInt(context.contentResolver, key, if (value) 1 else 0)
        } else {
            Settings.Secure.putIntForUser(context.contentResolver, key, if (value) 1 else 0, UserHandle.USER_CURRENT)
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (isLineageSettings) {
            LineageSettings.Secure.getInt(context.contentResolver, key, if (defaultValue) 1 else 0) != 0
        } else {
            Settings.Secure.getIntForUser(context.contentResolver, key, if (defaultValue) 1 else 0, UserHandle.USER_CURRENT) != 0
        }
    }
}
