package com.android.settings.utils

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.android.settings.R

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private val backgroundPaint: Paint
    private val progressPaint: Paint
    private val circleRect: RectF
    private var progress = 0f

    init {
        circleRect = RectF()
        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        // Retrieve theme colors
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        var progressColor = typedValue.data

        context.theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true)
        var backgroundColor = typedValue.data

        var strokeWidth = resources.getDimensionPixelSize(R.dimen.circular_progress_stroke_width).toFloat()

        // Retrieve custom attributes
        attrs?.let {
            val ta: TypedArray = context.obtainStyledAttributes(it, R.styleable.CircularProgressView)
            progressColor = ta.getColor(R.styleable.CircularProgressView_progressColor, progressColor)
            backgroundColor = ta.getColor(R.styleable.CircularProgressView_backgroundColor, backgroundColor)
            strokeWidth = ta.getDimension(R.styleable.CircularProgressView_strokeWidth, strokeWidth)
            ta.recycle()
        }

        // Set up paints
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.color = backgroundColor
        backgroundPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = strokeWidth
        progressPaint.color = progressColor
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = maxOf(progressPaint.strokeWidth, backgroundPaint.strokeWidth)
        circleRect.set(stroke, stroke, w - stroke, h - stroke)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background and progress arcs
        canvas.drawArc(circleRect, 0f, 360f, false, backgroundPaint)
        val sweepAngle = 360 * (progress / 100)
        canvas.drawArc(circleRect, -90f, sweepAngle, false, progressPaint)
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    fun getProgress(): Float {
        return progress
    }
}