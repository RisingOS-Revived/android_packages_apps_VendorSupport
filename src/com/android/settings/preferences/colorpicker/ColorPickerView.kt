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
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.round

/**
 * Displays a color picker to the user and allow them
 * to select a color. A slider for the alpha channel is
 * also available. Enable it by setting
 * setAlphaSliderVisible(boolean) to true.
 * @author Daniel Nilsson
 */
class ColorPickerView : View {

    companion object {
        private const val PANEL_SAT_VAL = 0
        private const val PANEL_HUE = 1
        private const val PANEL_ALPHA = 2

        /**
         * The width in pixels of the border
         * surrounding all color panels.
         */
        private const val BORDER_WIDTH_PX = 1f
    }

    /**
     * The width in dp of the hue panel.
     */
    private var HUE_PANEL_WIDTH = 30f
    /**
     * The height in dp of the alpha panel
     */
    private var ALPHA_PANEL_HEIGHT = 20f
    /**
     * The distance in dp between the different
     * color panels.
     */
    private var PANEL_SPACING = 10f
    /**
     * The radius in dp of the color palette tracker circle.
     */
    private var PALETTE_CIRCLE_TRACKER_RADIUS = 5f
    /**
     * The dp which the tracker of the hue or alpha panel
     * will extend outside of its bounds.
     */
    private var RECTANGLE_TRACKER_OFFSET = 2f

    private var mDensity = 1f

    private var mListener: OnColorChangedListener? = null

    private lateinit var mSatValPaint: Paint
    private lateinit var mSatValTrackerPaint: Paint

    private lateinit var mHuePaint: Paint
    private lateinit var mHueTrackerPaint: Paint

    private lateinit var mAlphaPaint: Paint
    private lateinit var mAlphaTextPaint: Paint

    private lateinit var mBorderPaint: Paint

    private var mValShader: Shader? = null
    private var mSatShader: Shader? = null
    private var mHueShader: Shader? = null
    private var mAlphaShader: Shader? = null

    private var mAlpha = 0xff
    private var mHue = 360f
    private var mSat = 0f
    private var mVal = 0f

    private var mAlphaSliderText = ""
    private var mSliderTrackerColor = 0xff1c1c1c.toInt()
    private var mBorderColor = 0xff6E6E6E.toInt()
    private var mShowAlphaPanel = false

    /*
     * To remember which panel that has the "focus" when
     * processing hardware button data.
     */
    private var mLastTouchedPanel = PANEL_SAT_VAL

    /**
     * Offset from the edge we must have or else
     * the finger tracker will get clipped when
     * it is drawn outside of the view.
     */
    private var mDrawingOffset: Float = 0f

    /*
     * Distance form the edges of the view
     * of where we are allowed to draw.
     */
    private lateinit var mDrawingRect: RectF

    private lateinit var mSatValRect: RectF
    private lateinit var mHueRect: RectF
    private var mAlphaRect: RectF? = null

    private var mAlphaPattern: AlphaPatternDrawable? = null

    private var mStartTouchPoint: Point? = null

    interface OnColorChangedListener {
        fun onColorChanged(color: Int)
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        mDensity = context.resources.displayMetrics.density
        PALETTE_CIRCLE_TRACKER_RADIUS *= mDensity
        RECTANGLE_TRACKER_OFFSET *= mDensity
        HUE_PANEL_WIDTH *= mDensity
        ALPHA_PANEL_HEIGHT *= mDensity
        PANEL_SPACING = PANEL_SPACING * mDensity

        mDrawingOffset = calculateRequiredOffset()

        initPaintTools()

        //Needed for receiving trackball motion events.
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun initPaintTools() {
        mSatValPaint = Paint()
        mSatValTrackerPaint = Paint()
        mHuePaint = Paint()
        mHueTrackerPaint = Paint()
        mAlphaPaint = Paint()
        mAlphaTextPaint = Paint()
        mBorderPaint = Paint()

        mSatValTrackerPaint.style = Style.STROKE
        mSatValTrackerPaint.strokeWidth = 2f * mDensity
        mSatValTrackerPaint.isAntiAlias = true

        mHueTrackerPaint.color = mSliderTrackerColor
        mHueTrackerPaint.style = Style.STROKE
        mHueTrackerPaint.strokeWidth = 2f * mDensity
        mHueTrackerPaint.isAntiAlias = true

        mAlphaTextPaint.color = 0xff1c1c1c.toInt()
        mAlphaTextPaint.textSize = 14f * mDensity
        mAlphaTextPaint.isAntiAlias = true
        mAlphaTextPaint.textAlign = Align.CENTER
        mAlphaTextPaint.isFakeBoldText = true
    }

    private fun calculateRequiredOffset(): Float {
        var offset = max(PALETTE_CIRCLE_TRACKER_RADIUS, RECTANGLE_TRACKER_OFFSET)
        offset = max(offset, BORDER_WIDTH_PX * mDensity)

        return offset * 1.5f
    }

    private fun buildHueColorArray(): IntArray {
        val hue = IntArray(361)

        var count = 0
        for (i in hue.size - 1 downTo 0) {
            hue[count] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
            count++
        }

        return hue
    }

    override fun onDraw(canvas: Canvas) {
        if (mDrawingRect.width() <= 0 || mDrawingRect.height() <= 0) return

        drawSatValPanel(canvas)
        drawHuePanel(canvas)
        drawAlphaPanel(canvas)
    }

    private fun drawSatValPanel(canvas: Canvas) {
        val rect = mSatValRect

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(
                mDrawingRect.left,
                mDrawingRect.top, rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX, mBorderPaint
            )
        }

        if (mValShader == null) {
            mValShader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                0xffffffff.toInt(), 0xff000000.toInt(), TileMode.CLAMP
            )
        }

        val rgb = Color.HSVToColor(floatArrayOf(mHue, 1f, 1f))

        mSatShader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top,
            0xffffffff.toInt(), rgb, TileMode.CLAMP
        )
        val mShader = ComposeShader(
            mValShader!!, mSatShader!!, PorterDuff.Mode.MULTIPLY
        )
        mSatValPaint.shader = mShader

        canvas.drawRect(rect, mSatValPaint)

        val p = satValToPoint(mSat, mVal)

        mSatValTrackerPaint.color = 0xff000000.toInt()
        canvas.drawCircle(
            p.x.toFloat(), p.y.toFloat(), PALETTE_CIRCLE_TRACKER_RADIUS - 1f * mDensity, mSatValTrackerPaint
        )

        mSatValTrackerPaint.color = 0xffdddddd.toInt()
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), PALETTE_CIRCLE_TRACKER_RADIUS, mSatValTrackerPaint)
    }

    private fun drawHuePanel(canvas: Canvas) {
        val rect = mHueRect

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(
                rect.left - BORDER_WIDTH_PX,
                rect.top - BORDER_WIDTH_PX,
                rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX,
                mBorderPaint
            )
        }

        if (mHueShader == null) {
            mHueShader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                buildHueColorArray(), null, TileMode.CLAMP
            )
            mHuePaint.shader = mHueShader
        }

        canvas.drawRect(rect, mHuePaint)

        val rectHeight = 4 * mDensity / 2

        val p = hueToPoint(mHue)

        val r = RectF()
        r.left = rect.left - RECTANGLE_TRACKER_OFFSET
        r.right = rect.right + RECTANGLE_TRACKER_OFFSET
        r.top = p.y - rectHeight
        r.bottom = p.y + rectHeight

        canvas.drawRoundRect(r, 2f, 2f, mHueTrackerPaint)
    }

    private fun drawAlphaPanel(canvas: Canvas) {
        if (!mShowAlphaPanel || mAlphaRect == null || mAlphaPattern == null) return

        val rect = mAlphaRect!!

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.color = mBorderColor
            canvas.drawRect(
                rect.left - BORDER_WIDTH_PX,
                rect.top - BORDER_WIDTH_PX,
                rect.right + BORDER_WIDTH_PX,
                rect.bottom + BORDER_WIDTH_PX,
                mBorderPaint
            )
        }

        mAlphaPattern!!.draw(canvas)

        val hsv = floatArrayOf(mHue, mSat, mVal)
        val color = Color.HSVToColor(hsv)
        val acolor = Color.HSVToColor(0, hsv)

        mAlphaShader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top,
            color, acolor, TileMode.CLAMP
        )

        mAlphaPaint.shader = mAlphaShader

        canvas.drawRect(rect, mAlphaPaint)

        if (mAlphaSliderText.isNotEmpty()) {
            canvas.drawText(
                mAlphaSliderText, rect.centerX(),
                rect.centerY() + 4 * mDensity, mAlphaTextPaint
            )
        }

        val rectWidth = 4 * mDensity / 2

        val p = alphaToPoint(mAlpha)

        val r = RectF()
        r.left = p.x - rectWidth
        r.right = p.x + rectWidth
        r.top = rect.top - RECTANGLE_TRACKER_OFFSET
        r.bottom = rect.bottom + RECTANGLE_TRACKER_OFFSET

        canvas.drawRoundRect(r, 2f, 2f, mHueTrackerPaint)
    }

    private fun hueToPoint(hue: Float): Point {
        val rect = mHueRect
        val height = rect.height()

        val p = Point()

        p.y = (height - (hue * height / 360f) + rect.top).toInt()
        p.x = rect.left.toInt()

        return p
    }

    private fun satValToPoint(sat: Float, `val`: Float): Point {
        val rect = mSatValRect
        val height = rect.height()
        val width = rect.width()

        val p = Point()

        p.x = (sat * width + rect.left).toInt()
        p.y = ((1f - `val`) * height + rect.top).toInt()

        return p
    }

    private fun alphaToPoint(alpha: Int): Point {
        val rect = mAlphaRect!!
        val width = rect.width()

        val p = Point()

        p.x = (width - (alpha * width / 0xff) + rect.left).toInt()
        p.y = rect.top.toInt()

        return p
    }

    private fun pointToSatVal(x: Float, y: Float): FloatArray {
        val rect = mSatValRect
        val result = FloatArray(2)

        val width = rect.width()
        val height = rect.height()

        val adjustedX = when {
            x < rect.left -> 0f
            x > rect.right -> width
            else -> x - rect.left
        }

        val adjustedY = when {
            y < rect.top -> 0f
            y > rect.bottom -> height
            else -> y - rect.top
        }

        result[0] = 1f / width * adjustedX
        result[1] = 1f - (1f / height * adjustedY)

        return result
    }

    private fun pointToHue(y: Float): Float {
        val rect = mHueRect

        val height = rect.height()

        val adjustedY = when {
            y < rect.top -> 0f
            y > rect.bottom -> height
            else -> y - rect.top
        }

        return 360f - (adjustedY * 360f / height)
    }

    private fun pointToAlpha(x: Int): Int {
        val rect = mAlphaRect!!
        val width = rect.width().toInt()

        val adjustedX = when {
            x < rect.left -> 0
            x > rect.right -> width
            else -> x - rect.left.toInt()
        }

        return 0xff - (adjustedX * 0xff / width)
    }

    override fun onTrackballEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        var update = false

        if (event.action == MotionEvent.ACTION_MOVE) {
            when (mLastTouchedPanel) {
                PANEL_SAT_VAL -> {
                    var sat = mSat + x / 50f
                    var `val` = mVal - y / 50f

                    sat = when {
                        sat < 0f -> 0f
                        sat > 1f -> 1f
                        else -> sat
                    }

                    `val` = when {
                        `val` < 0f -> 0f
                        `val` > 1f -> 1f
                        else -> `val`
                    }

                    mSat = sat
                    mVal = `val`

                    update = true
                }

                PANEL_HUE -> {
                    var hue = mHue - y * 10f

                    hue = when {
                        hue < 0f -> 0f
                        hue > 360f -> 360f
                        else -> hue
                    }

                    mHue = hue

                    update = true
                }

                PANEL_ALPHA -> {
                    if (!mShowAlphaPanel || mAlphaRect == null) {
                        update = false
                    } else {
                        var alpha = (mAlpha - x * 10).toInt()

                        alpha = when {
                            alpha < 0 -> 0
                            alpha > 0xff -> 0xff
                            else -> alpha
                        }

                        mAlpha = alpha

                        update = true
                    }
                }
            }
        }

        if (update) {
            mListener?.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
            invalidate()
            return true
        }

        return super.onTrackballEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var update = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartTouchPoint = Point(event.x.toInt(), event.y.toInt())
                update = moveTrackersIfNeeded(event)
            }

            MotionEvent.ACTION_MOVE -> {
                update = moveTrackersIfNeeded(event)
            }

            MotionEvent.ACTION_UP -> {
                mStartTouchPoint = null
                update = moveTrackersIfNeeded(event)
            }
        }

        if (update) {
            mListener?.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
            invalidate()
            return true
        }

        return super.onTouchEvent(event)
    }

    private fun moveTrackersIfNeeded(event: MotionEvent): Boolean {
        if (mStartTouchPoint == null) return false

        var update = false

        val startX = mStartTouchPoint!!.x
        val startY = mStartTouchPoint!!.y

        when {
            mHueRect.contains(startX.toFloat(), startY.toFloat()) -> {
                mLastTouchedPanel = PANEL_HUE
                mHue = pointToHue(event.y)
                update = true
            }

            mSatValRect.contains(startX.toFloat(), startY.toFloat()) -> {
                mLastTouchedPanel = PANEL_SAT_VAL
                val result = pointToSatVal(event.x, event.y)
                mSat = result[0]
                mVal = result[1]
                update = true
            }

            mAlphaRect != null && mAlphaRect!!.contains(startX.toFloat(), startY.toFloat()) -> {
                mLastTouchedPanel = PANEL_ALPHA
                mAlpha = pointToAlpha(event.x.toInt())
                update = true
            }
        }

        return update
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 0
        var height = 0

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val widthAllowed = MeasureSpec.getSize(widthMeasureSpec)
        val heightAllowed = MeasureSpec.getSize(heightMeasureSpec)

        val finalWidthAllowed = chooseWidth(widthMode, widthAllowed)
        val finalHeightAllowed = chooseHeight(heightMode, heightAllowed)

        if (!mShowAlphaPanel) {
            height = (finalWidthAllowed - PANEL_SPACING - HUE_PANEL_WIDTH).toInt()

            //If calculated height (based on the width) is more than the allowed height.
            if (height > finalHeightAllowed) {
                height = finalHeightAllowed
                width = (height + PANEL_SPACING + HUE_PANEL_WIDTH).toInt()
            } else {
                width = finalWidthAllowed
            }
        } else {
            width = (finalHeightAllowed - ALPHA_PANEL_HEIGHT + HUE_PANEL_WIDTH).toInt()

            if (width > finalWidthAllowed) {
                width = finalWidthAllowed
                height = (finalWidthAllowed - HUE_PANEL_WIDTH + ALPHA_PANEL_HEIGHT).toInt()
            } else {
                height = finalHeightAllowed
            }
        }

        setMeasuredDimension(width, height)
    }

    private fun chooseWidth(mode: Int, size: Int): Int {
        return if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            size
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            getPrefferedWidth()
        }
    }

    private fun chooseHeight(mode: Int, size: Int): Int {
        return if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            size
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            getPrefferedHeight()
        }
    }

    private fun getPrefferedWidth(): Int {
        var width = getPrefferedHeight()

        if (mShowAlphaPanel) {
            width -= (PANEL_SPACING + ALPHA_PANEL_HEIGHT).toInt()
        }

        return (width + HUE_PANEL_WIDTH + PANEL_SPACING).toInt()
    }

    private fun getPrefferedHeight(): Int {
        var height = (200 * mDensity).toInt()

        if (mShowAlphaPanel) {
            height += (PANEL_SPACING + ALPHA_PANEL_HEIGHT).toInt()
        }

        return height
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mDrawingRect = RectF()
        mDrawingRect.left = mDrawingOffset + paddingLeft
        mDrawingRect.right = w - mDrawingOffset - paddingRight
        mDrawingRect.top = mDrawingOffset + paddingTop
        mDrawingRect.bottom = h - mDrawingOffset - paddingBottom

        setUpSatValRect()
        setUpHueRect()
        setUpAlphaRect()
    }

    private fun setUpSatValRect() {
        val dRect = mDrawingRect
        var panelSide = dRect.height() - BORDER_WIDTH_PX * 2

        if (mShowAlphaPanel) {
            panelSide -= PANEL_SPACING + ALPHA_PANEL_HEIGHT
        }

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = top + panelSide
        val right = left + panelSide

        mSatValRect = RectF(left, top, right, bottom)
    }

    private fun setUpHueRect() {
        val dRect = mDrawingRect

        val left = dRect.right - HUE_PANEL_WIDTH + BORDER_WIDTH_PX
        val top = dRect.top + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX - (if (mShowAlphaPanel) (PANEL_SPACING + ALPHA_PANEL_HEIGHT) else 0f)
        val right = dRect.right - BORDER_WIDTH_PX

        mHueRect = RectF(left, top, right, bottom)
    }

    private fun setUpAlphaRect() {
        if (!mShowAlphaPanel) return

        val dRect = mDrawingRect

        val left = dRect.left + BORDER_WIDTH_PX
        val top = dRect.bottom - ALPHA_PANEL_HEIGHT + BORDER_WIDTH_PX
        val bottom = dRect.bottom - BORDER_WIDTH_PX
        val right = dRect.right - BORDER_WIDTH_PX

        mAlphaRect = RectF(left, top, right, bottom)

        mAlphaPattern = AlphaPatternDrawable((5 * mDensity).toInt())
        mAlphaPattern!!.setBounds(
            round(mAlphaRect!!.left).toInt(),
            round(mAlphaRect!!.top).toInt(),
            round(mAlphaRect!!.right).toInt(),
            round(mAlphaRect!!.bottom).toInt()
        )
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     * @param listener
     */
    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        mListener = listener
    }

    /**
     * Set the color of the border surrounding all panels.
     * @param color
     */
    fun setBorderColor(color: Int) {
        mBorderColor = color
        invalidate()
    }

    /**
     * Get the color of the border surrounding all panels.
     */
    fun getBorderColor(): Int {
        return mBorderColor
    }

    /**
     * Get the current color this view is showing.
     * @return the current color.
     */
    fun getColor(): Int {
        return Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal))
    }

    /**
     * Set the color the view should show.
     * @param color The color that should be selected.
     */
    fun setColor(color: Int) {
        setColor(color, false)
    }

    /**
     * Set the color this view should show.
     * @param color The color that should be selected.
     * @param callback If you want to get a callback to
     * your OnColorChangedListener.
     */
    fun setColor(color: Int, callback: Boolean) {
        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val blue = Color.blue(color)
        val green = Color.green(color)

        val hsv = FloatArray(3)

        Color.RGBToHSV(red, green, blue, hsv)

        mAlpha = alpha
        mHue = hsv[0]
        mSat = hsv[1]
        mVal = hsv[2]

        if (callback && mListener != null) {
            mListener!!.onColorChanged(Color.HSVToColor(mAlpha, floatArrayOf(mHue, mSat, mVal)))
        }

        invalidate()
    }

    /**
     * Get the drawing offset of the color picker view.
     * The drawing offset is the distance from the side of
     * a panel to the side of the view minus the padding.
     * Useful if you want to have your own panel below showing
     * the currently selected color and want to align it perfectly.
     * @return The offset in pixels.
     */
    fun getDrawingOffset(): Float {
        return mDrawingOffset
    }

    /**
     * Set if the user is allowed to adjust the alpha panel. Default is false.
     * If it is set to false no alpha will be set.
     * @param visible
     */
    fun setAlphaSliderVisible(visible: Boolean) {
        if (mShowAlphaPanel != visible) {
            mShowAlphaPanel = visible

            /*
             * Reset all shader to force a recreation.
             * Otherwise they will not look right after
             * the size of the view has changed.
             */
            mValShader = null
            mSatShader = null
            mHueShader = null
            mAlphaShader = null

            requestLayout()
        }
    }

    fun setSliderTrackerColor(color: Int) {
        mSliderTrackerColor = color
        mHueTrackerPaint.color = mSliderTrackerColor
        invalidate()
    }

    fun getSliderTrackerColor(): Int {
        return mSliderTrackerColor
    }

    /**
     * Set the text that should be shown in the
     * alpha slider. Set to null to disable text.
     * @param res string resource id.
     */
    fun setAlphaSliderText(res: Int) {
        val text = context.getString(res)
        setAlphaSliderText(text)
    }

    /**
     * Set the text that should be shown in the
     * alpha slider. Set to null to disable text.
     * @param text Text that should be shown.
     */
    fun setAlphaSliderText(text: String?) {
        mAlphaSliderText = text ?: ""
        invalidate()
    }

    /**
     * Get the current value of the text
     * that will be shown in the alpha
     * slider.
     * @return
     */
    fun getAlphaSliderText(): String {
        return mAlphaSliderText
    }
}
