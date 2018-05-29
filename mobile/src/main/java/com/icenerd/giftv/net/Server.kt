package com.icenerd.giftv.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.util.Log

import com.icenerd.giftv.BuildConfig

import java.io.IOException
import java.net.ServerSocket

import javax.crypto.spec.SecretKeySpec

class Server(private val mNsdManager: NsdManager,
             name: String,
             private val mType: String) : NsdManager.RegistrationListener {

    companion object {
        private val TAG = "Server"

        val NAME = "network_service_name"
        val TYPE = "network_service_type"

        val SIGNAL_JSON = "signal_json"
        val SIGNAL_CLOSE = "signal_close"
        val SIGNAL_IDENTIFY = "signal_identify"
    }

    var name: String? = null
        private set
    private var mServerThread: ServerThread? = null

    private var mHandler: Handler? = null
    fun setHandler(handler: Handler) {
        mHandler = handler
    }

    init {
        this.name = name
    }

    fun startServer(secretKeySpec: SecretKeySpec) {

        try {

            val serverSocket = ServerSocket(0)
            val port = serverSocket.localPort

            mServerThread = ServerThread(secretKeySpec, serverSocket, mHandler!!)
            Thread(mServerThread).start()

            val mNsdServiceInfo = NsdServiceInfo()
            mNsdServiceInfo.serviceName = name
            mNsdServiceInfo.serviceType = mType
            mNsdServiceInfo.port = port

            mNsdManager.registerService(mNsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, this)

        } catch (err: IOException) {
            Log.e(TAG, err.message)
        }

    }

    fun stopServer() {
        mNsdManager.unregisterService(this)
        mServerThread!!.terminate()
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        Log.e(TAG, "onRegistrationFailed: " + serviceInfo.serviceName)
    }

    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        Log.e(TAG, "onUnregistrationFailed: " + serviceInfo.serviceName)
    }

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
        name = serviceInfo.serviceName
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceRegistered: " + name!!)
    }

    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceUnregistered: " + serviceInfo.serviceName)
    }
}