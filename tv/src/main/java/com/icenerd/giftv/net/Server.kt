package com.icenerd.giftv.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.util.Log

import com.icenerd.giftv.BuildConfig

import java.io.IOException
import java.net.ServerSocket

import javax.crypto.spec.SecretKeySpec

class Server(private val nsdManager: NsdManager,
             private var serviceName: String,
             private val serviceType: String) : NsdManager.RegistrationListener {

    companion object {
        private const val TAG = "Server"

        const val NAME = "network_service_name"
        const val TYPE = "network_service_type"

        const val SIGNAL_JSON = "signal_json"
        const val SIGNAL_CLOSE = "signal_close"
        const val SIGNAL_IDENTIFY = "signal_identify"
    }

    var handler: Handler? = null

    private var serverThread: ServerThread? = null

    fun startServer(secretKeySpec: SecretKeySpec) {

        try {

            val serverSocket = ServerSocket(0)
            val port = serverSocket.localPort

            serverThread = ServerThread(secretKeySpec, serverSocket, handler!!)
            Thread(serverThread).start()

            val mNsdServiceInfo = NsdServiceInfo()
            mNsdServiceInfo.serviceName = serviceName
            mNsdServiceInfo.serviceType = serviceType
            mNsdServiceInfo.port = port

            nsdManager.registerService(mNsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, this)

        } catch (err: IOException) {
            Log.e(TAG, err.message)
        }

    }

    fun stopServer() {
        nsdManager.unregisterService(this)
        serverThread?.terminate()
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        Log.e(TAG, "onRegistrationFailed: ${serviceInfo.serviceName}")
    }

    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        Log.e(TAG, "onUnregistrationFailed: ${serviceInfo.serviceName}")
    }

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
        serviceName = serviceInfo.serviceName
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceRegistered: $serviceName")
    }

    override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceUnregistered: ${serviceInfo.serviceName}")
    }
}
