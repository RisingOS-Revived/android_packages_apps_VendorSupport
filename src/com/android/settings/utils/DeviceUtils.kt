/*
 * Copyright (C) 2016-2022 crDroid Android Project
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

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.fingerprint.FingerprintManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.SystemProperties
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.Display
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.Surface
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import org.lineageos.internal.util.DeviceKeysConstants.*

object DeviceUtils {

    /* returns whether the device has a centered display cutout or not. */
    fun hasCenteredCutout(context: Context): Boolean {
        val display = context.display
        val cutout = display.cutout
        if (cutout != null) {
            val realSize = Point()
            display.getRealSize(realSize)

            when (display.rotation) {
                Surface.ROTATION_0 -> {
                    val rect = cutout.boundingRectTop
                    return !(rect.left <= 0 || rect.right >= realSize.x)
                }
                Surface.ROTATION_90 -> {
                    val rect = cutout.boundingRectLeft
                    return !(rect.top <= 0 || rect.bottom >= realSize.y)
                }
                Surface.ROTATION_180 -> {
                    val rect = cutout.boundingRectBottom
                    return !(rect.left <= 0 || rect.right >= realSize.x)
                }
                Surface.ROTATION_270 -> {
                    val rect = cutout.boundingRectRight
                    return !(rect.top <= 0 || rect.bottom >= realSize.y)
                }
            }
        }
        return false
    }

    fun getDeviceKeys(context: Context): Int {
        return context.resources.getInteger(
            org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys
        )
    }

    fun getDeviceWakeKeys(context: Context): Int {
        return context.resources.getInteger(
            org.lineageos.platform.internal.R.integer.config_deviceHardwareWakeKeys
        )
    }

    /* returns whether the device has power key or not. */
    fun hasPowerKey(): Boolean {
        return KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
    }

    /* returns whether the device has home key or not. */
    fun hasHomeKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_HOME) != 0
    }

    /* returns whether the device has back key or not. */
    fun hasBackKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_BACK) != 0
    }

    /* returns whether the device has menu key or not. */
    fun hasMenuKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_MENU) != 0
    }

    /* returns whether the device has assist key or not. */
    fun hasAssistKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_ASSIST) != 0
    }

    /* returns whether the device has app switch key or not. */
    fun hasAppSwitchKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_APP_SWITCH) != 0
    }

    /* returns whether the device has camera key or not. */
    fun hasCameraKey(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_CAMERA) != 0
    }

    /* returns whether the device has volume rocker or not. */
    fun hasVolumeKeys(context: Context): Boolean {
        return (getDeviceKeys(context) and KEY_MASK_VOLUME) != 0
    }

    /* returns whether the device can be waken using the home key or not. */
    fun canWakeUsingHomeKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_HOME) != 0
    }

    /* returns whether the device can be waken using the back key or not. */
    fun canWakeUsingBackKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_BACK) != 0
    }

    /* returns whether the device can be waken using the menu key or not. */
    fun canWakeUsingMenuKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_MENU) != 0
    }

    /* returns whether the device can be waken using the assist key or not. */
    fun canWakeUsingAssistKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_ASSIST) != 0
    }

    /* returns whether the device can be waken using the app switch key or not. */
    fun canWakeUsingAppSwitchKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_APP_SWITCH) != 0
    }

    /* returns whether the device can be waken using the camera key or not. */
    fun canWakeUsingCameraKey(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_CAMERA) != 0
    }

    /* returns whether the device can be waken using the volume rocker or not. */
    fun canWakeUsingVolumeKeys(context: Context): Boolean {
        return (getDeviceWakeKeys(context) and KEY_MASK_VOLUME) != 0
    }

    /* returns whether the device supports button backlight adjusment or not. */
    fun hasButtonBacklightSupport(context: Context): Boolean {
        val buttonBrightnessControlSupported = context.resources.getInteger(
            org.lineageos.platform.internal.R.integer
                .config_deviceSupportsButtonBrightnessControl
        ) != 0

        // All hardware keys besides volume and camera can possibly have a backlight
        return buttonBrightnessControlSupported && (hasHomeKey(context) || hasBackKey(context) || hasMenuKey(context) || hasAssistKey(context) || hasAppSwitchKey(context))
    }

    /* returns whether the device supports keyboard backlight adjusment or not. */
    fun hasKeyboardBacklightSupport(context: Context): Boolean {
        return context.resources.getInteger(
            org.lineageos.platform.internal.R.integer
                .config_deviceSupportsKeyboardBrightnessControl
        ) != 0
    }

    fun isPackageInstalled(context: Context, pkg: String?, ignoreState: Boolean): Boolean {
        if (pkg != null) {
            try {
                val pi = context.packageManager.getPackageInfo(pkg, 0)
                if (!pi.applicationInfo?.enabled!! && !ignoreState) {
                    return false
                }
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }
        }

        return true
    }

    /**
     * Locks the activity orientation to the current device orientation
     * @param activity
     */
    fun lockCurrentOrientation(activity: Activity) {
        val currentRotation = activity.display.rotation
        val orientation = activity.resources.configuration.orientation
        val frozenRotation = when (currentRotation) {
            Surface.ROTATION_0 -> if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            Surface.ROTATION_90 -> if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            Surface.ROTATION_180 -> if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

            Surface.ROTATION_270 -> if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        activity.requestedOrientation = frozenRotation
    }

    fun isDozeAvailable(context: Context): Boolean {
        var name: String? = if (Build.IS_DEBUGGABLE) SystemProperties.get("debug.doze.component") else null
        if (TextUtils.isEmpty(name)) {
            name = context.resources.getString(
                com.android.internal.R.string.config_dozeComponent
            )
        }
        return !TextUtils.isEmpty(name)
    }

    fun deviceSupportsMobileData(ctx: Context): Boolean {
        val telephonyManager = ctx.getSystemService(TelephonyManager::class.java)
        return telephonyManager.isDataCapable
    }

    fun deviceSupportsBluetooth(): Boolean {
        return BluetoothAdapter.getDefaultAdapter() != null
    }

    fun deviceSupportsBluetooth(ctx: Context): Boolean {
        val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter != null
    }

    fun deviceSupportsNfc(ctx: Context): Boolean {
        return NfcAdapter.getDefaultAdapter(ctx) != null
    }

    fun deviceSupportsFlashLight(context: Context): Boolean {
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return false
        try {
            val ids = cameraManager.cameraIdList ?: return false
            for (id in ids) {
                val c = cameraManager.getCameraCharacteristics(id)
                val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable != null && flashAvailable && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore ArrayIndexOutOfBoundsException, CameraAccessException, AssertionError
        }
        return false
    }

    fun isMobileDataEnabled(context: Context): Boolean {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        return telephonyManager.createForSubscriptionId(subId).isDataEnabled
    }

    fun isSwipeUpEnabled(context: Context): Boolean {
        if (isEdgeToEdgeEnabled(context)) {
            return false
        }
        return NAV_BAR_MODE_2BUTTON == context.resources.getInteger(
            com.android.internal.R.integer.config_navBarInteractionMode
        )
    }

    fun isEdgeToEdgeEnabled(context: Context): Boolean {
        return NAV_BAR_MODE_GESTURAL == context.resources.getInteger(
            com.android.internal.R.integer.config_navBarInteractionMode
        )
    }

    fun isBlurSupported(): Boolean {
        val blurSupportedSysProp = SystemProperties
            .getBoolean("ro.surface_flinger.supports_background_blur", false)
        val blurDisabledSysProp = SystemProperties
            .getBoolean("persist.sys.sf.disable_blurs", false)
        return blurSupportedSysProp && !blurDisabledSysProp && ActivityManager.isHighEndGfx()
    }

    /**
     * Checks if the device has udfps
     * @param context context for getting FingerprintManager
     * @return true is udfps is present
     */
    fun hasUDFPS(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FingerprintManager::class.java)
        val props = fingerprintManager.sensorPropertiesInternal
        return props != null && props.size == 1 && props[0].isAnyUdfpsType
    }

    @JvmStatic
    fun isCurrentlySupportedPixel(): Boolean {
        val isPixelDevice = SystemProperties.get("ro.product.model").matches(Regex("Pixel (3|4|5|6|7|8|9|10)[a-zA-Z ]*"))
        return isPixelDevice
    }
}
