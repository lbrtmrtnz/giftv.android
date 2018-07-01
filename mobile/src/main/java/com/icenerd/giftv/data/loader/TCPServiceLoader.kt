package com.icenerd.giftv.data.loader

import android.content.*
import android.database.Cursor
import android.support.v4.content.LocalBroadcastManager
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.orm.TCPServiceORM
import com.icenerd.giftv.net.DataService

class TCPServiceLoader(ctx: Context) : AsyncTaskLoader<Cursor>(ctx) {
    private var cursor: Cursor? = null
        set(value) {
            field?.isClosed?:field?.close()
            field = value
        }
    private val tcpServiceORM: TCPServiceORM by lazy { TCPServiceORM(GIFTVDB(context).readableDatabase) }
    private val localBroadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }
    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DataService.UPDATE_SERVICE_INFO) onContentChanged()
        }
    }

    override fun loadInBackground(): Cursor {
        return tcpServiceORM.getCursorAll(null)
    }
    override fun deliverResult(data: Cursor?) {
        if (isReset) {
            cursor = null
            return
        }

        val oldData = cursor
        cursor = data

        if (isStarted) {
            super.deliverResult(data)
        }
        if (oldData != null && oldData != data) cursor = null
    }
    override fun onStartLoading() {
        if (cursor != null) deliverResult(cursor)

        val intentFilter = IntentFilter()
        intentFilter.addAction(DataService.UPDATE_SERVICE_INFO)
        localBroadcastManager.registerReceiver(localBroadcastReceiver, intentFilter)

        if (takeContentChanged() || cursor == null) forceLoad()
    }
    override fun onStopLoading() {
        cancelLoad()
    }
    override fun onReset() {
        cancelLoad()
        cursor = null
        localBroadcastManager.unregisterReceiver(localBroadcastReceiver)
    }

    override fun onCanceled(data: Cursor?) {
        super.onCanceled(data)
        cursor = null
    }
}