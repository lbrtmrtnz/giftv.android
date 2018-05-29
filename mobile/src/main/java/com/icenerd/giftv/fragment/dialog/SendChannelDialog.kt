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
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
        private val LOADER_ID = Random().nextInt()
    }
    private var mNsDiscoverer: NsdHelper? = null

    private var mFragmentView: View? = null
    private var mProgressBar: ProgressBar? = null
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: TCPServiceAdapter? = null

    private var mListener: OnTVSelectedListener? = null

    interface OnTVSelectedListener {
        fun onTVSelected(model: TCPServiceModel)
    }

    fun setOnTVSelectedListener(listener: OnTVSelectedListener) {
        mListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mFragmentView = inflater.inflate(R.layout.dialog_send_channel, container, false)

        mProgressBar = mFragmentView!!.findViewById(R.id.progress_bar) as ProgressBar

        mRecyclerView = mFragmentView!!.findViewById(R.id.recyclerview) as RecyclerView
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.layoutManager = LinearLayoutManager(activity)
        mRecyclerView!!.itemAnimator = DefaultItemAnimator()

        mAdapter = TCPServiceAdapter(this)
        mRecyclerView!!.adapter = mAdapter
        activity?.loaderManager?.initLoader(LOADER_ID, Bundle(), this@SendChannelDialog)?.forceLoad()

        mFragmentView!!.findViewById<View>(R.id.action_cancel).setOnClickListener { dismiss() }

        return mFragmentView
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(DataService.ACTION_LOST, null, activity, DataService::class.java)
        //intent.putExtra("name", null );
        if (Build.VERSION.SDK_INT >= 21) {
            DataService.enqueueWork(activity!!, intent)
        } else {
            activity?.startService(intent)
        }

        val nsdMan = activity?.getSystemService(Context.NSD_SERVICE) as NsdManager
        val nsType = getString(R.string.network_service_type)
        mNsDiscoverer = NsdHelper(nsdMan, nsType)
        mNsDiscoverer!!.startDiscovering(this)
    }

    override fun onPause() {
        super.onPause()
        if (mNsDiscoverer != null) {
            mNsDiscoverer!!.stopDiscovering()
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? {
        return if (id == LOADER_ID) {
            TCPServiceLoader(activity!!)
        } else null
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        mAdapter!!.swapCursor(data)
        if (data.count > 0) {
            mProgressBar!!.visibility = View.GONE
        } else {
            mProgressBar!!.visibility = View.VISIBLE
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mAdapter!!.changeCursor(null)
    }

    override fun NsdServiceResolved(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo != null) {
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

    override fun NsdServiceLost(serviceInfo: NsdServiceInfo?) {
        if (serviceInfo != null) {
            val intent = Intent(DataService.ACTION_LOST, null, activity, DataService::class.java)
            intent.putExtra("name", serviceInfo.serviceName)
            if (Build.VERSION.SDK_INT >= 21) {
                DataService.enqueueWork(activity!!, intent)
            } else {
                activity?.startService(intent)
            }
        } else {
            // used to be where I would signal a dump of the current data collected
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
            err.printStackTrace()
        } finally {
            if (mListener != null) mListener!!.onTVSelected(model)
            dismiss()
        }
    }
}