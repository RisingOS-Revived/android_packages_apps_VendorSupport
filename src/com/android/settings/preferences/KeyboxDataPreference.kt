package com.android.settings.preferences

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.settings.R

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

class KeyboxDataPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    companion object {
        private const val TAG = "KeyboxDataPref"
    }

    private var mFilePickerLauncher: ActivityResultLauncher<Intent>? = null

    init {
        layoutResource = R.layout.keybox_data_pref
    }

    fun setFilePickerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.mFilePickerLauncher = launcher
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val ctx = context
        val cr = ctx.contentResolver

        val title = holder.findViewById(R.id.title) as TextView
        val summary = holder.findViewById(R.id.summary) as TextView
        val deleteButton = holder.findViewById(R.id.delete_button) as ImageButton

        title.text = getTitle()

        val hasData = Settings.Secure.getString(cr, Settings.Secure.KEYBOX_DATA) != null

        summary.text = ctx.getString(
            if (hasData) R.string.keybox_data_loaded_summary else R.string.keybox_data_summary
        )

        deleteButton.visibility = if (hasData) View.VISIBLE else View.GONE
        deleteButton.isEnabled = hasData

        holder.itemView.setOnClickListener {
            mFilePickerLauncher?.let { launcher ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/xml", "application/xml"))
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                launcher.launch(intent)
            }
        }

        deleteButton.setOnClickListener {
            if (!callChangeListener(false)) return@setOnClickListener
            Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA, null)
            Toast.makeText(ctx, ctx.getString(R.string.keybox_toast_file_cleared), Toast.LENGTH_SHORT).show()
            notifyChanged()
        }
    }

    fun handleFileSelected(uri: Uri?) {
        val ctx = context
        val cr = ctx.contentResolver

        if (uri == null) {
            Toast.makeText(
                ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val type = cr.getType(uri)
        val isXmlMime = "text/xml" == type || "application/xml" == type
        val hasXmlExt = uri.path?.lowercase()?.endsWith(".xml") == true
        if (!isXmlMime && !hasXmlExt) {
            Toast.makeText(
                ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            cr.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val xmlContent = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        xmlContent.append(line).append('\n')
                    }

                    val xml = xmlContent.toString()
                    if (!validateXml(xml)) {
                        Toast.makeText(
                            ctx,
                            ctx.getString(R.string.keybox_toast_missing_data),
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    if (!callChangeListener(true)) return
                    Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA, xml)
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.keybox_toast_file_loaded),
                        Toast.LENGTH_SHORT
                    ).show()
                    notifyChanged()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read XML file", e)
            Toast.makeText(
                ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun validateXml(xml: String): Boolean {
        var hasEcdsaKey = false
        var hasRsaKey = false
        var hasEcdsaPrivKey = false
        var hasRsaPrivKey = false
        var ecdsaCertCount = 0
        var rsaCertCount = 0
        var numberOfKeyboxes = -1

        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xml))

            var currentAlg: String? = null

            var eventType = parser.next()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (val name = parser.name) {
                        "NumberOfKeyboxes" -> {
                            parser.next() // move to TEXT event
                            if (parser.eventType == XmlPullParser.TEXT) {
                                try {
                                    numberOfKeyboxes = parser.text.trim().toInt()
                                } catch (e: NumberFormatException) {
                                    numberOfKeyboxes = -1
                                }
                            }
                        }

                        "Key" -> {
                            currentAlg = parser.getAttributeValue(null, "algorithm")
                            when {
                                "ecdsa".equals(currentAlg, ignoreCase = true) -> hasEcdsaKey = true
                                "rsa".equals(currentAlg, ignoreCase = true) -> hasRsaKey = true
                                else -> currentAlg = null // unsupported key
                            }
                        }

                        "PrivateKey" -> {
                            val format = parser.getAttributeValue(null, "format")
                            if (!"pem".equals(format, ignoreCase = true)) {
                                Log.w(TAG, "Invalid or missing format for PrivateKey")
                                return false
                            }
                            when {
                                "ecdsa".equals(currentAlg, ignoreCase = true) -> hasEcdsaPrivKey = true
                                "rsa".equals(currentAlg, ignoreCase = true) -> hasRsaPrivKey = true
                            }
                        }

                        "Certificate" -> {
                            val format = parser.getAttributeValue(null, "format")
                            if (!"pem".equals(format, ignoreCase = true)) {
                                Log.w(TAG, "Invalid or missing format for Certificate")
                                return false
                            }

                            when {
                                "ecdsa".equals(currentAlg, ignoreCase = true) -> ecdsaCertCount++
                                "rsa".equals(currentAlg, ignoreCase = true) -> rsaCertCount++
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "Key" == parser.name) {
                    currentAlg = null
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML validation failed", e)
            return false
        }

        return numberOfKeyboxes == 1 &&
                hasEcdsaKey && hasEcdsaPrivKey && ecdsaCertCount >= 1 &&
                hasRsaKey && hasRsaPrivKey && rsaCertCount >= 1
    }
}
