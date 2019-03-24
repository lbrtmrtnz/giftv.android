package com.icenerd.giftv

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.icenerd.data.Installation
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.model.StatusModel
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giftv.net.ClientMessageHandler
import com.icenerd.giftv.net.Server
import com.icenerd.giftv.net.ServerThread
import com.icenerd.giftv.ui.GifLoader
import com.icenerd.giphy.data.model.GifModel
import com.icenerd.giphy.data.orm.GifORM
import org.json.JSONException
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.util.*
import javax.crypto.spec.SecretKeySpec

class GifTVActivity : Activity() {
    companion object {
        private const val TAG = "GIFTV"
        const val BATCH_SIZE = 20
        const val EXTRA_NAME = "mobile_tv_name"
        const val SIGNAL_CHANGE_CHANNEL = "signal_change_channel"
        private const val SIZE_LIMIT_BYTES: Long = 4000000
    }
    private lateinit var gifImageView: GifImageView

    private var networkServer: Server? = null
    private var requestQueue: RequestQueue? = null
    private var gifLoader: GifLoader? = null
    private var listGIF: List<GifModel> = ArrayList()
    private var timer: Timer? = null
        set(value) {
            value?.cancel()
            value?.purge()
            field = value
        }
    private var nextPosition = 0

    private val currentSSID: String?
        get() {
            var ssid: String? = null
            val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connManager.activeNetworkInfo
            if (networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val connectionInfo = wifiManager.connectionInfo
                if (connectionInfo != null && !connectionInfo.ssid.isEmpty()) {
                    ssid = connectionInfo.ssid
                }
            }
            return ssid
        }

    private var localBroadcastManager: LocalBroadcastManager? = null
    private val localBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SIGNAL_CHANGE_CHANNEL) {
                if (BuildConfig.DEBUG) Log.d(TAG, SIGNAL_CHANGE_CHANNEL)
                val db = GIFTVDB(context).readableDatabase
                val statusORM = StatusORM(db)
                val updateStatus = statusORM.findWhere("1 = 1 ORDER BY ${StatusORM.COL_CREATED_ON} desc limit 1")
                ServerThread.STATE = updateStatus
                db.close()

                if (updateStatus == null) {
                    Log.w(TAG, "STATUS NOT FOUND")
                } else {
                    if (BuildConfig.DEBUG) {
                        try {
                            Log.d(TAG, "Status: ${updateStatus.getJSONObject().toString(1)}")
                        } catch (err: JSONException) {
                            err.printStackTrace()
                        }

                    }
                    when (updateStatus.channel_type) {
                        StatusORM.CHANNEL_GIPHY -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "GOT A GIPHY!")
                            taskGIFLoad()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_tv)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gifImageView = findViewById(R.id.gif) as GifImageView
    }
    override fun onStart() {
        super.onStart()
        val ssid = currentSSID
        if (ssid != null && !ssid.isEmpty()) {
            (findViewById(R.id.text_ssid) as TextView).text = ssid
        } else {
            (findViewById(R.id.text_ssid) as TextView).setText(R.string.tv_ssid_not_found)
        }
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        requestQueue = Volley.newRequestQueue(this)
        gifLoader = GifLoader(requestQueue!!, object : GifLoader.GifCache {
            private val mCache = LruCache<String, GifDrawable>(BATCH_SIZE)
            override fun putGif(url: String, gif: GifDrawable) {
                mCache.put(url, gif)
            }
            override fun getGif(url: String): GifDrawable? {
                return mCache.get(url)
            }
        })
        val nsdMan = getSystemService(Context.NSD_SERVICE) as NsdManager
        val nsName = intent.getStringExtra(Server.NAME)
        var nsType: String? = intent.getStringExtra(Server.TYPE)
        if (nsName == null || nsName.isEmpty()) {
            finish()
        } else {
            if (nsType == null || nsType.isEmpty()) {
                nsType = getString(R.string.network_service_type)
            }

            networkServer = Server(
                    nsdMan,
                    nsName,
                    nsType!!
            )
            networkServer?.handler = ClientMessageHandler(this)
        }

        findViewById<TextView>(R.id.text_tv_name).text = nsName
    }
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SIGNAL_CHANGE_CHANNEL)
        localBroadcastManager!!.registerReceiver(localBroadcastReceiver, intentFilter)
        if (networkServer != null) {
            ServerThread.STATE = StatusModel(Installation.getUUID(this)!!)
            networkServer!!.startServer(SecretKeySpec(Base64.decode(getString(R.string.network_service_secret), Base64.NO_CLOSE or Base64.NO_WRAP), "AES"))
        }
    }
    override fun onPause() {
        super.onPause()
        timer = null
        localBroadcastManager?.unregisterReceiver(localBroadcastReceiver)
        networkServer?.stopServer()
    }

    override fun onStop() {
        super.onStop()
        networkServer = null
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
    private fun taskGIFLoad() {
        val db = GIFTVDB(this@GifTVActivity).readableDatabase
        val orm = GifORM(db)
        listGIF = orm.tv_gif_list()
        if (nextPosition >= listGIF.size) nextPosition = 0
        if (listGIF.size == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No Gifs found!")
            findViewById<View>(R.id.frame_gif).visibility = View.INVISIBLE
            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()
                timer = null
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, listGIF.size.toString() + " Gifs found!")
            findViewById<View>(R.id.frame_gif).visibility = View.VISIBLE
            if (timer == null) {
                timer = Timer()
                timer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread { gifNext() }
                    }
                }, 0, 3000)
            }
        }
    }
    private fun gifNext() {
        if (nextPosition >= listGIF.size) {
            nextPosition = 0
            taskGIFLoad()
        } else if (listGIF.isEmpty()) {
            val model = listGIF[nextPosition]
            val container = gifLoader!![if (model.size < SIZE_LIMIT_BYTES) model.original?:"" else model.downsized?:"", object : GifLoader.GifListener {
                override fun onResponse(response: GifLoader.GifContainer, isImmediate: Boolean) {
                    gifLoad(response)
                }
                override fun onErrorResponse(error: VolleyError) {}
            }]
            gifLoad(container)
            // pre load the next 5
            var i = nextPosition + 1
            while (i < nextPosition + BATCH_SIZE / 2 && i < listGIF.size) {
                gifLoader!![if (model.size < SIZE_LIMIT_BYTES) model.original?:"" else model.downsized?:"", null]
                i++
            }
        }
        nextPosition++
    }

    private fun gifLoad(container: GifLoader.GifContainer) {
        val drawable = container.gif
        if (drawable == null) {
            gifImageView.visibility = View.INVISIBLE
        } else {
            gifImageView.setImageDrawable(drawable)
            gifImageView.visibility = View.VISIBLE
            if (!drawable.isPlaying) drawable.start()
        }
    }
}
