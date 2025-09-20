/*
 * Copyright (C) 2016-2023 crDroid Android Project
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
import android.content.res.TypedArray
import android.graphics.PorterDuff
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

import com.android.settings.R
import com.android.settings.Utils

import com.android.settings.utils.AdaptivePreferenceUtils

open class CustomSeekBarPreference : Preference, SeekBar.OnSeekBarChangeListener {
    protected val TAG = javaClass.name
    private val SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings"
    protected val ANDROIDNS = "http://schemas.android.com/apk/res/android"

    protected var mInterval = 1
    protected var mShowSign = false
    protected var mUnits = ""
    protected var mContinuousUpdates = false

    protected var mMinValue = 0
    protected var mMaxValue = 100
    protected var mDefaultValueExists = false
    protected var mDefaultValue: Int = 0
    protected var mDefaultValueTextExists = false
    protected var mDefaultValueText: String = ""

    protected var mValue: Int = 0

    protected var mValueTextView: TextView? = null
    protected var mResetImageView: ImageView? = null
    protected var mMinusImageView: ImageView? = null
    protected var mPlusImageView: ImageView? = null
    protected var mSeekBar: SeekBar

    protected var mTrackingTouch = false
    protected var mTrackingValue: Int = 0

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference)
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, mShowSign)
            val units = a.getString(R.styleable.CustomSeekBarPreference_units)
            if (units != null)
                mUnits = " $units"
            mContinuousUpdates = a.getBoolean(R.styleable.CustomSeekBarPreference_continuousUpdates, mContinuousUpdates)
            val defaultValueText = a.getString(R.styleable.CustomSeekBarPreference_defaultValueText)
            mDefaultValueTextExists = defaultValueText != null && defaultValueText.isNotEmpty()
            if (mDefaultValueTextExists) {
                mDefaultValueText = defaultValueText!!
            }
        } finally {
            a.recycle()
        }

        try {
            val newInterval = attrs?.getAttributeValue(SETTINGS_NS, "interval")
            if (newInterval != null)
                mInterval = Integer.parseInt(newInterval)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid interval value", e)
        }
        mMinValue = attrs?.getAttributeIntValue(SETTINGS_NS, "min", mMinValue) ?: mMinValue
        mMaxValue = attrs?.getAttributeIntValue(ANDROIDNS, "max", mMaxValue) ?: mMaxValue
        if (mMaxValue < mMinValue)
            mMaxValue = mMinValue
        val defaultValue = attrs?.getAttributeValue(ANDROIDNS, "defaultValue")
        mDefaultValueExists = defaultValue != null && defaultValue.isNotEmpty()
        if (mDefaultValueExists) {
            mDefaultValue = getLimitedValue(Integer.parseInt(defaultValue))
            mValue = mDefaultValue
        } else {
            mValue = mMinValue
        }

        mSeekBar = SeekBar(context, attrs)
        val layoutRes = AdaptivePreferenceUtils.getSeekBarLayoutResourceId(context, attrs)
        if (layoutRes != -1) {
            setLayoutResource(layoutRes)
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context, attrs, TypedArrayUtils.getAttr(
            context,
            androidx.preference.R.attr.seekBarPreferenceStyle,
            com.android.internal.R.attr.seekBarPreferenceStyle
        )
    )

    constructor(context: Context) : this(context, null)

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)
        this.shouldDisableView = true
        mSeekBar.isEnabled = !disableDependent
        mResetImageView?.isEnabled = !disableDependent
        mPlusImageView?.isEnabled = !disableDependent
        mMinusImageView?.isEnabled = !disableDependent
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        try {
            // move our seekbar to the new view we've been given
            val oldContainer = mSeekBar.parent
            val newContainer = holder.findViewById(R.id.seekbar) as ViewGroup
            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    (oldContainer as ViewGroup).removeView(mSeekBar)
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews()
                newContainer.addView(
                    mSeekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error binding view: ${ex}")
        }

        mSeekBar.max = getSeekValue(mMaxValue)
        mSeekBar.progress = getSeekValue(mValue)
        mSeekBar.isEnabled = isEnabled

        mValueTextView = holder.findViewById(R.id.value) as? TextView
        mResetImageView = holder.findViewById(R.id.reset) as? ImageView
        mMinusImageView = holder.findViewById(R.id.minus) as? ImageView
        mPlusImageView = holder.findViewById(R.id.plus) as? ImageView

        updateValueViews()

        mSeekBar.setOnSeekBarChangeListener(this)
        mResetImageView?.setOnClickListener {
            Toast.makeText(
                context, context.getString(
                    R.string.custom_seekbar_default_value_to_set,
                    getTextValue(mDefaultValue)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
        mResetImageView?.setOnLongClickListener {
            setValue(mDefaultValue, true)
            true
        }
        mMinusImageView?.setOnClickListener {
            setValue(mValue - mInterval, true)
        }
        mMinusImageView?.setOnLongClickListener {
            setValue(
                if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue < mValue * 2)
                    Math.floorDiv(mMaxValue + mMinValue, 2)
                else
                    mMinValue, true
            )
            true
        }
        mPlusImageView?.setOnClickListener {
            setValue(mValue + mInterval, true)
        }
        mPlusImageView?.setOnLongClickListener {
            setValue(
                if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue > mValue * 2)
                    -1 * Math.floorDiv(-1 * (mMaxValue + mMinValue), 2)
                else
                    mMaxValue, true
            )
            true
        }
    }

    protected fun getLimitedValue(v: Int): Int {
        return when {
            v < mMinValue -> mMinValue
            v > mMaxValue -> mMaxValue
            else -> v
        }
    }

    protected fun getSeekValue(v: Int): Int {
        return 0 - Math.floorDiv(mMinValue - v, mInterval)
    }

    protected fun getTextValue(v: Int): String {
        if (mDefaultValueTextExists && mDefaultValueExists && v == mDefaultValue) {
            return mDefaultValueText
        }
        return (if (mShowSign && v > 0) "+" else "") + v.toString() + mUnits
    }

    protected fun updateValueViews() {
        mValueTextView?.let { textView ->
            if (!mTrackingTouch || mContinuousUpdates) {
                if (mDefaultValueTextExists && mDefaultValueExists && mValue == mDefaultValue) {
                    textView.text = mDefaultValueText + " (" +
                            context.getString(R.string.custom_seekbar_default_value) + ")"
                } else {
                    textView.text = context.getString(R.string.custom_seekbar_value, getTextValue(mValue)) +
                            if (mDefaultValueExists && mValue == mDefaultValue) " (" +
                                    context.getString(R.string.custom_seekbar_default_value) + ")" else ""
                }
            } else {
                if (mDefaultValueTextExists && mDefaultValueExists && mTrackingValue == mDefaultValue) {
                    textView.text = "[$mDefaultValueText]"
                } else {
                    textView.text = context.getString(R.string.custom_seekbar_value, "[${getTextValue(mTrackingValue)}]")
                }
            }
        }
        mResetImageView?.let { resetView ->
            resetView.visibility = if (!mDefaultValueExists || mValue == mDefaultValue || mTrackingTouch)
                View.INVISIBLE
            else
                View.VISIBLE
        }
        mMinusImageView?.let { minusView ->
            if (mValue == mMinValue || mTrackingTouch) {
                minusView.isClickable = false
                minusView.setColorFilter(
                    Utils.getColorAttrDefaultColor(context, android.R.attr.textColorTertiary),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                minusView.isClickable = true
                minusView.clearColorFilter()
            }
        }
        mPlusImageView?.let { plusView ->
            if (mValue == mMaxValue || mTrackingTouch) {
                plusView.isClickable = false
                plusView.setColorFilter(
                    Utils.getColorAttrDefaultColor(context, android.R.attr.textColorTertiary),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                plusView.isClickable = true
                plusView.clearColorFilter()
            }
        }
    }

    protected open fun changeValue(newValue: Int) {
        // for subclasses
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val newValue = getLimitedValue(mMinValue + (progress * mInterval))
        if (mTrackingTouch && !mContinuousUpdates) {
            mTrackingValue = newValue
            updateValueViews()
        } else if (mValue != newValue) {
            // change rejected, revert to the previous value
            if (!callChangeListener(newValue)) {
                mSeekBar.progress = getSeekValue(mValue)
                return
            }
            // change accepted, store it
            changeValue(newValue)
            persistInt(newValue)

            mValue = newValue
            updateValueViews()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mTrackingValue = mValue
        mTrackingTouch = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        mTrackingTouch = false
        if (!mContinuousUpdates)
            onProgressChanged(mSeekBar, getSeekValue(mTrackingValue), false)
        notifyChanged()
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        if (restoreValue)
            mValue = getPersistedInt(mValue)
    }

    override fun setDefaultValue(defaultValue: Any?) {
        when (defaultValue) {
            is Int -> setDefaultValue(defaultValue, mSeekBar != null)
            else -> setDefaultValue(defaultValue?.toString(), mSeekBar != null)
        }
    }

    fun setDefaultValue(newValue: Int, update: Boolean) {
        val limitedValue = getLimitedValue(newValue)
        if (!mDefaultValueExists || mDefaultValue != limitedValue) {
            mDefaultValueExists = true
            mDefaultValue = limitedValue
            if (update)
                updateValueViews()
        }
    }

    fun setDefaultValue(newValue: String?, update: Boolean) {
        if (mDefaultValueExists && (newValue == null || newValue.isEmpty())) {
            mDefaultValueExists = false
            if (update)
                updateValueViews()
        } else if (newValue != null && newValue.isNotEmpty()) {
            setDefaultValue(Integer.parseInt(newValue), update)
        }
    }

    fun setValue(newValue: Int) {
        mValue = getLimitedValue(newValue)
        mSeekBar.progress = getSeekValue(mValue)
    }

    fun setValue(newValue: Int, update: Boolean) {
        val limitedValue = getLimitedValue(newValue)
        if (mValue != limitedValue) {
            if (update)
                mSeekBar.progress = getSeekValue(limitedValue)
            else
                mValue = limitedValue
        }
    }

    fun setMax(max: Int) {
        mMaxValue = max
        mSeekBar.max = getSeekValue(mMaxValue)
    }

    fun getValue(): Int {
        return mValue
    }

    fun refresh(newValue: Int) {
        // this will ...
        setValue(newValue, mSeekBar != null)
    }
}
