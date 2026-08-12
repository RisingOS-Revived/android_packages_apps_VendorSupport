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

import android.content.Context
import android.content.res.TypedArray
import android.telephony.TelephonyManager
import androidx.preference.Preference
import android.util.AttributeSet
import com.android.settings.R

import com.android.settings.utils.AdaptivePreferenceUtils

class AdaptivePreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    init {
        if ("device_model" == key && !hasSimHardware(context)) {
            layoutResource = R.layout.top_level_preference_top_card
        }
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            layoutResource = layoutRes
        }
    }

    private fun hasSimHardware(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        return tm != null && tm.phoneCount > 0
    }
}
