package com.icenerd.giftv.fragment.dialog

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.icenerd.giftv.MobileTVActivity
import com.icenerd.giftv.R
import java.util.*

class MobileTVNameDialog : DialogFragment() {

    var actionListener: ActionListener? = null
    interface ActionListener {
        fun onStartMobileTV(name: String)
    }

    private val tvName by lazy { view?.findViewById<EditText>(R.id.input_mobile_tv_name) }
    private val actionRandomizeName by lazy { view?.findViewById<Button>(R.id.action_generate_random_name) }
    private val actionCancel by lazy { view?.findViewById<Button>(R.id.action_cancel) }
    private val actionOK by lazy { view?.findViewById<Button>(R.id.action_ok) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_mobile_tv_name, container, false)
    }

    override fun onStart() {
        super.onStart()

        val sharedPref = context?.getSharedPreferences("giftv", MODE_PRIVATE)
        tvName?.setText(sharedPref?.getString(MobileTVActivity.EXTRA_NAME, generateRandomName()))
        tvName?.inputType = InputType.TYPE_CLASS_TEXT

        actionRandomizeName?.setOnClickListener {
            tvName?.setText(generateRandomName())
            tvName?.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        actionOK?.setOnClickListener {
            actionListener?.onStartMobileTV(tvName?.text.toString())
            dismiss()
        }

        actionCancel?.setOnClickListener { dismiss() }
    }

    private fun generateRandomName(): String {
        val random = Random()
        val onomatopoeia = resources.getStringArray(R.array.omp_array)

        val first = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val second = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val third = onomatopoeia[random.nextInt(onomatopoeia.size)]

        return "$first-$second-$third"
    }
}