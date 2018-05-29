package com.icenerd.giftv.fragment.dialog

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import com.icenerd.giftv.MobileTVActivity
import com.icenerd.giftv.R
import java.util.*

class MobileTVNameDialog : DialogFragment() {
    private var mListener: OnStartMobileTVListener? = null
    private var mMobileTVName: EditText? = null
    private var mRNGName: Button? = null
    private var mOK: Button? = null
    interface OnStartMobileTVListener {
        fun onStartMobileTV(name: String)
    }
    fun setOnStartMobileTVListener(listener: OnStartMobileTVListener) {
        mListener = listener
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_mobile_tv_name, container, false)
        val dialog = dialog
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        mMobileTVName = view.findViewById(R.id.input_mobile_tv_name) as EditText
        mRNGName = view.findViewById(R.id.action_generate_random_name) as Button
        mOK = view.findViewById(R.id.action_ok) as Button

        val sharedPref = activity?.getSharedPreferences("giftv", MODE_PRIVATE)
        mMobileTVName!!.setText(sharedPref?.getString(MobileTVActivity.EXTRA_NAME, generateRandomName()))
        mMobileTVName!!.inputType = InputType.TYPE_CLASS_TEXT

        mRNGName!!.setOnClickListener {
            mMobileTVName!!.setText(generateRandomName())
            mMobileTVName!!.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        mOK!!.setOnClickListener {
            if (mListener != null) mListener!!.onStartMobileTV(mMobileTVName!!.text.toString())
            dismiss()
        }

        view.findViewById<View>(R.id.action_cancel).setOnClickListener { dismiss() }

        return view
    }
    private fun generateRandomName(): String {
        val random = Random()
        val onomatopoeia = resources.getStringArray(R.array.omp_array)

        val first = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val second = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val third = onomatopoeia[random.nextInt(onomatopoeia.size)]

        return String.format(Locale.US, "%s-%s-%s", first, second, third)
    }
}