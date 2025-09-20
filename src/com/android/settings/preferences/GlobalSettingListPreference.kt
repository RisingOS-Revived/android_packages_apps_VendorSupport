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
import androidx.preference.ListPreference
import androidx.preference.PreferenceDataStore
import android.text.TextUtils
import android.util.AttributeSet

import com.android.settings.utils.AdaptivePreferenceUtils

class GlobalSettingListPreference : ListPreference {

    private lateinit var dataStore: PreferenceDataStore
    private var mAutoSummary = false

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
        dataStore = GlobalSettingsStore(context.contentResolver)
        preferenceDataStore = dataStore
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }

    override fun setValue(value: String?) {
        super.setValue(value)
        if (mAutoSummary || TextUtils.isEmpty(summary)) {
            setSummary(entry, true)
        }
    }

    override fun setSummary(summary: CharSequence?) {
        setSummary(summary, false)
    }

    private fun setSummary(summary: CharSequence?, autoSummary: Boolean) {
        mAutoSummary = autoSummary
        super.setSummary(summary)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        setValue(if (restoreValue) getPersistedString(defaultValue as? String) else defaultValue as? String)
    }

    fun getIntValue(defValue: Int): Int {
        return if (value == null) defValue else Integer.valueOf(value)
    }
}
