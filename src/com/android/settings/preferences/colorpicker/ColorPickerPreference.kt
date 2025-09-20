/*
 * Copyright (C) 2011 Sergey Margaritov
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2015 The TeamEos Project
 * Copyright (C) 2020-2021 crDroid Android Project
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

package com.android.settings.preferences.colorpicker

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.GridLayout
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout

import com.android.settings.R

import com.android.settings.utils.AdaptivePreferenceUtils

/**
 * A preference type that allows a user to choose a time
 * @author Sergey Margaritov
 */
open class ColorPickerPreference : Preference, ColorPickerDialog.OnColorChangedListener {

    companion object {
        private const val ANDROIDNS = "http://schemas.android.com/apk/res/android"
        private const val SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings"

        fun convertToRGB(color: Int): String {
            var red = Integer.toHexString(Color.red(color))
            var green = Integer.toHexString(Color.green(color))
            var blue = Integer.toHexString(Color.blue(color))

            if (red.length == 1) {
                red = "0$red"
            }

            if (green.length == 1) {
                green = "0$green"
            }

            if (blue.length == 1) {
                blue = "0$blue"
            }

            return "#$red$green$blue"
        }

        /**
         * For custom purposes. Not used by ColorPickerPreferrence
         *
         * @param color
         * @author Unknown
         */
        fun convertToARGB(color: Int): String {
            var alpha = Integer.toHexString(Color.alpha(color))
            var red = Integer.toHexString(Color.red(color))
            var green = Integer.toHexString(Color.green(color))
            var blue = Integer.toHexString(Color.blue(color))

            if (alpha.length == 1) {
                alpha = "0$alpha"
            }

            if (red.length == 1) {
                red = "0$red"
            }

            if (green.length == 1) {
                green = "0$green"
            }

            if (blue.length == 1) {
                blue = "0$blue"
            }

            return "#$alpha$red$green$blue"
        }

        /**
         * For custom purposes. Not used by ColorPickerPreferrence
         *
         * @param argb
         * @throws NumberFormatException
         * @author Unknown
         */
        @Throws(NumberFormatException::class)
        fun convertToColorInt(argb: String): Int {
            var argbString = argb
            if (argbString.startsWith("#")) {
                argbString = argbString.replace("#", "")
            }

            var alpha = -1
            var red = -1
            var green = -1
            var blue = -1

            when (argbString.length) {
                8 -> {
                    alpha = Integer.parseInt(argbString.substring(0, 2), 16)
                    red = Integer.parseInt(argbString.substring(2, 4), 16)
                    green = Integer.parseInt(argbString.substring(4, 6), 16)
                    blue = Integer.parseInt(argbString.substring(6, 8), 16)
                }
                6 -> {
                    alpha = 255
                    red = Integer.parseInt(argbString.substring(0, 2), 16)
                    green = Integer.parseInt(argbString.substring(2, 4), 16)
                    blue = Integer.parseInt(argbString.substring(4, 6), 16)
                }
            }

            return Color.argb(alpha, red, green, blue)
        }

        private fun createOvalShape(size: Int, color: Int): ShapeDrawable {
            val shape = ShapeDrawable(OvalShape())
            shape.intrinsicHeight = size
            shape.intrinsicWidth = size
            shape.paint.color = color
            return shape
        }
    }

    private var mView: PreferenceViewHolder? = null
    private var mWidgetFrameView: LinearLayout? = null
    private var mDialog: ColorPickerDialog? = null
    private var mDefaultValue = Color.BLACK
    private var mCurrentValue = mDefaultValue
    private var mCurrentHexValue: String? = null
    private var mDensity = 0f
    private var mAlphaSliderEnabled = false
    private var mShowReset: Boolean = false
    private var mShowPreview: Boolean = false
    private var mDividerAbove: Boolean = false
    private var mDividerBelow: Boolean = false
    private var mAutoSummary = true
    private var mEditText: EditText? = null

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        val layoutRes = AdaptivePreferenceUtils.getLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            layoutResource = layoutRes
        }
        init(context, attrs)
    }

    override fun onGetDefaultValue(ta: TypedArray, index: Int): Any {
        return ta.getInt(index, Color.BLACK)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        // when using PreferenceDataStore, restorePersistedValue is always true (see Preference class for reference)
        // so we load the persistent value with getPersistedInt if available in the data store, 
        // and use defaultValue as fallback (onGetDefaultValue has been already called and it loaded the android:defaultValue attr from our xml).
        val fallbackValue = defaultValue ?: Color.BLACK
        mCurrentValue = Settings.System.getInt(
            context.contentResolver, key, fallbackValue as Int
        )
        mCurrentHexValue = convertToARGB(fallbackValue as Int)
        if (mAutoSummary) summary = mCurrentHexValue
        onColorChanged(mCurrentValue)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        mDensity = context.resources.displayMetrics.density
        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false)
            mDefaultValue = attrs.getAttributeIntValue(ANDROIDNS, "defaultValue", Color.BLACK)
            mShowReset = attrs.getAttributeBooleanValue(SETTINGS_NS, "showReset", true)
            mShowPreview = attrs.getAttributeBooleanValue(SETTINGS_NS, "showPreview", true)
            mDividerAbove = attrs.getAttributeBooleanValue(SETTINGS_NS, "dividerAbove", false)
            mDividerBelow = attrs.getAttributeBooleanValue(SETTINGS_NS, "dividerBelow", false)
        }
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        mView = view
        super.onBindViewHolder(view)
        view.setDividerAllowedAbove(mDividerAbove)
        view.setDividerAllowedBelow(mDividerBelow)

        view.itemView.setOnClickListener {
            showDialog(null)
        }
        mWidgetFrameView = view.findViewById(android.R.id.widget_frame) as? LinearLayout
        mWidgetFrameView?.let { widgetFrame ->
            widgetFrame.orientation = LinearLayout.HORIZONTAL
            widgetFrame.visibility = View.VISIBLE
            widgetFrame.minimumWidth = 0
            widgetFrame.setPadding(
                widgetFrame.paddingLeft,
                widgetFrame.paddingTop,
                (mDensity * 8).toInt(),
                widgetFrame.paddingBottom
            )
        }
        setDefaultButton()
        setPreviewColor()
    }

    /**
     * Restore a default value, not necessarily a color
     * For example: Set default value to -1 to remove a color filter
     *
     * @author Randall Rushing aka Bigrushdog
     */
    private fun setDefaultButton() {
        if (!mShowReset || mView == null || mWidgetFrameView == null)
            return

        // remove already created default button
        val count = mWidgetFrameView!!.childCount
        if (count > 0) {
            val oldView = mWidgetFrameView!!.findViewWithTag<View>("default")
            val spacer = mWidgetFrameView!!.findViewWithTag<View>("spacer")
            oldView?.let { mWidgetFrameView!!.removeView(it) }
            spacer?.let { mWidgetFrameView!!.removeView(it) }
        }

        if (!isEnabled) return

        val defView = ImageView(context)
        mWidgetFrameView!!.addView(defView)
        defView.setImageDrawable(context.getDrawable(R.drawable.ic_settings_backup_restore))
        defView.tag = "default"
        defView.setOnClickListener {
            onColorChanged(mDefaultValue)
        }
        // sorcery for a linear layout ugh
        val spacer = View(context)
        spacer.tag = "spacer"
        spacer.layoutParams = LinearLayout.LayoutParams(
            (mDensity * 16).toInt(),
            LayoutParams.MATCH_PARENT
        )
        mWidgetFrameView!!.addView(spacer)
    }

    private fun setPreviewColor() {
        if (!mShowPreview || mView == null || mWidgetFrameView == null)
            return

        // remove already create preview image
        val count = mWidgetFrameView!!.childCount
        if (count > 0) {
            val preview = mWidgetFrameView!!.findViewWithTag<View>("preview")
            preview?.let { mWidgetFrameView!!.removeView(it) }
        }
        if (!isEnabled) return
        val iView = ImageView(context)
        mWidgetFrameView!!.addView(iView)
        val size = context.resources.getDimension(R.dimen.oval_notification_size).toInt()
        val imageColor = if ((mCurrentValue and 0xF0F0F0) == 0xF0F0F0) {
            mCurrentValue - 0x101010
        } else {
            mCurrentValue
        }
        iView.setImageDrawable(createOvalShape(size, 0xFF000000.toInt() + imageColor))
        iView.tag = "preview"
    }

    override fun onColorChanged(color: Int) {
        mCurrentValue = color
        mCurrentHexValue = convertToARGB(color)
        if (mAutoSummary) summary = mCurrentHexValue
        setPreviewColor()
        Settings.System.putInt(context.contentResolver, key, color)
        try {
            onPreferenceChangeListener?.onPreferenceChange(this, color)
        } catch (e: NullPointerException) {
        }
        try {
            mEditText?.setText(Integer.toString(color, 16))
        } catch (e: NullPointerException) {
        }
    }

    protected fun showDialog(state: Bundle?) {
        if (!isEnabled)
            return

        mDialog = ColorPickerDialog(context, mCurrentValue)
        mDialog!!.setOnColorChangedListener(this)
        if (mAlphaSliderEnabled) {
            mDialog!!.setAlphaSliderVisible(true)
        }
        if (state != null) {
            mDialog!!.onRestoreInstanceState(state)
        }
        mDialog!!.show()
        mDialog!!.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     *
     * @param enable
     */
    fun setAlphaSliderEnabled(enable: Boolean) {
        mAlphaSliderEnabled = enable
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * set color preview value from outside
     * @author kufikugel
     */
    fun setNewPreviewColor(color: Int) {
        onColorChanged(color)
    }

    fun setDefaultValue(value: Int) {
        mDefaultValue = value
    }

    /**
     * Toggle Auto Summary (by default it's enabled)
     *
     * @param enable
     */
    fun setAutoSummaryEnabled(enable: Boolean) {
        mAutoSummary = enable
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        if (mDialog == null || !mDialog!!.isShowing) {
            return superState
        }

        val myState = SavedState(superState)
        myState.dialogBundle = mDialog!!.onSaveInstanceState()
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state !is SavedState) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state
        super.onRestoreInstanceState(myState.superState)
        showDialog(myState.dialogBundle)
    }

    private class SavedState : BaseSavedState {
        var dialogBundle: Bundle? = null

        constructor(source: Parcel) : super(source) {
            dialogBundle = source.readBundle()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeBundle(dialogBundle)
        }

        constructor(superState: Parcelable?) : super(superState)

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setPreviewColor()
        setDefaultButton()
    }
}
