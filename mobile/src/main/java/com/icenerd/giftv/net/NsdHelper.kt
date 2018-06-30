package com.icenerd.giftv.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

import com.icenerd.giftv.BuildConfig

class NsdHelper(private val mNsdManager: NsdManager?, private val mType: String) : NsdManager.DiscoveryListener {
    companion object {
        private val TAG = "NsdHelper"
    }
    private var mbDiscovering = false
    private var mNsdListener: NsdListener? = null


    interface NsdListener {
        fun nsdServiceResolved(serviceInfo: NsdServiceInfo?)
        fun nsdServiceLost(serviceInfo: NsdServiceInfo?)
    }

    fun startDiscovering(listener: NsdListener) {
        if (mNsdManager != null) {
            mNsdManager.discoverServices(mType, NsdManager.PROTOCOL_DNS_SD, this)
            mNsdListener = listener
            if (mNsdListener != null) mNsdListener!!.nsdServiceResolved(null)
        }
    }

    fun stopDiscovering() {
        if (mbDiscovering && mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(this)
        }
    }

    override fun onDiscoveryStarted(regType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service discovery started")
        mbDiscovering = true
    }

    override fun onServiceFound(service: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service found: $service")

        if (service.serviceType == mType) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Resolving: " + service.serviceType)
            mNsdManager!!.resolveService(service, object : NsdManager.ResolveListener {

                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                        mNsdManager.resolveService(serviceInfo, this)
                    } else {
                        Log.e(TAG, "onResolveFailed: $errorCode")
                    }
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "onServiceResolved: $serviceInfo")
                    if (mNsdListener != null) mNsdListener!!.nsdServiceResolved(serviceInfo)
                }

            })
        }
    }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Network Service loss: $serviceInfo")
        if (mNsdListener != null) mNsdListener!!.nsdServiceLost(serviceInfo)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Stopped looking for $serviceType")
        mbDiscovering = false
        if (mNsdListener != null) mNsdListener!!.nsdServiceLost(null)
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStartDiscoveryFailed: $errorCode")
        mbDiscovering = false
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStopDiscoveryFailed: $errorCode")
        mbDiscovering = false
    }

}