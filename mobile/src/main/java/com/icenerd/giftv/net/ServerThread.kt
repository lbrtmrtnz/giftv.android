package com.icenerd.giftv.net


import android.os.Handler
import android.util.Log

import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.model.StatusModel

import java.io.IOException
import java.net.ServerSocket
import java.security.NoSuchAlgorithmException

import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

class ServerThread(private val mSecretKeySpec: SecretKeySpec, private val mServerSocket: ServerSocket, private val mHandler: Handler) : Runnable {
    companion object {
        private val TAG = "ServerThread"

        var STATE: StatusModel? = null
    }

    @get:Synchronized
    var isRunning = false
        private set

    @Synchronized
    fun terminate() {
        isRunning = false
    }

    override fun run() {
        isRunning = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Listening for connections")
        while (isRunning) {
            try {

                val socket = mServerSocket.accept()
                if (BuildConfig.DEBUG) Log.d(TAG, "Connection accepted")

                val connectionThread = ClientThread(mSecretKeySpec, socket, mHandler)
                Thread(connectionThread).start()

            } catch (err: IOException) {
                err.printStackTrace()
            } catch (err: NoSuchAlgorithmException) {
                err.printStackTrace()
            } catch (err: NoSuchPaddingException) {
                err.printStackTrace()
            }

        }
        if (!mServerSocket.isClosed) {
            try {

                mServerSocket.close()
                if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")

            } catch (err: IOException) {
                err.printStackTrace()
            }

        }
    }
}