/*
 * Copyright (C) 2023-2024 the risingOS Project
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
import android.os.SystemProperties
import android.util.AttributeSet

import com.android.settingslib.development.SystemPropPoker

import lineageos.preference.SelfRemovingListPreference

import com.android.settings.utils.AdaptivePreferenceUtils

class SystemPropertyListPreference : SelfRemovingListPreference {

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
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }

    override fun isPersisted(): Boolean {
        return SystemProperties.get(key, "").isNotEmpty()
    }

    override fun putString(key: String, value: String?) {
        SystemProperties.set(key, value)
        SystemPropPoker.getInstance().poke()
    }

    override fun getString(key: String, defaultValue: String?): String {
        return SystemProperties.get(key, defaultValue)
    }
}
