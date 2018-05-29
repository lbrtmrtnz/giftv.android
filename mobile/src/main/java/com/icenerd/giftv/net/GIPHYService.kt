package com.icenerd.giftv.net

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager
import com.icenerd.giftv.R
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giphy.data.model.GifModel
import com.icenerd.giphy.data.orm.GifORM
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*


class GIPHYService : JobIntentService() {
    companion object {
        private val TAG = "GIPHYService"

        val ACTION_GET_TRENDING = "action_get_trending"
        val ACTION_GET_SEARCH = "action_get_search"

        val PAGESIZE_TRENDING = 24
        val PAGESIZE_SEARCH = 24

        val UPDATE_TRENDING = "update_trending"
        val UPDATE_SEARCH = "update_search"

        private val JOB_ID = Random().nextInt()
        @JvmStatic fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, GIPHYService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action.equals(ACTION_GET_TRENDING)) {
            if (API_get_trending()) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(UPDATE_TRENDING))
            }
        }
        if (intent.action.equals(ACTION_GET_SEARCH)) {
            if (API_get_search(intent.getStringExtra("terms"), intent.getIntExtra("offset", 0))) {
                val updateSearch = Intent(UPDATE_SEARCH)
                updateSearch.putExtra("terms", intent.getStringExtra("terms"))
                updateSearch.putExtra("offset", intent.getIntExtra("offset", 0))
                LocalBroadcastManager.getInstance(this).sendBroadcast(updateSearch)
            }
        }
    }

    private fun API_get_trending(): Boolean {
        var bReturn = false
        val url = "${getString(R.string.api_giphy)}/trending?api_key=${resources.getString(R.string.api_giphy_key)}&limit=${PAGESIZE_TRENDING}"
        val httpClient = OkHttpClient()
        val request = Request.Builder()
                .url(url)
                .build()

        var json: JSONObject? = null
        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw IOException(String.format("Unexpected code: %s", response))
            json = JSONObject(response.body()!!.string())
        } catch (err: JSONException) {
            err.printStackTrace()
        } catch (err: IOException) {
            err.printStackTrace()
        }

        if (json != null) {
            val db = GIFTVDB(this).writableDatabase
            try {
                val jsonData = json.getJSONArray("data")
                val orm = GifORM(db)
                orm.deleteWhere("1 = 1") // todo: build something else to manage gif data?
                for (i in 0 until jsonData.length()) {
                    val jsonGif = jsonData.getJSONObject(i)
                    val model = GifModel(jsonGif)
                    orm.save(model)
                }
                bReturn = true
            } catch (err: JSONException) {
                err.printStackTrace()
            } finally {
                db.close()
            }
        }

        return bReturn
    }

    private fun API_get_search(terms: String, offset: Int): Boolean {
        var bReturn = false
        val endPoint = String.format("/search?api_key=%s&q=%s&limit=%s&offset=%s", resources.getString(R.string.api_giphy_key), terms, PAGESIZE_SEARCH, offset)
        val httpClient = OkHttpClient()
        val request = Request.Builder()
                .url(String.format("%s%s", getString(R.string.api_giphy), endPoint))
                .build()

        var json: JSONObject? = null
        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw IOException(String.format("Unexpected code: %s", response))
            json = JSONObject(response.body()!!.string())
        } catch (err: JSONException) {
            err.printStackTrace()
        } catch (err: IOException) {
            err.printStackTrace()
        }

        if (json != null) {
            val db = GIFTVDB(this).writableDatabase
            try {
                val jsonData = json.getJSONArray("data")
                val orm = GifORM(db)
                for (i in 0 until jsonData.length()) {
                    val jsonGif = jsonData.getJSONObject(i)
                    val model = GifModel(jsonGif)
                    orm.save(model)
                    orm.linkSearchGif(terms, offset + i, model._id?:"")
                }
                orm.deleteSearchLinksAfter(offset + jsonData.length())
                bReturn = true
            } catch (err: JSONException) {
                err.printStackTrace()
            } finally {
                db.close()
            }
        }

        return bReturn
    }
}