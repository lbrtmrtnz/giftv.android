package com.icenerd.giftv.data.loader

import android.content.*
import android.database.Cursor
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.net.GIPHYService
import com.icenerd.giphy.data.orm.GifORM

class GIPHYSearchLoader(ctx: Context, private var currentTerms: String) : AsyncTaskLoader<Cursor>(ctx) {
    private var requestSent = false

    private var cursor: Cursor? = null
        set(value) {
            field?.isClosed?:field?.close()
            field = value
        }
    private val gifORM: GifORM by lazy { GifORM(GIFTVDB(context).readableDatabase) }
    private val localBroadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }
    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == GIPHYService.UPDATE_SEARCH) onContentChanged()
        }
    }

    override fun loadInBackground(): Cursor {
        return gifORM.getSearch(currentTerms)
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

            if (!requestSent) {
                requestSent = true
                val intent = Intent(GIPHYService.ACTION_GET_SEARCH, null, context, GIPHYService::class.java)
                intent.putExtra("terms", currentTerms)
                if (Build.VERSION.SDK_INT >= 21) {
                    GIPHYService.enqueueWork(context, intent)
                } else {
                    context?.startService(intent)
                }
            }
        }
        if (oldData != null && oldData != data) cursor = null
    }

    override fun onStartLoading() {
        if (cursor != null) deliverResult(cursor)

        val intentFilter = IntentFilter()
        intentFilter.addAction(GIPHYService.UPDATE_SEARCH)
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