/*
 * Copyright (C) 2010 Daniel Nilsson
 * Copyright (C) 2013 Slimroms
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
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import androidx.annotation.NonNull
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout

import com.android.settings.R

class ColorPickerDialog(context: Context, initialColor: Int) : AlertDialog(context), 
    ColorPickerView.OnColorChangedListener, View.OnClickListener {

    private lateinit var mColorPicker: ColorPickerView
    private lateinit var mOldColor: ColorPickerPanelView
    private lateinit var mNewColor: ColorPickerPanelView
    private var mHex: EditText? = null

    private var mWhite: ColorPickerPanelView? = null
    private var mBlack: ColorPickerPanelView? = null
    private var mCyan: ColorPickerPanelView? = null
    private var mRed: ColorPickerPanelView? = null
    private var mGreen: ColorPickerPanelView? = null
    private var mYellow: ColorPickerPanelView? = null

    private var mListener: OnColorChangedListener? = null

    interface OnColorChangedListener {
        fun onColorChanged(color: Int)
    }

    init {
        init(initialColor)
    }

    private fun init(color: Int) {
        window?.let { window ->
            window.setFormat(PixelFormat.RGBA_8888)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setUp(color)
        }
    }

    private fun setUp(color: Int) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.dialog_color_picker, null)

        mColorPicker = layout.findViewById(R.id.color_picker_view)
        mOldColor = layout.findViewById(R.id.old_color_panel)
        mNewColor = layout.findViewById(R.id.new_color_panel)

        mWhite = layout.findViewById(R.id.white_panel)
        mBlack = layout.findViewById(R.id.black_panel)
        mCyan = layout.findViewById(R.id.cyan_panel)
        mRed = layout.findViewById(R.id.red_panel)
        mGreen = layout.findViewById(R.id.green_panel)
        mYellow = layout.findViewById(R.id.yellow_panel)

        mHex = layout.findViewById(R.id.hex)
        val mSetButton = layout.findViewById<ImageButton>(R.id.enter)

        (mOldColor.parent as LinearLayout).setPadding(
            Math.round(mColorPicker.getDrawingOffset()).toInt(),
            0, 
            Math.round(mColorPicker.getDrawingOffset()).toInt(), 
            0
        )

        mOldColor.setOnClickListener(this)
        mNewColor.setOnClickListener(this)
        mColorPicker.setOnColorChangedListener(this)
        mOldColor.color = color
        mColorPicker.setColor(color, true)

        setColorAndClickAction(mWhite, Color.WHITE)
        setColorAndClickAction(mBlack, Color.BLACK)
        setColorAndClickAction(mCyan, 0xff24b7d6.toInt())
        setColorAndClickAction(mRed, 0xfff90028.toInt())
        setColorAndClickAction(mGreen, 0xff76c124.toInt())
        setColorAndClickAction(mYellow, 0xffffc90f.toInt())

        mHex?.setText(ColorPickerPreference.convertToARGB(color))
        mSetButton?.setOnClickListener {
            val text = mHex?.text.toString()
            try {
                val newColor = ColorPickerPreference.convertToColorInt(text)
                mColorPicker.setColor(newColor, true)
            } catch (e: Exception) {
                // Ignore invalid color input
            }
        }

        setView(layout)
    }

    override fun onColorChanged(color: Int) {
        mNewColor.color = color
        try {
            mHex?.setText(ColorPickerPreference.convertToARGB(color))
        } catch (e: Exception) {
            // Ignore conversion errors
        }
    }

    fun setAlphaSliderVisible(visible: Boolean) {
        mColorPicker.setAlphaSliderVisible(visible)
    }

    fun setColorAndClickAction(previewRect: ColorPickerPanelView?, color: Int) {
        previewRect?.let { rect ->
            rect.color = color
            rect.setOnClickListener {
                try {
                    mColorPicker.setColor(color, true)
                } catch (e: Exception) {
                    // Ignore color setting errors
                }
            }
        }
    }

    /**
     * Set a OnColorChangedListener to get notified when the color selected by the user has changed.
     *
     * @param listener
     */
    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        mListener = listener
    }

    fun getColor(): Int {
        return mColorPicker.getColor()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.new_color_panel) {
            mListener?.onColorChanged(mNewColor.color)
        }
        dismiss()
    }

    @NonNull
    override fun onSaveInstanceState(): Bundle {
        val state = super.onSaveInstanceState()
        state.putInt("old_color", mOldColor.color)
        state.putInt("new_color", mNewColor.color)
        dismiss()
        return state
    }

    override fun onRestoreInstanceState(@NonNull savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mOldColor.color = savedInstanceState.getInt("old_color")
        mColorPicker.setColor(savedInstanceState.getInt("new_color"), true)
    }
}
