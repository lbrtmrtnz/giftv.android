package com.icenerd.giftv.net


import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Message
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.MobileTVActivity
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.model.StatusModel
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giphy.data.model.GifModel
import com.icenerd.giphy.data.orm.GifORM
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class ClientMessageHandler(context: Context) : Handler() {
    private val mContext: WeakReference<Context>
    init {
        mContext = WeakReference(context)
    }
    override fun handleMessage(msg: Message) {
        val bundle = msg.data
        if (bundle.containsKey("json")) { // command to change state
            try {
                val json = JSONObject(bundle.getString("json"))
                handleJSON(json)
            } catch (err: JSONException) {
                err.printStackTrace()
            }
        }
    }
    @Throws(JSONException::class)
    private fun handleJSON(json: JSONObject) {
        var bSignal = false
        val context = mContext.get()

        var statusModel: StatusModel? = null
        if (json.has(StatusORM.TABLE)) {
            var db: SQLiteDatabase? = null
            try {
                db = GIFTVDB(context!!).writableDatabase
                statusModel = StatusModel(json.getJSONObject(StatusORM.TABLE))
                val orm = StatusORM(db)
                if (orm.save(statusModel)) {
                    db!!.close()
                    bSignal = true
                }
            } catch (err: JSONException) {
                err.printStackTrace()
            } finally {
                if (db != null) db.close()
            }
        }

        if (statusModel != null && json.has(GifORM.TABLE)) { // this has gif data, save it
            if (BuildConfig.DEBUG) Log.d(TAG, "Saving Gifs")
            var db: SQLiteDatabase? = null
            try {
                db = GIFTVDB(context!!).writableDatabase
                val current_time = System.currentTimeMillis()
                val terms = if (json.isNull("terms")) null else json.getString("terms")
                val jsonGifs = json.getJSONArray(GifORM.TABLE)
                val orm = GifORM(db)
                for (i in 0 until jsonGifs.length()) {
                    val model = GifModel(jsonGifs.getJSONObject(i))
                    orm.save(model)
                    orm.tv_log(current_time, terms?:"", model)
                    if (BuildConfig.DEBUG) Log.d(TAG, "gif added to television log")
                }
            } catch (err: JSONException) {
                err.printStackTrace()
            } finally {
                if (db != null) db.close()
            }
        }

        if (bSignal) LocalBroadcastManager.getInstance(context!!).sendBroadcast(Intent(MobileTVActivity.SIGNAL_CHANGE_CHANNEL))
    }

}