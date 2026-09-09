/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

import android.os.UserHandle

import android.content.Context
import android.content.om.OverlayManager
import android.content.om.OverlayManagerTransaction
import android.content.om.OverlayIdentifier
import android.content.om.OverlayInfo
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log

import lineageos.preference.SelfRemovingSwitchPreference

import com.android.settings.utils.AdaptivePreferenceUtils

class OverlaySwitchPreference : SelfRemovingSwitchPreference {

    companion object {
        private const val TAG = "OverlaySwitchPreference"
        private const val SETTINGSNS = "http://schemas.android.com/apk/res-auto"
        private const val DKEY = "dkey"
        private const val DKEY_NIGHT_ONLY = "dkeyNightOnly"
    }

    private val mDisableKey: String?
    private val mDKeyNightOnly: Boolean
    private val mUserId: Int
    private val mUserHandle: UserHandle
    private val mOverlayManager: OverlayManager?

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        mDisableKey = attrs?.getAttributeValue(SETTINGSNS, DKEY)
        mDKeyNightOnly = attrs?.getAttributeBooleanValue(SETTINGSNS, DKEY_NIGHT_ONLY, false) ?: false
        mUserId = UserHandle.myUserId()
        mUserHandle = UserHandle.of(mUserId)
        mOverlayManager = context.getSystemService(OverlayManager::class.java)
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mDisableKey = attrs?.getAttributeValue(SETTINGSNS, DKEY)
        mDKeyNightOnly = attrs?.getAttributeBooleanValue(SETTINGSNS, DKEY_NIGHT_ONLY, false) ?: false
        mUserId = UserHandle.myUserId()
        mUserHandle = UserHandle.of(mUserId)
        mOverlayManager = context.getSystemService(OverlayManager::class.java)
        init(context, attrs)
    }

    constructor(context: Context) : this(context, null)

    private fun init(context: Context, attrs: AttributeSet?) {
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }

    override fun isPersisted(): Boolean {
        return true
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (mOverlayManager == null) return false
        val overlayId = getOverlayID(this.key) ?: return false
        val info = mOverlayManager.getOverlayInfo(overlayId, mUserHandle)
        return info?.isEnabled ?: false
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (mOverlayManager == null) return
        val overlayId = getOverlayID(this.key) ?: return
        val transaction = OverlayManagerTransaction.Builder()
        transaction.setEnabled(overlayId, value, mUserId)
        if (mDisableKey != null && mDisableKey.isNotEmpty()) {
            val disableOverlayId = getOverlayID(mDisableKey) ?: return
            if (mDKeyNightOnly) {
                val isNight = (context.resources.configuration.uiMode
                        and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    transaction.setEnabled(disableOverlayId, !value, mUserId)
                } else {
                    // always enabled in day
                    transaction.setEnabled(disableOverlayId, true, mUserId)
                }
            } else {
                transaction.setEnabled(disableOverlayId, !value, mUserId)
            }
        }
        try {
            mOverlayManager.commit(transaction.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed setting overlay(s), future logs will point the reason")
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed setting overlay(s), future logs will point the reason")
            e.printStackTrace()
        }
    }

    private fun getOverlayID(name: String?): OverlayIdentifier? {
        if (mOverlayManager == null || name == null) return null
        try {
            if (name.contains(":")) {
                // specific overlay name in a package
                val value = name.split(":")
                val pkgName = value[0]
                val overlayName = value[1]
                val infos = mOverlayManager.getOverlayInfosForTarget(pkgName, mUserHandle)
                for (info in infos) {
                    if (overlayName == info.overlayName) {
                        return info.overlayIdentifier
                    }
                }
                Log.e(TAG, "No overlay found for $name")
                return null
            }
            // package with only one overlay
            val info = mOverlayManager.getOverlayInfo(name, mUserHandle)
            return if (info != null) {
                info.overlayIdentifier
            } else {
                Log.e(TAG, "No overlay info found for $name")
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error retrieving overlay ID for $name", e)
            return null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error retrieving overlay ID for $name", e)
            return null
        }
    }
}
