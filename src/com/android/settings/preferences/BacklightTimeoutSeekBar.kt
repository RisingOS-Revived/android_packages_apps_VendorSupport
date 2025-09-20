/*
 * Copyright (C) 2013 The CyanogenMod Project
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
package com.android.settings.preferences

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.SeekBar

class BacklightTimeoutSeekBar : SeekBar {
    private var mMax: Int = 0
    private var mGap: Int = 0
    private var mUpdatingThumb: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mUpdatingThumb = true
        super.onSizeChanged(w, h, oldw, oldh)
        mUpdatingThumb = false
    }

    override fun setThumb(thumb: Drawable?) {
        mUpdatingThumb = true
        super.setThumb(thumb)
        mUpdatingThumb = false
    }

    override fun setMax(max: Int) {
        mMax = max
        mGap = max / 10
        super.setMax(max + 2 * mGap - 1)
    }

    override fun updateTouchProgress(lastProgress: Int, newProgress: Int): Int {
        return when {
            newProgress < mMax -> newProgress
            newProgress < mMax + mGap -> mMax - 1
            else -> max
        }
    }
}
