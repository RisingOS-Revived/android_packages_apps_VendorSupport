/*
 * Copyright (C) 2023-2024 the risingOS Android Project
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
package com.android.settings.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.android.settings.R

object SystemRestartUtils {

    private const val TAG = "SystemRestartUtils"

    fun showSystemUIRestartDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(com.android.internal.R.string.systemui_restart_title)
            .setMessage(com.android.internal.R.string.systemui_restart_message)
            .setPositiveButton(R.string.ok) { dialog, which -> restartSystemUI(context) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun restartSystemUI(context: Context) {
        val resolver = context.contentResolver
        val currentValue = Settings.System.getInt(resolver, "system_ui_restart", 0)
        val newValue = if (currentValue == 0) 1 else 0
        Settings.System.putInt(resolver, "system_ui_restart", newValue)
    }

    fun reloadSystemUI(context: Context) {
        val resolver = context.contentResolver
        val currentValue = Settings.System.getInt(resolver, "system_ui_reload", 0)
        val newValue = if (currentValue == 0) 1 else 0
        Settings.System.putInt(resolver, "system_ui_reload", newValue)
    }
}