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
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

class ClientThread @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
constructor(private val mSecretKey: SecretKeySpec, private val mSocket: Socket, private val mHandler: Handler?) : Runnable {
    companion object {
        private val TAG = "ClientThread"
    }

    private val mCipher: Cipher?

    @get:Synchronized
    var isRunning = false
        private set

    @Synchronized
    fun terminate() {
        isRunning = false
    }

    init {
        mCipher = Cipher.getInstance("AES")
    }

    override fun run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        isRunning = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Connection started")

        try {

            val outgoing = PrintWriter(mSocket.getOutputStream(), true)
            val incoming = BufferedReader(InputStreamReader(mSocket.getInputStream()))

            var data: String = incoming.readLine() // chomp
            while(!data.isEmpty()) {
                try {
                    if (mCipher != null) {
                        mCipher.init(Cipher.DECRYPT_MODE, mSecretKey)
                        data = String(mCipher.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                    }
                } catch (err: BadPaddingException) {
                    err.printStackTrace()
                } catch (err: IllegalBlockSizeException) {
                    err.printStackTrace()
                } catch (err: InvalidKeyException) {
                    err.printStackTrace()
                }

                if (data.equals(Server.SIGNAL_IDENTIFY)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_IDENTIFY received")
                    data = incoming.readLine()
                    if (!data.isEmpty()) {

                        try {
                            if (mCipher != null) {
                                mCipher.init(Cipher.DECRYPT_MODE, mSecretKey)
                                data = String(mCipher.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                            }
                        } catch (err: BadPaddingException) {
                            err.printStackTrace()
                        } catch (err: IllegalBlockSizeException) {
                            err.printStackTrace()
                        } catch (err: InvalidKeyException) {
                            err.printStackTrace()
                        }

                        try {

                            val json = JSONObject(data)
                            if (BuildConfig.DEBUG) Log.d("SIGNAL_IDENTIFY", json.toString(1))

                            json.put(StatusORM.TABLE, ServerThread.STATE?.getJSONObject())
                            json.put(InstallationORM.COL_UUID, ServerThread.STATE?.getUUID())
                            json.put(InstallationORM.COL_BRAND, Build.BRAND)
                            json.put(InstallationORM.COL_MANUFACTURER, Build.MANUFACTURER)
                            json.put(InstallationORM.COL_MODEL, Build.MODEL)
                            json.put(InstallationORM.COL_SERIAL, Build.SERIAL)
                            json.put(InstallationORM.COL_SDK_INT, Build.VERSION.SDK_INT)

                            if (mCipher != null) {
                                mCipher.init(Cipher.ENCRYPT_MODE, mSecretKey)

                                outgoing.println(Base64.encodeToString(mCipher.doFinal(Server.SIGNAL_IDENTIFY.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))

                                if (BuildConfig.DEBUG) Log.d("RESPONSE", json.toString(1))
                                outgoing.println(Base64.encodeToString(mCipher.doFinal(json.toString().toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
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

                } else if (data.equals(Server.SIGNAL_JSON)) { // next line will be json
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_JSON received")
                    data = incoming.readLine()
                    if (!data.isEmpty()) {

                        try {
                            if (mCipher != null) {
                                mCipher.init(Cipher.DECRYPT_MODE, mSecretKey)
                                data = String(mCipher.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                            }
                        } catch (err: BadPaddingException) {
                            err.printStackTrace()
                        } catch (err: IllegalBlockSizeException) {
                            err.printStackTrace()
                        } catch (err: InvalidKeyException) {
                            err.printStackTrace()
                        }

                        if (BuildConfig.DEBUG) Log.d("SIGNAL_JSON", data)
                        if (mHandler != null) {
                            val bundle = Bundle()
                            bundle.putString("json", data)
                            val message = Message()
                            message.data = bundle
                            mHandler.sendMessage(message)
                            if (BuildConfig.DEBUG) Log.d("SIGNAL_JSON", "sent to handler")
                        }
                    }
                } else if (data.equals(Server.SIGNAL_CLOSE) || !isRunning) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE received")
                    try {
                        if (mCipher != null) {
                            mCipher.init(Cipher.ENCRYPT_MODE, mSecretKey)

                            outgoing.println(Base64.encodeToString(mCipher.doFinal(Server.SIGNAL_CLOSE.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                            if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE sent")
                        }
                    } catch (err: BadPaddingException) {
                        err.printStackTrace()
                    } catch (err: IllegalBlockSizeException) {
                        err.printStackTrace()
                    } catch (err: InvalidKeyException) {
                        err.printStackTrace()
                    }

                    break
                }

            }

        } catch (err: IOException) {
            err.printStackTrace()
        } finally {
            isRunning = false
            closeSocket()
        }
    }

    private fun closeSocket() {
        if (!mSocket.isClosed) {
            try {
                mSocket.close()
                if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")
            } catch (err: IOException) {
                err.printStackTrace()
            }

        }
    }
}