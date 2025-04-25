/*
 * Copyright (C) 2025 the RisingOS Revived Android Project
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
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceCategory

class HiddenPreferenceCategory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.preferenceCategoryStyle
) : PreferenceCategory(context, attrs, defStyleAttr) {

    override fun onAttached() {
        super.onAttached()
        // Hide the category and all its children
        isVisible = false
        hideAllChildren()
    }

    override fun addPreference(preference: Preference): Boolean {
        val added = super.addPreference(preference)
        if (added) {
            // Hide any new preferences that are added
            preference.isVisible = false
        }
        return added
    }

    private fun hideAllChildren() {
        for (i in 0 until preferenceCount) {
            getPreference(i).isVisible = false
        }
    }
}
