package com.icenerd.giftv.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

import com.icenerd.giftv.BuildConfig

class NsdHelper(private val nsdManager: NsdManager?, private val serviceType: String) : NsdManager.DiscoveryListener {
    companion object {
        private val TAG = "NsdHelper"
    }
    private var isDiscovering = false
    private var nsdListener: NsdListener? = null


    interface NsdListener {
        fun nsdServiceResolved(serviceInfo: NsdServiceInfo?)
        fun nsdServiceLost(serviceInfo: NsdServiceInfo?)
    }

    fun startDiscovering(listener: NsdListener) {
        if (nsdManager != null) {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this)
            nsdListener = listener
            nsdListener?.nsdServiceResolved(null)
        }
    }

    fun stopDiscovering() {
        if (isDiscovering && nsdManager != null) {
            nsdManager.stopServiceDiscovery(this)
        }
    }

    override fun onDiscoveryStarted(regType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service discovery started")
        isDiscovering = true
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service found: $service")

        if (service.serviceType == serviceType) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Resolving: " + service.serviceType)
            nsdManager!!.resolveService(service, object : NsdManager.ResolveListener {

                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                        nsdManager.resolveService(serviceInfo, this)
                    } else {
                        Log.e(TAG, "onResolveFailed: $errorCode")
                    }
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "onServiceResolved: $serviceInfo")
                    nsdListener?.nsdServiceResolved(serviceInfo)
                }

            })
        }
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Network Service loss: $serviceInfo")
        nsdListener?.nsdServiceLost(serviceInfo)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Stopped looking for $serviceType")
        isDiscovering = false
        nsdListener?.nsdServiceLost(null)
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStartDiscoveryFailed: $errorCode")
        isDiscovering = false
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStopDiscoveryFailed: $errorCode")
        isDiscovering = false
    }

}