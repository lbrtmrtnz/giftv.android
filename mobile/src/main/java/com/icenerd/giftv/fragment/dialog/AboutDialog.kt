package com.icenerd.giftv.fragment.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment

import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.R

class AboutDialog : DialogFragment() {

    private val text_build by lazy { view?.findViewById<TextView>(R.id.text_build) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_about, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        text_build?.text = "${BuildConfig.BUILD_TIME} - ${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
    }
}