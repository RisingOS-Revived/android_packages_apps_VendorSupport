/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget

import android.content.Context
import android.util.AttributeSet

import androidx.preference.SwitchPreferenceCompat

/**
 * The SwitchPreference for the pages need to show apps icon.
*/
class AppSwitchPreference : SwitchPreferenceCompat {

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app)
    }

    constructor(context: Context) : super(context) {
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app)
    }
}
