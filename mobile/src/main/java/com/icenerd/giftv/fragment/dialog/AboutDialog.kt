package com.icenerd.giftv.fragment.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView

import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.R

class AboutDialog : DialogFragment() {
    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_about, container, false)
        val dialog = dialog
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        view.findViewById<TextView>(R.id.text_build).text = "${BuildConfig.BUILD_TIME} - ${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
        return view
    }
}