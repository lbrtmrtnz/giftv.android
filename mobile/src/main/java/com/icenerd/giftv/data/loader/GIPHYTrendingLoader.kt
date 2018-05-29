package com.icenerd.giftv.data.loader


import android.content.*
import android.database.Cursor
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.net.GIPHYService
import com.icenerd.giphy.data.orm.GifORM

class GIPHYTrendingLoader(context: Context) : AsyncTaskLoader<Cursor>(context) {
    companion object {
        private val TAG = "GIPHYTrendingLoader"
    }
    private var mRemoteRequestSent = false
    private var mData: Cursor? = null
    private val mORM: GifORM
    private val mBroadMan: LocalBroadcastManager
    private val mObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GIPHYService.UPDATE_TRENDING) {
                onContentChanged()
            }
        }
    }
    init {
        val db = GIFTVDB(context)
        mORM = GifORM(db.readableDatabase)
        mBroadMan = LocalBroadcastManager.getInstance(context)
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

            if (!mRemoteRequestSent) {
                mRemoteRequestSent = true
                val intent = Intent(GIPHYService.ACTION_GET_TRENDING, null, context, GIPHYService::class.java)
                if (Build.VERSION.SDK_INT >= 21) {
                    GIPHYService.enqueueWork(context, intent)
                } else {
                    context?.startService(intent)
                }
            }
        }
        if (oldData != null && oldData !== data) releaseResources(oldData)
    }
    override fun onStartLoading() {
        if (mData != null) deliverResult(mData)

        val intentFilter = IntentFilter()
        intentFilter.addAction(GIPHYService.UPDATE_TRENDING)
        mBroadMan.registerReceiver(mObserver, intentFilter)

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

        mBroadMan.unregisterReceiver(mObserver)
    }
    override fun onCanceled(data: Cursor?) {
        super.onCanceled(data)
        if (data != null) releaseResources(data)
    }
    private fun releaseResources(data: Cursor) {
        data.close()
    }
}