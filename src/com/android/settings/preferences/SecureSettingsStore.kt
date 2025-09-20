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

import android.content.ContentResolver
import androidx.preference.PreferenceDataStore
import android.os.UserHandle
import android.provider.Settings

class SecureSettingsStore(private val mContentResolver: ContentResolver) : 
    androidx.preference.PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return Settings.Secure.getIntForUser(mContentResolver, key, if (defValue) 1 else 0, UserHandle.USER_CURRENT) != 0
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return Settings.Secure.getFloatForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return Settings.Secure.getIntForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return Settings.Secure.getLongForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT)
    }

    override fun getString(key: String, defValue: String?): String? {
        val result = Settings.Secure.getString(mContentResolver, key)
        return result ?: defValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        putInt(key, if (value) 1 else 0)
    }

    override fun putFloat(key: String, value: Float) {
        Settings.Secure.putFloatForUser(mContentResolver, key, value, UserHandle.USER_CURRENT)
    }

    override fun putInt(key: String, value: Int) {
        Settings.Secure.putIntForUser(mContentResolver, key, value, UserHandle.USER_CURRENT)
    }

    override fun putLong(key: String, value: Long) {
        Settings.Secure.putLongForUser(mContentResolver, key, value, UserHandle.USER_CURRENT)
    }

    override fun putString(key: String, value: String?) {
        Settings.Secure.putString(mContentResolver, key, value)
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        // Secure settings doesn't support string sets, return default
        return defValues
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        // Secure settings doesn't support string sets, do nothing
    }
}
