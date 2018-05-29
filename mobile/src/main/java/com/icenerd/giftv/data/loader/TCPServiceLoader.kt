package com.icenerd.giftv.data.loader

import android.content.*
import android.database.Cursor
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.orm.TCPServiceORM
import com.icenerd.giftv.net.DataService

class TCPServiceLoader(context: Context) : AsyncTaskLoader<Cursor>(context) {
    companion object {
        private val TAG = "TCPServiceLoader"
    }
    private val mORM: TCPServiceORM
    private val mBroadcastManager: LocalBroadcastManager
    private val mObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(DataService.UPDATE_SERVICE_INFO)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "TCP Service Info updated, calling onContentChanged()")
                onContentChanged()
            }
        }
    }
    private var mData: Cursor? = null
    init {
        mORM = TCPServiceORM(GIFTVDB(context).readableDatabase)
        mBroadcastManager = LocalBroadcastManager.getInstance(context)
    }
    override fun loadInBackground(): Cursor {
        return mORM.getCursorAll(null)
    }
    override fun deliverResult(data: Cursor?) {
        if (isReset) {
            if (data != null) releaseResources(data)
            return
        }

        val oldData = mData
        mData = data

        if (isStarted) {
            super.deliverResult(data)
        }
        if (oldData != null && oldData !== data) releaseResources(oldData)
    }
    override fun onStartLoading() {
        if (mData != null) deliverResult(mData)

        val intentFilter = IntentFilter()
        intentFilter.addAction(DataService.UPDATE_SERVICE_INFO)
        mBroadcastManager.registerReceiver(mObserver, intentFilter)

        if (takeContentChanged() || mData == null) forceLoad()
    }
    override fun onStopLoading() {
        cancelLoad()
    }
    override fun onReset() {
        onStopLoading()

        if (mData != null) {
            releaseResources(mData!!)
            mData = null
        }

        mBroadcastManager.unregisterReceiver(mObserver)
    }
    override fun onCanceled(data: Cursor?) {
        super.onCanceled(data)
        if (data != null) releaseResources(data)
    }
    private fun releaseResources(data: Cursor) {
        data.close()
    }
}