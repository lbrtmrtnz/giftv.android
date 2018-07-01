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

class ServerThread(private val secretKeySpec: SecretKeySpec, private val serverSocket: ServerSocket, private val handler: Handler) : Runnable {
    companion object {
        private const val TAG = "ServerThread"
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

                val socket = serverSocket.accept()
                if (BuildConfig.DEBUG) Log.d(TAG, "Connection accepted")

                val connectionThread = ClientThread(secretKeySpec, socket, handler)
                Thread(connectionThread).start()

            } catch (err: IOException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: NoSuchAlgorithmException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: NoSuchPaddingException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            }

        }
        if (!serverSocket.isClosed) {
            try {

                serverSocket.close()
                if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")

            } catch (err: IOException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            }

        }
    }
}