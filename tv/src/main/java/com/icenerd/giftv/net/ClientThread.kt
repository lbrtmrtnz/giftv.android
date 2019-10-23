package com.icenerd.giftv.net

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.util.Log

import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.orm.InstallationORM
import com.icenerd.giftv.data.orm.StatusORM

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.Buffer
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

class ClientThread @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
constructor(private val secretKeySpec: SecretKeySpec, private val socket: Socket, private val handler: Handler?) : Runnable {
    companion object {
        private const val TAG = "ClientThread"
    }

    private val cipher by lazy { Cipher.getInstance("AES") }

    @get:Synchronized
    var isRunning = false
        private set

    @Synchronized
    fun terminate() {
        isRunning = false
    }

    override fun run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        isRunning = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Connection started")

        try {

            val outgoing = PrintWriter(socket.getOutputStream(), true)
            val incoming = BufferedReader(InputStreamReader(socket.getInputStream()))

            var data: String = incoming.readLine() // chomp
            while(data.isNotEmpty()) {
                var shouldBreak = false
                decrypt(data)?.let { decrypted ->
                    when (decrypted) {
                        Server.SIGNAL_IDENTIFY -> onSignalIdentify(incoming, outgoing)
                        Server.SIGNAL_JSON -> onSignalJSON(incoming, outgoing)
                        Server.SIGNAL_CLOSE -> {
                            onSignalClose(outgoing)
                            shouldBreak = true
                        }
                    }
                }
                if (shouldBreak) break
                data = incoming.readLine()
            }

        } catch (err: IOException) {
            if(BuildConfig.DEBUG) err.printStackTrace()
        } finally {
            isRunning = false
            closeSocket()
        }
    }

    private fun decrypt(data: String): String? {
        var decrypted: String? = null
        try {
            if (cipher != null) {
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
                decrypted = String(cipher.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
            }
        } catch (err: BadPaddingException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        } catch (err: IllegalBlockSizeException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        } catch (err: InvalidKeyException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        }
        return decrypted
    }

    private fun onSignalIdentify(incoming: BufferedReader, outgoing: PrintWriter) {
        if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_IDENTIFY received")
        val data = incoming.readLine()
        if (data.isNotEmpty()) {
            try {
                decrypt(data)?.let { decrypted ->
                    val json = JSONObject(decrypted)
                    if (BuildConfig.DEBUG) Log.d("SIGNAL_IDENTIFY", json.toString(1))

                    json.put(StatusORM.TABLE, ServerThread.STATE?.getJSONObject())
                    json.put(InstallationORM.COL_UUID, ServerThread.STATE?.getUUID())
                    json.put(InstallationORM.COL_BRAND, Build.BRAND)
                    json.put(InstallationORM.COL_MANUFACTURER, Build.MANUFACTURER)
                    json.put(InstallationORM.COL_MODEL, Build.MODEL)
                    json.put(InstallationORM.COL_SERIAL, BuildConfig.BUILD_TIME)
                    json.put(InstallationORM.COL_SDK_INT, Build.VERSION.SDK_INT)

                    if (cipher != null) {
                        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)

                        outgoing.println(Base64.encodeToString(cipher.doFinal(Server.SIGNAL_IDENTIFY.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))

                        if (BuildConfig.DEBUG) Log.d("RESPONSE", json.toString(1))
                        outgoing.println(Base64.encodeToString(cipher.doFinal(json.toString().toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                    }
                }
            } catch (err: BadPaddingException) {
                err.printStackTrace()
            } catch (err: IllegalBlockSizeException) {
                err.printStackTrace()
            } catch (err: InvalidKeyException) {
                err.printStackTrace()
            } catch (err: JSONException) {
                err.printStackTrace()
            }
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_IDENTIFY complete")
    }

    private fun onSignalJSON(incoming: BufferedReader, outgoing: PrintWriter) {
        if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_JSON received")
        val data = incoming.readLine()
        if (data.isNotEmpty()) {
            decrypt(data)?.let { decrypted ->
                if (BuildConfig.DEBUG) Log.d("SIGNAL_JSON", decrypted)
                if (handler != null) {
                    val bundle = Bundle()
                    bundle.putString("json", decrypted)
                    val message = Message()
                    message.data = bundle
                    handler.sendMessage(message)
                    if (BuildConfig.DEBUG) Log.d("SIGNAL_JSON", "sent to handler")
                }
            }
        }
    }

    private fun onSignalClose(outgoing: PrintWriter) {
        if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE received")
        try {
            if (cipher != null) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)

                outgoing.println(Base64.encodeToString(cipher.doFinal(Server.SIGNAL_CLOSE.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE sent")
            }
        } catch (err: BadPaddingException) {
            err.printStackTrace()
        } catch (err: IllegalBlockSizeException) {
            err.printStackTrace()
        } catch (err: InvalidKeyException) {
            err.printStackTrace()
        }
    }

    private fun closeSocket() {
        if (!socket.isClosed) {
            try {
                socket.close()
                if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")
            } catch (err: IOException) {
                if(BuildConfig.DEBUG) err.printStackTrace()
            }

        }
    }
}
