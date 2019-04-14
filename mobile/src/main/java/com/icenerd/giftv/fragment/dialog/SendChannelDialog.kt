package com.icenerd.giftv.fragment.dialog

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.icenerd.data.Installation
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.R
import com.icenerd.giftv.adapter.TCPServiceAdapter
import com.icenerd.giftv.data.model.StatusModel
import com.icenerd.giftv.data.model.TCPServiceModel
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giftv.net.DataService
import com.icenerd.giftv.net.NsdHelper
import com.icenerd.giphy.data.orm.GifORM
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class SendChannelDialog : AppCompatDialogFragment(), NsdHelper.NsdListener, TCPServiceAdapter.ActionListener {
    companion object {
        private const val TAG = "SendChannelDialog"
    }
    var actionListener: ActionListener? = null
    interface ActionListener {
        fun onTVSelected(model: TCPServiceModel)
    }
    private val nsdHelper: NsdHelper by lazy {
        val nsdMan = context!!.getSystemService(Context.NSD_SERVICE) as NsdManager
        val nsType = getString(R.string.network_service_type)
        NsdHelper(nsdMan, nsType)
    }

    private val mProgressBar by lazy { view!!.findViewById<ProgressBar>(R.id.progress_bar) }
    private val mRecyclerView by lazy { view!!.findViewById<RecyclerView>(R.id.recyclerview) }
    private val mActionCancel by lazy { view!!.findViewById<View>(R.id.action_cancel) }
    private val mTCPServiceAdapter by lazy { TCPServiceAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_send_channel, container, false)
    }

    override fun onStart() {
        super.onStart()
        mActionCancel.setOnClickListener { dismiss() }
        mRecyclerView.apply {
            adapter = mTCPServiceAdapter
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
        }
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(DataService.ACTION_LOST, null, context, DataService::class.java)
        if (Build.VERSION.SDK_INT >= 21) {
            context?.let { DataService.enqueueWork(it, intent) }
        } else {
            context?.startService(intent)
        }
        nsdHelper.startDiscovering(this)
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovering()
    }

    override fun nsdServiceResolved(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) {
            if(BuildConfig.DEBUG) Log.d(TAG, "nsdServiceResolved(serviceInfo == null)")
        } else {

            val intent = Intent(DataService.ACTION_IDENTIFY, null, activity, DataService::class.java).apply {
                putExtra("name", serviceInfo.serviceName)
                putExtra("host", serviceInfo.host.hostAddress)
                putExtra("port", serviceInfo.port)
            }

            try {
                val data = JSONObject().apply {
                    put("host", serviceInfo.host.hostAddress)
                    put("port", serviceInfo.port)
                }
                intent.putExtra("data", data.toString())
            } catch (err: JSONException) {
                if(BuildConfig.DEBUG) err.printStackTrace()
            }

            if (Build.VERSION.SDK_INT >= 21) {
                context?.let { DataService.enqueueWork(it, intent) }
            } else {
                context?.startService(intent)
            }
        }
    }

    override fun nsdServiceLost(serviceInfo: NsdServiceInfo?) {
        val serviceName = serviceInfo?.serviceName
        if(BuildConfig.DEBUG) Log.d(TAG, "nsdServiceLost($serviceName)")

        if (!serviceName.isNullOrEmpty()) {
            val intent = Intent(DataService.ACTION_LOST, null, context, DataService::class.java)
            intent.putExtra("name", serviceName)
            if (Build.VERSION.SDK_INT >= 21) {
                context?.let { DataService.enqueueWork(it, intent) }
            } else {
                context?.startService(intent)
            }
        }
    }

    override fun onItemClick(model: TCPServiceModel) {
        try {
            val statusModel = StatusModel(Installation.getUUID(context!!)!!)
            statusModel.channel_type = StatusORM.CHANNEL_GIPHY
            statusModel.channel_id = 1
            val jsonGifs = JSONArray(arguments?.getString(GifORM.TABLE))
            val json = JSONObject().apply {
                put(StatusORM.TABLE, statusModel.getJSONObject())
                put(GifORM.TABLE, jsonGifs)
                put("terms", arguments?.getString("terms"))
            }
            val intent = Intent(DataService.ACTION_SEND, null, context, DataService::class.java).apply {
                putExtra("host", model.host)
                putExtra("port", model.port)
                putExtra("data", json.toString())
            }
            if (BuildConfig.DEBUG) Log.d(TAG, intent.extras?.toString())
            if (Build.VERSION.SDK_INT >= 21) {
                context?.let { DataService.enqueueWork(it, intent) }
            } else {
                context?.startService(intent)
            }
        } catch (err: JSONException) {
            if(BuildConfig.DEBUG) err.printStackTrace()
        } finally {
            actionListener?.onTVSelected(model)
            dismiss()
        }
    }
}