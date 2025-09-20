/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
 *               2022 The LineageOS Project
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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import com.android.settings.R

import java.util.Collections
import java.util.HashSet
import java.util.LinkedList
import java.util.TreeSet

class PackageListAdapter(context: Context) : BaseAdapter(), Runnable {
    private val mPm: PackageManager = context.packageManager
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val mInstalledPackages: MutableList<PackageItem> = LinkedList()
    private var mExcludedPackages: Set<String> = HashSet()

    // Packages which don't have launcher icons, but which we want to show nevertheless
    companion object {
        private val PACKAGE_WHITELIST = arrayOf(
            "android",                          /* system server */
            "com.android.systemui",             /* system UI */
            "com.android.providers.downloads"   /* download provider */
        )
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val item = msg.obj as PackageItem
            val index = Collections.binarySearch(mInstalledPackages, item)
            if (index < 0) {
                mInstalledPackages.add(-index - 1, item)
            } else {
                mInstalledPackages[index].activityTitles.addAll(item.activityTitles)
            }
            notifyDataSetChanged()
        }
    }

    class PackageItem(
        val packageName: String,
        val title: CharSequence,
        val icon: Drawable
    ) : Comparable<PackageItem> {
        val activityTitles: TreeSet<CharSequence> = TreeSet()

        override fun compareTo(other: PackageItem): Int {
            val result = title.toString().compareTo(other.title.toString(), ignoreCase = true)
            return if (result != 0) result else packageName.compareTo(other.packageName)
        }
    }

    init {
        reloadList()
    }

    override fun getCount(): Int {
        synchronized(mInstalledPackages) {
            return mInstalledPackages.size
        }
    }

    override fun getItem(position: Int): PackageItem {
        synchronized(mInstalledPackages) {
            return mInstalledPackages[position]
        }
    }

    override fun getItemId(position: Int): Long {
        synchronized(mInstalledPackages) {
            // packageName is guaranteed to be unique in mInstalledPackages
            return mInstalledPackages[position].packageName.hashCode().toLong()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View
        if (convertView != null) {
            holder = convertView.tag as ViewHolder
            view = convertView
        } else {
            view = mInflater.inflate(R.layout.applist_preference_icon, null, false)
            holder = ViewHolder()
            view.tag = holder
            holder.title = view.findViewById(com.android.internal.R.id.title)
            holder.summary = view.findViewById(com.android.internal.R.id.summary)
            holder.icon = view.findViewById(com.android.internal.R.id.icon)
        }

        val applicationInfo = getItem(position)
        holder.title.text = applicationInfo.title
        holder.icon.setImageDrawable(applicationInfo.icon)

        var needSummary = applicationInfo.activityTitles.size > 0
        if (applicationInfo.activityTitles.size == 1) {
            if (TextUtils.equals(applicationInfo.title, applicationInfo.activityTitles.first())) {
                needSummary = false
            }
        }

        if (needSummary) {
            holder.summary.text = TextUtils.join(", ", applicationInfo.activityTitles)
            holder.summary.visibility = View.VISIBLE
        } else {
            holder.summary.visibility = View.GONE
        }

        return view
    }

    private fun reloadList() {
        mInstalledPackages.clear()
        Thread(this).start()
    }

    override fun run() {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val installedAppsInfo = mPm.queryIntentActivities(mainIntent, 0)

        for (info in installedAppsInfo) {
            val appInfo = info.activityInfo.applicationInfo
            if (mExcludedPackages.contains(appInfo.packageName)) {
                continue
            }

            val item = PackageItem(
                appInfo.packageName,
                appInfo.loadLabel(mPm),
                appInfo.loadIcon(mPm)
            )
            item.activityTitles.add(info.loadLabel(mPm))
            mHandler.obtainMessage(0, item).sendToTarget()
        }

        for (packageName in PACKAGE_WHITELIST) {
            if (mExcludedPackages.contains(packageName)) {
                continue
            }
            try {
                val appInfo = mPm.getApplicationInfo(packageName, 0)
                val item = PackageItem(
                    appInfo.packageName,
                    appInfo.loadLabel(mPm),
                    appInfo.loadIcon(mPm)
                )
                mHandler.obtainMessage(0, item).sendToTarget()
            } catch (ignored: PackageManager.NameNotFoundException) {
                // package not present, so nothing to add -> ignore it
            }
        }
    }

    fun setExcludedPackages(packages: HashSet<String>) {
        mExcludedPackages = packages
        reloadList()
    }

    private class ViewHolder {
        lateinit var title: TextView
        lateinit var summary: TextView
        lateinit var icon: ImageView
    }
}
