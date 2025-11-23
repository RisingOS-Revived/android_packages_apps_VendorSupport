/*
 * Copyright (C) 2025 RisingOS Revived Android Project
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
import android.content.res.ColorStateList
import android.os.SystemProperties
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R

class LauncherIconPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private var currentValue: Int = 0
    private val launcherData = mutableListOf<LauncherInfo>()

    data class LauncherInfo(
        val name: String,
        val value: Int,
        val iconResId: Int
    )

    init {
        layoutResource = R.layout.preference_launcher_icons
        isSelectable = false

        val res = context.resources
        val entries = res.getStringArray(R.array.quickswitch_launcher_entries)
        val values = res.getStringArray(R.array.quickswitch_launcher_values)
        val icons = intArrayOf(
            R.drawable.ic_ortus_launcher,
            R.drawable.ic_pixel_launcher,
            R.drawable.ic_lawnchair_launcher
        )

        launcherData.add(LauncherInfo(entries[0], values[0].toInt(), icons[0]))

        if (SystemProperties.getInt("persist.sys.quickswitch_pixel_shipped", 0) != 0) {
            launcherData.add(LauncherInfo(entries[1], values[1].toInt(), icons[1]))
        }

        if (SystemProperties.getInt("persist.sys.quickswitch_lawnchair_shipped", 0) != 0) {
            launcherData.add(LauncherInfo(entries[2], values[2].toInt(), icons[2]))
        }

        currentValue = SystemProperties.getInt(key, 0)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.background = null
        holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false

        val container = holder.findViewById(R.id.launcher_icons_container) as LinearLayout
        container.removeAllViews()

        val accentColor = com.google.android.material.color.MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorPrimary,
            context.getColor(android.R.color.system_accent1_500)
        )

        launcherData.forEach { launcher ->
            val itemView = View.inflate(context, R.layout.launcher_icon_item, null)
            val itemContainer = itemView.findViewById<LinearLayout>(R.id.launcher_container)
            val icon = itemView.findViewById<ImageView>(R.id.launcher_icon)
            val name = itemView.findViewById<TextView>(R.id.launcher_name)

            icon.setImageResource(launcher.iconResId)
            name.text = launcher.name

            updateSelectionState(icon, launcher.value == currentValue, accentColor)

            itemContainer.setOnClickListener {
                if (currentValue != launcher.value) {
                    currentValue = launcher.value

                    for (i in 0 until container.childCount) {
                        val childView = container.getChildAt(i)
                        val childIcon = childView.findViewById<ImageView>(R.id.launcher_icon)
                        val childValue = launcherData[i].value
                        updateSelectionState(childIcon, childValue == currentValue, accentColor)
                    }

                    persistInt(currentValue)
                    callChangeListener(currentValue)
                }
            }

            val layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            layoutParams.setMargins(8, 0, 8, 0)
            container.addView(itemView, layoutParams)
        }
    }

    private fun updateSelectionState(icon: ImageView, isSelected: Boolean, accentColor: Int) {
        if (isSelected) {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.TRANSPARENT)
            drawable.setStroke(
                context.resources.getDimensionPixelSize(R.dimen.launcher_card_stroke_selected),
                accentColor
            )
            icon.background = drawable
        } else {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(android.graphics.Color.TRANSPARENT)
            drawable.setStroke(0, android.graphics.Color.TRANSPARENT)
            icon.background = drawable
        }
    }

    fun setValue(value: Int) {
        currentValue = value
        notifyChanged()
    }

    fun getValue(): Int = currentValue
}
