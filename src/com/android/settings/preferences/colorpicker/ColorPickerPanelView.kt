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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * This class draws a panel which which will be filled with a color which can be set.
 * It can be used to show the currently selected color which you will get from
 * the [ColorPickerView].
 * @author Daniel Nilsson
 *
 */
class ColorPickerPanelView : View {

    companion object {
        /**
         * The width in pixels of the border
         * surrounding the color panel.
         */
        private const val BORDER_WIDTH_PX = 1f
    }

    private var mDensity = 1f

    private var mBorderColor = 0xff6E6E6E.toInt()
    private var mColor = 0xff000000.toInt()

    private lateinit var mBorderPaint: Paint
    private lateinit var mColorPaint: Paint

    private var mDrawingRect: RectF? = null
    private var mColorRect: RectF? = null

    private var mAlphaPattern: AlphaPatternDrawable? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        mBorderPaint = Paint()
        mColorPaint = Paint()
        mDensity = context.resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        val rect = mColorRect

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            mDrawingRect?.let { canvas.drawRect(it, mBorderPaint) }
        }

        mAlphaPattern?.draw(canvas)

        mColorPaint.color = mColor

        rect?.let { canvas.drawRect(it, mColorPaint) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mDrawingRect = RectF().apply {
            left = paddingLeft.toFloat()
            right = (w - paddingRight).toFloat()
            top = paddingTop.toFloat()
            bottom = (h - paddingBottom).toFloat()
        }

        setUpColorRect()
    }

    private fun setUpColorRect() {
        val dRect = mDrawingRect ?: return

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX

        mColorRect = RectF(left, top, right, bottom)

        mAlphaPattern = AlphaPatternDrawable((5 * mDensity).toInt())

        mColorRect?.let { colorRect ->
            mAlphaPattern?.setBounds(
                Math.round(colorRect.left),
                Math.round(colorRect.top),
                Math.round(colorRect.right),
                Math.round(colorRect.bottom)
            )
        }
    }

    /**
     * Set the color that should be shown by this view.
     * @param color
     */
    var color: Int
        get() = mColor
        set(color) {
            mColor = color
            invalidate()
        }

    /**
     * Set the color of the border surrounding the panel.
     * @param color
     */
    fun setBorderColor(color: Int) {
        mBorderColor = color
        invalidate()
    }

    /**
     * Get the color of the border surrounding the panel.
     */
    fun getBorderColor(): Int {
        return mBorderColor
    }
}
