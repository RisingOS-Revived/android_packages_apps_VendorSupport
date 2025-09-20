/*
 * Copyright (c) 2025 Rising Revived Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.preferences.rainbow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.TextView

class RainbowTextView : TextView {
    private var mGradient: LinearGradient? = null
    private var mMatrix: Matrix? = null
    private var mTranslate: Float = 0f
    private var mAnimator: ValueAnimator? = null
    private var mTextWidth: Int = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        setShadowLayer(2f, 0f, 0f, 0x80000000.toInt())
        setLayerType(LAYER_TYPE_HARDWARE, null)
        mMatrix = Matrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return

        // Measure text width for seamless animation
        mTextWidth = paint.measureText(text.toString()).toInt()
        if (mTextWidth <= 0) mTextWidth = w

        // Define the gradient colors - ensure they repeat perfectly
        val colors = intArrayOf(
            0xFF80CBC4.toInt(), // Teal Accent 
            0xFF64B5F6.toInt(), // Light Blue Accent
            0xFF7986CB.toInt(), // Indigo Accent
            0xFFBA68C8.toInt(), // Purple Accent
            0xFFE57373.toInt(), // Soft Red Accent
            0xFFFFB74D.toInt(), // Orange Accent
            0xFFFFD54F.toInt(), // Yellow Accent
            0xFF80CBC4.toInt()  // Teal Accent (repeat first color for seamless looping)
        )

        // Create a gradient exactly as wide as needed for one complete color cycle
        val gradientWidth = mTextWidth.toFloat() // Width of one complete color cycle
        mGradient = LinearGradient(0f, 0f, gradientWidth, 0f, colors, null, Shader.TileMode.REPEAT)
        paint.shader = mGradient

        // Restart the animation
        startAnimation()
    }

    private fun startAnimation() {
        mAnimator?.cancel()

        // Animate exactly one color cycle width for perfect looping
        mAnimator = ValueAnimator.ofFloat(0f, mTextWidth.toFloat())

        // Set to 3 seconds (3000ms) as requested
        mAnimator?.duration = 3000

        // Using INFINITE + RESTART with a perfect cycle means no visible seams
        mAnimator?.repeatCount = ValueAnimator.INFINITE
        mAnimator?.repeatMode = ValueAnimator.RESTART
        mAnimator?.interpolator = LinearInterpolator()

        mAnimator?.addUpdateListener { animation ->
            mTranslate = animation.animatedValue as Float
            invalidate()
        }

        mAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        mGradient?.let { gradient ->
            mMatrix?.setTranslate(-mTranslate, 0f)
            gradient.setLocalMatrix(mMatrix)
        }
        super.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) {
            post { startAnimation() }
        }
    }

    override fun onDetachedFromWindow() {
        mAnimator?.let { animator ->
            animator.cancel()
            mAnimator = null
        }
        super.onDetachedFromWindow()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility == VISIBLE) {
            startAnimation()
        } else {
            mAnimator?.cancel()
        }
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        if (width > 0) {
            onSizeChanged(width, height, width, height)
        }
    }
}
