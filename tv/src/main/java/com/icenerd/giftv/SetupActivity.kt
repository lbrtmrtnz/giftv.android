package com.icenerd.giftv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.icenerd.giftv.net.Server
import java.util.*

class SetupActivity : Activity() {
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
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        val sharedPref = getSharedPreferences("giftv", Context.MODE_PRIVATE)
        (findViewById(R.id.input_name) as TextView).text = sharedPref.getString(GifTVActivity.EXTRA_NAME, generateRandomName())
        (findViewById(R.id.text_ssid) as TextView).text = currentSSID
        (findViewById(R.id.action_generate_random_name) as Button).setOnClickListener { (findViewById(R.id.input_name) as TextView).text = generateRandomName() }
        (findViewById(R.id.action_start) as Button).setOnClickListener {
            val name = (findViewById(R.id.input_name) as TextView).text.toString()
            val editor = sharedPref.edit()
            editor.putString(GifTVActivity.EXTRA_NAME, name)
            editor.apply()
            val intent = Intent(this@SetupActivity, GifTVActivity::class.java)
            intent.putExtra(Server.NAME, name)
            intent.putExtra(Server.TYPE, getString(R.string.network_service_type))
            startActivity(intent)
        }
    }
    private fun generateRandomName(): String {
        val random = Random()
        val onomatopoeia = resources.getStringArray(R.array.omp_array)
        val first = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val second = onomatopoeia[random.nextInt(onomatopoeia.size)]
        val third = onomatopoeia[random.nextInt(onomatopoeia.size)]
        return "${first}-${second}-${third}"
    }
}
