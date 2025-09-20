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
package com.android.settings.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log

import com.android.internal.telephony.PhoneConstants
import com.android.internal.telephony.RILConstants

/**
 * Helper class which has the same logic as MobileNetworkSettings to display the same
 * network modes and strings as it does.
 */
object TelephonyUtils {

    private val TAG = TelephonyUtils::class.java.simpleName

    // from MobileNetworkSettings
    const val ACTION_PICK_NETWORK_MODE = "lineageos.platform.intent.action.NETWORK_MODE_PICKER"
    const val EXTRA_NONE_TEXT = "network_mode_picker::neutral_text"
    const val EXTRA_SHOW_NONE = "network_mode_picker::show_none"
    const val EXTRA_INITIAL_NETWORK_VALUE = "network_mode_picker::selected_mode"
    const val EXTRA_NETWORK_PICKER_PICKED_VALUE = "network_mode_picker::chosen_value"
    const val EXTRA_SUBID = "network_mode_picker::sub_id"

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    fun isVoiceCapable(context: Context): Boolean {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        return telephony?.isVoiceCapable == true
    }

    fun getNetworkModeString(context: Context, networkMode: Int, subId: Int): String? {
        return getNetworkModeString(
            context,
            networkMode,
            TelephonyManager.from(context).getCurrentPhoneType(subId),
            show4GForLTE(context),
            isSupportTdscdma(context, subId),
            isGlobalCDMA(context, subId, isLteOnCdma(context, subId)),
            isWorldMode(context)
        )
    }

    fun getNetworkModeString(
        context: Context, networkMode: Int,
        phoneType: Int, show4GForLTE: Boolean, isSupportTdsCdma: Boolean, isGlobalCdma: Boolean,
        isWorldMode: Boolean
    ): String? {
        var r: String? = null
        when (networkMode) {
            RILConstants.NETWORK_MODE_TDSCDMA_WCDMA,
            RILConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA,
            RILConstants.NETWORK_MODE_TDSCDMA_GSM,
            RILConstants.NETWORK_MODE_WCDMA_ONLY,
            RILConstants.NETWORK_MODE_GSM_UMTS,
            RILConstants.NETWORK_MODE_WCDMA_PREF,
            RILConstants.NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA,
            RILConstants.NETWORK_MODE_CDMA,
            RILConstants.NETWORK_MODE_EVDO_NO_CDMA,
            RILConstants.NETWORK_MODE_GLOBAL -> r = "network_3G"
            RILConstants.NETWORK_MODE_GSM_ONLY -> r = "network_2G"
            RILConstants.NETWORK_MODE_LTE_GSM_WCDMA,
            RILConstants.NETWORK_MODE_LTE_WCDMA,
            RILConstants.NETWORK_MODE_LTE_ONLY,
            RILConstants.NETWORK_MODE_LTE_CDMA_EVDO -> r = if (show4GForLTE) "network_4G" else "network_lte"
            RILConstants.NETWORK_MODE_CDMA_NO_EVDO -> r = "network_1x"
            RILConstants.NETWORK_MODE_TDSCDMA_ONLY -> r = "network_tdscdma"
            RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM,
            RILConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA,
            RILConstants.NETWORK_MODE_LTE_TDSCDMA,
            RILConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA,
            RILConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA,
            RILConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA -> {
                if (isSupportTdsCdma) {
                    r = "network_lte"
                } else {
                    if (phoneType == RILConstants.CDMA_PHONE || isGlobalCdma || isWorldMode) {
                        r = "network_global"
                    } else {
                        r = if (show4GForLTE) "network_4G" else "network_lte"
                    }
                }
            }
            else -> Log.w(TAG, "unknown phone mode: $networkMode")
        }

        if (r != null) {
            // grab the phone resources
            val phoneResources = getPhoneResources(context)
            if (phoneResources != null) {
                val id = phoneResources.getIdentifier(r, "string", "com.android.phone")
                if (id > 0) {
                    return phoneResources.getString(id)
                } else {
                    Log.w(TAG, "couldn't find resource id with name: $r")
                }
            }
        }
        return null
    }

    private fun isSupportTdscdma(context: Context, subId: Int): Boolean {
        val phoneResources = getPhoneResources(context)
        if (phoneResources != null) {
            val id = phoneResources.getIdentifier(
                "config_support_tdscdma",
                "bool", "com.android.phone"
            )
            if (phoneResources.getBoolean(id)) {
                return true
            }

            val operatorNumeric = TelephonyManager.from(context)
                .getSimOperatorNumeric(subId)

            val tdcdmaArrId = phoneResources.getIdentifier(
                "config_support_tdscdma_roaming_on_networks",
                "string-array", "com.android.phone"
            )

            if (tdcdmaArrId > 0) {
                val numericArray = phoneResources.getStringArray(tdcdmaArrId)
                if (numericArray.isEmpty() || operatorNumeric == null) {
                    return false
                }
                for (numeric in numericArray) {
                    if (operatorNumeric == numeric) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun show4GForLTE(context: Context): Boolean {
        return try {
            val con = context.createPackageContext("com.android.systemui", 0)
            val id = con.resources.getIdentifier(
                "config_show4GForLTE",
                "bool", "com.android.systemui"
            )
            con.resources.getBoolean(id)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isGlobalCDMA(context: Context, subId: Int, isLteOnCdma: Boolean): Boolean {
        val carrierConfigMan = context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as CarrierConfigManager
        val carrierConfig = carrierConfigMan.getConfigForSubId(subId)
        return isLteOnCdma && carrierConfig?.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL) == true
    }

    private fun isLteOnCdma(context: Context, subId: Int): Boolean {
        return TelephonyManager.from(context).getLteOnCdmaMode(subId) == PhoneConstants.LTE_ON_CDMA_TRUE
    }

    private fun isWorldMode(context: Context): Boolean {
        var worldModeOn = false
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?

        val phoneResources = getPhoneResources(context)
        if (phoneResources != null) {
            val id = phoneResources.getIdentifier(
                "config_world_mode",
                "string", "com.android.phone"
            )

            if (id > 0) {
                val configString = phoneResources.getString(id)

                if (!TextUtils.isEmpty(configString)) {
                    val configArray = configString.split(";").toTypedArray()
                    // Check if we have World mode configuration set to True only or config is set to True
                    // and SIM GID value is also set and matches to the current SIM GID.
                    if (configArray.isNotEmpty() &&
                        ((configArray.size == 1 && configArray[0].equals("true", ignoreCase = true)) ||
                                (configArray.size == 2 && !TextUtils.isEmpty(configArray[1]) &&
                                        tm != null && configArray[1].equals(tm.groupIdLevel1, ignoreCase = true)))
                    ) {
                        worldModeOn = true
                    }
                }
            } else {
                Log.w(TAG, "couldn't find resource of config_world_mode")
            }
        }

        return worldModeOn
    }

    private fun getPhoneResources(context: Context): Resources? {
        return try {
            val packageContext = context.createPackageContext("com.android.phone", 0)
            packageContext.resources
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            Log.w(TAG, "couldn't locate resources for com.android.phone!")
            null
        }
    }
}