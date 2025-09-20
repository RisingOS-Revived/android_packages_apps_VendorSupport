/*
 * Copyright (C) 2024 risingOS Android Project
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
import androidx.preference.ListPreference
import androidx.preference.PreferenceDataStore
import android.text.TextUtils
import android.util.AttributeSet
import android.provider.Settings

import com.android.settings.utils.AdaptivePreferenceUtils

import lineageos.providers.LineageSettings

import lineageos.preference.LineageSystemSettingListPreference

class RisingSystemSettingListPreference : LineageSystemSettingListPreference {

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }
}
