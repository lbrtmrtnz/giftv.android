package com.icenerd.giftv.net

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

import com.icenerd.giftv.BuildConfig

class NsdHelper(private val mNsdManager: NsdManager, private val mServiceType: String) : NsdManager.DiscoveryListener {
    companion object {
        private val TAG = "NsdHelper"
    }

    interface NsdListener {
        fun nsdServiceResolved(serviceInfo: NsdServiceInfo?)
        fun nsdServiceLost(serviceInfo: NsdServiceInfo?)
    }
    private var mNsdListener: NsdListener? = null
    var isDiscovering = false
        private set

    fun startDiscovering(listener: NsdListener) {
        mNsdManager.discoverServices(mServiceType, NsdManager.PROTOCOL_DNS_SD, this)
        mNsdListener = listener
        mNsdListener?.nsdServiceResolved(null)
    }

    fun stopDiscovering() {
        if (isDiscovering) mNsdManager.stopServiceDiscovery(this)
    }

    /*---vvv--- NsdManager.DiscoveryListener ---vvv---*/

    override fun onDiscoveryStarted(serviceType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDiscoveryStarted($serviceType)")
        isDiscovering = true
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceFound($serviceInfo)")

        val serviceType = serviceInfo.serviceType

        if (serviceType == mServiceType) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Resolving onServiceFound($serviceType)")
            mNsdManager.resolveService(serviceInfo, mResolveServiceListener)
        }
    }
        private val mResolveServiceListener by lazy { object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    mNsdManager.resolveService(serviceInfo, this)
                } else {
                    Log.e(TAG, "onResolveFailed($errorCode)")
                }
            }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onServiceResolved($serviceInfo)")
                mNsdListener?.nsdServiceResolved(serviceInfo)
            }
        } }

    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onServiceLost($serviceInfo)")
        mNsdListener?.nsdServiceLost(serviceInfo)
    }

    override fun onDiscoveryStopped(serviceType: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDiscoveryStopped($serviceType)")
        isDiscovering = false
        mNsdListener?.nsdServiceLost(null)
    }

    override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStartDiscoveryFailed($serviceType - $errorCode)")
        isDiscovering = false
    }

    override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e(TAG, "onStopDiscoveryFailed($serviceType - $errorCode)")
        isDiscovering = false
    }

}