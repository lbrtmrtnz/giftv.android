package com.icenerd.giftv.fragment.dialog

import android.app.LoaderManager
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.database.Cursor
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icenerd.data.Installation
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.R
import com.icenerd.giftv.adapter.TCPServiceAdapter
import com.icenerd.giftv.data.loader.TCPServiceLoader
import com.icenerd.giftv.data.model.StatusModel
import com.icenerd.giftv.data.model.TCPServiceModel
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giftv.net.DataService
import com.icenerd.giftv.net.NsdHelper
import com.icenerd.giphy.data.orm.GifORM
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class SendChannelDialog : AppCompatDialogFragment(), LoaderManager.LoaderCallbacks<Cursor>, NsdHelper.NsdListener, TCPServiceAdapter.OnItemClickListener {
    companion object {
        private const val TAG = "SendChannelDialog"
        private val LOADER_ID = Random().nextInt()
    }
    var actionListener: ActionListener? = null
    interface ActionListener {
        fun onTVSelected(model: TCPServiceModel)
    }
    private val nsdHelper: NsdHelper by lazy {
        val nsdMan = activity?.getSystemService(Context.NSD_SERVICE) as NsdManager
        val nsType = getString(R.string.network_service_type)
        NsdHelper(nsdMan, nsType)
    }
    private var fragmentView: View? = null
    private var progressBar: ProgressBar? = null
    private var recyclerView: RecyclerView? = null
    private val adapter: TCPServiceAdapter by lazy { TCPServiceAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.dialog_send_channel, container, false)
        progressBar = fragmentView!!.findViewById(R.id.progress_bar) as ProgressBar
        recyclerView = fragmentView!!.findViewById(R.id.recyclerview) as RecyclerView
        return fragmentView
    }

    override fun onStart() {
        super.onStart()
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = LinearLayoutManager(activity)
        recyclerView!!.itemAnimator = DefaultItemAnimator()
        recyclerView!!.adapter = adapter
        fragmentView!!.findViewById<View>(R.id.action_cancel).setOnClickListener { dismiss() }
        activity?.loaderManager?.initLoader(LOADER_ID, Bundle(), this@SendChannelDialog)?.forceLoad()
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(DataService.ACTION_LOST, null, activity, DataService::class.java)
        if (Build.VERSION.SDK_INT >= 21) {
            DataService.enqueueWork(activity!!, intent)
        } else {
            activity?.startService(intent)
        }
        nsdHelper.startDiscovering(this)
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovering()
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? {
        return if (id == LOADER_ID) {
            TCPServiceLoader(activity!!)
        } else null
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        if (data.count > 0) {
            progressBar!!.visibility = View.GONE
        } else {
            progressBar!!.visibility = View.VISIBLE
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.changeCursor(null)
    }

    override fun nsdServiceResolved(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) {
            if(BuildConfig.DEBUG) Log.d(TAG, "nsdServiceResolved(serviceInfo == null)")
        } else {
            val intent = Intent(DataService.ACTION_IDENTIFY, null, activity, DataService::class.java)
            intent.putExtra("name", serviceInfo.serviceName)
            intent.putExtra("host", serviceInfo.host.hostAddress)
            intent.putExtra("port", serviceInfo.port)
            try {
                val data = JSONObject()
                data.put("host", serviceInfo.host.hostAddress)
                data.put("port", serviceInfo.port)
                intent.putExtra("data", data.toString())
            } catch (err: JSONException) {
                err.printStackTrace()
            }

            if (Build.VERSION.SDK_INT >= 21) {
                DataService.enqueueWork(activity!!, intent)
            } else {
                activity?.startService(intent)
            }
        }
    }

    override fun nsdServiceLost(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo == null) {
            if(BuildConfig.DEBUG) Log.d(TAG, "nsdServiceLost(serviceInfo == null)")
        } else {
            val intent = Intent(DataService.ACTION_LOST, null, activity, DataService::class.java)
            intent.putExtra("name", serviceInfo.serviceName)
            if (Build.VERSION.SDK_INT >= 21) {
                DataService.enqueueWork(activity!!, intent)
            } else {
                activity?.startService(intent)
            }
        }
    }

    override fun onItemClick(model: TCPServiceModel) {
        try {
            val statusModel = StatusModel(Installation.getUUID(context!!)!!)
            statusModel.channel_type = StatusORM.CHANNEL_GIPHY
            statusModel.channel_id = 1
            val jsonGifs = JSONArray(arguments?.getString(GifORM.TABLE))

            val json = JSONObject()
            json.put(StatusORM.TABLE, statusModel.getJSONObject())
            json.put(GifORM.TABLE, jsonGifs)
            json.put("terms", arguments?.getString("terms"))

            val intent = Intent(DataService.ACTION_SEND, null, context, DataService::class.java)
            intent.putExtra("host", model.host)
            intent.putExtra("port", model.port)
            intent.putExtra("data", json.toString())
            if (BuildConfig.DEBUG) Log.d(javaClass.simpleName, intent.extras!!.toString())
            if (Build.VERSION.SDK_INT >= 21) {
                DataService.enqueueWork(activity!!, intent)
            } else {
                activity?.startService(intent)
            }
        } catch (err: JSONException) {
            if(BuildConfig.DEBUG) err.printStackTrace()
        } finally {
            if (actionListener != null) actionListener!!.onTVSelected(model)
            dismiss()
        }
    }
}