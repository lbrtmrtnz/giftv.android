package com.icenerd.giftv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.view.View
import android.view.WindowManager
import android.widget.TextView
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

class MobileTVActivity : AppCompatActivity() {
    companion object {
        private val TAG = "MobileTV"

        val BATCH_SIZE = 20

        val EXTRA_NAME = "mobile_tv_name"
        val SIGNAL_CHANGE_CHANNEL = "signal_change_channel"

        private val SIZE_LIMIT_BYTES: Long = 4000000
    }

    private var mNetworkService: Server? = null
    private var mGifImageView: GifImageView? = null
    private var mRequestQueue: RequestQueue? = null
    private var mGifLoader: GifLoader? = null
    private var mBroadcastManager: LocalBroadcastManager? = null
    private val mObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(SIGNAL_CHANGE_CHANNEL)) {
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
                            Log.d(TAG, "Status: " + updateStatus.getJSONObject().toString())
                        } catch (err: JSONException) {
                            err.printStackTrace()
                        }

                    }
                    when (updateStatus.channel_type) {
                        StatusORM.CHANNEL_GIPHY -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "GOT A GIPHY!")
                            TASK_load_gif()
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    private var mGifs: List<GifModel> = ArrayList()
    private var mTimer: Timer? = null
    private var mNextPosition = 0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_tv)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mGifImageView = findViewById(R.id.gif) as GifImageView

        val ssid = currentSSID
        if (ssid != null && !ssid.isEmpty()) {
            (findViewById(R.id.text_ssid) as TextView).text = ssid
        } else {
            (findViewById(R.id.text_ssid) as TextView).setText(R.string.tv_ssid_not_found)
        }
    }

    override fun onStart() {
        super.onStart()
        mBroadcastManager = LocalBroadcastManager.getInstance(this)
        mRequestQueue = Volley.newRequestQueue(this)
        mGifLoader = GifLoader(mRequestQueue!!, object : GifLoader.GifCache {
            private val mCache = LruCache<String, GifDrawable>(BATCH_SIZE)
            override fun putGif(url: String, gif: GifDrawable) {
                mCache.put(url, gif)
            }

            override fun getGif(url: String): GifDrawable {
                return mCache.get(url)
            }
        })

        val nsdMan = getSystemService(Context.NSD_SERVICE) as NsdManager
        val nsName = intent.getStringExtra(Server.NAME)
        var nsType: String? = intent.getStringExtra(Server.TYPE)

        if (nsName == null || nsName.isEmpty()) {
            finish()
        } else {
            (findViewById(R.id.text_tv_name) as TextView).text = nsName
            if (nsType == null || nsType.isEmpty()) nsType = getString(R.string.network_service_type)

            mNetworkService = Server(
                    nsdMan,
                    nsName,
                    nsType!!
            )
            mNetworkService.handler = ClientMessageHandler(this)
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(SIGNAL_CHANGE_CHANNEL)
        mBroadcastManager!!.registerReceiver(mObserver, intentFilter)
        if (mNetworkService != null) {
            ServerThread.STATE = StatusModel(Installation.getUUID(this)!!)
            mNetworkService!!.startServer(SecretKeySpec(Base64.decode(getString(R.string.network_service_secret), Base64.NO_CLOSE or Base64.NO_WRAP), "AES"))
        }
    }

    override fun onPause() {
        super.onPause()
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer!!.purge()
            mTimer = null
        }
        mBroadcastManager!!.unregisterReceiver(mObserver)
        if (mNetworkService != null) {
            mNetworkService!!.stopServer()
        }
    }

    override fun onStop() {
        super.onStop()
        //mNetworkService!!.setHandler(null)
        mNetworkService = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= 19) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else if (Build.VERSION.SDK_INT >= 16) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }
        }
    }

    private fun TASK_load_gif() {
        val db = GIFTVDB(this@MobileTVActivity).readableDatabase
        val orm = GifORM(db)
        mGifs = orm.tv_gif_list()
        if (mNextPosition >= mGifs.size) mNextPosition = 0

        if (mGifs.size == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No Gifs found!")
            findViewById<View>(R.id.frame_gif).visibility = View.INVISIBLE
            if (mTimer != null) {
                mTimer!!.cancel()
                mTimer!!.purge()
                mTimer = null
            }
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, mGifs.size.toString() + " Gifs found!")
            findViewById<View>(R.id.frame_gif).visibility = View.VISIBLE
            if (mTimer == null) {
                mTimer = Timer()
                mTimer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread { gif_rotation() }
                    }
                }, 0, 3000)
            }
        }
    }

    private fun gif_rotation() {
        if (mNextPosition >= mGifs.size) {
            mNextPosition = 0
            TASK_load_gif()
        } else if (mGifs.size > 0) {
            val model = mGifs[mNextPosition]
            val container = mGifLoader!![if (model.size < SIZE_LIMIT_BYTES) model.original!! else model.downsized!!, object : GifLoader.GifListener {
                override fun onResponse(response: GifLoader.GifContainer, isImmediate: Boolean) {
                    LOAD_gif(response)
                }

                override fun onErrorResponse(error: VolleyError) {}
            }]
            LOAD_gif(container)

            var i = mNextPosition + 1
            while (i < mNextPosition + BATCH_SIZE / 2 && i < mGifs.size) {
                val preModel = mGifs[i]
                mGifLoader!![if (preModel.size < SIZE_LIMIT_BYTES) preModel.original!! else preModel.downsized!!, null]
                i++
            }
        }
        mNextPosition++
    }

    private fun LOAD_gif(container: GifLoader.GifContainer) {
        val drawable = container.gif
        if (drawable == null) {
            mGifImageView!!.visibility = View.INVISIBLE
        } else {
            mGifImageView!!.setImageDrawable(drawable)
            mGifImageView!!.visibility = View.VISIBLE
            if (!drawable.isPlaying()) drawable.start()
        }
    }
}