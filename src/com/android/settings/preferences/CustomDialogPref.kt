/*
 * SPDX-FileCopyrightText: 2015 The Android Open Source Project
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017,2019,2021-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.preferences

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.AttributeSet
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat

open class CustomDialogPref<T : DialogInterface> : DialogPreference {

    private var mFragment: CustomPreferenceDialogFragment? = null

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    fun isDialogOpen(): Boolean {
        return getDialog() != null && getDialog() is Dialog && (getDialog() as Dialog).isShowing
    }

    @Suppress("UNCHECKED_CAST")
    fun getDialog(): T? {
        return mFragment?.dialog as? T
    }

    protected open fun onPrepareDialogBuilder(builder: AlertDialog.Builder, listener: DialogInterface.OnClickListener) {
    }

    protected open fun onDialogClosed(positiveResult: Boolean) {
    }

    protected open fun onClick(dialog: T, which: Int) {
    }

    protected open fun onBindDialogView(view: View) {
    }

    protected open fun onStart() {
    }

    protected open fun onStop() {
    }

    protected open fun onPause() {
    }

    protected open fun onResume() {
    }

    open fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        return null
    }

    protected open fun onCreateDialogView(context: Context): View? {
        return null
    }

    private fun setFragment(fragment: CustomPreferenceDialogFragment) {
        mFragment = fragment
    }

    protected open fun onDismissDialog(dialog: T, which: Int): Boolean {
        return true
    }

    class CustomPreferenceDialogFragment : PreferenceDialogFragmentCompat() {

        companion object {
            fun newInstance(key: String): CustomPreferenceDialogFragment {
                val fragment = CustomPreferenceDialogFragment()
                val b = Bundle(1)
                b.putString(ARG_KEY, key)
                fragment.arguments = b
                return fragment
            }
        }

        private fun getCustomizablePreference(): CustomDialogPref<*> {
            return preference as CustomDialogPref<*>
        }

        private inner class OnDismissListener(
            private val mDialog: DialogInterface,
            private val mWhich: Int
        ) : View.OnClickListener {

            override fun onClick(view: View) {
                this@CustomPreferenceDialogFragment.onClick(mDialog, mWhich)
                @Suppress("UNCHECKED_CAST")
                if ((getCustomizablePreference() as CustomDialogPref<DialogInterface>).onDismissDialog(mDialog as DialogInterface, mWhich)) {
                    mDialog.dismiss()
                }
            }
        }

        override fun onStart() {
            super.onStart()
            if (dialog is AlertDialog) {
                val a = dialog as AlertDialog
                a.getButton(Dialog.BUTTON_NEUTRAL)?.setOnClickListener(
                    OnDismissListener(a, Dialog.BUTTON_NEUTRAL)
                )
                a.getButton(Dialog.BUTTON_POSITIVE)?.setOnClickListener(
                    OnDismissListener(a, Dialog.BUTTON_POSITIVE)
                )
                a.getButton(Dialog.BUTTON_NEGATIVE)?.setOnClickListener(
                    OnDismissListener(a, Dialog.BUTTON_NEGATIVE)
                )
            }
            getCustomizablePreference().onStart()
        }

        override fun onStop() {
            super.onStop()
            getCustomizablePreference().onStop()
        }

        override fun onPause() {
            super.onPause()
            getCustomizablePreference().onPause()
        }

        override fun onResume() {
            super.onResume()
            getCustomizablePreference().onResume()
        }

        override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
            super.onPrepareDialogBuilder(builder)
            getCustomizablePreference().setFragment(this)
            getCustomizablePreference().onPrepareDialogBuilder(builder, this)
        }

        override fun onDialogClosed(positiveResult: Boolean) {
            getCustomizablePreference().onDialogClosed(positiveResult)
        }

        override fun onBindDialogView(view: View) {
            super.onBindDialogView(view)
            getCustomizablePreference().onBindDialogView(view)
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            super.onClick(dialog, which)
            @Suppress("UNCHECKED_CAST")
            (getCustomizablePreference() as CustomDialogPref<DialogInterface>).onClick(dialog as DialogInterface, which)
        }

        @NonNull
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            getCustomizablePreference().setFragment(this)
            val sub = getCustomizablePreference().onCreateDialog(savedInstanceState)
            return sub ?: super.onCreateDialog(savedInstanceState)
        }

        override fun onCreateDialogView(context: Context): View? {
            val v = getCustomizablePreference().onCreateDialogView(context)
            return v ?: super.onCreateDialogView(context)
        }
    }
}
