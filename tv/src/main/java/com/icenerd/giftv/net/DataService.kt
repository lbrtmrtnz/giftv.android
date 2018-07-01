package com.icenerd.giftv.net


import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.support.v4.content.LocalBroadcastManager
import android.util.Base64
import android.util.Log
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.R
import com.icenerd.giftv.data.GIFTVDB
import com.icenerd.giftv.data.model.StatusModel
import com.icenerd.giftv.data.model.TCPServiceModel
import com.icenerd.giftv.data.orm.StatusORM
import com.icenerd.giftv.data.orm.TCPServiceORM
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec


class DataService : JobIntentService() {
    companion object {
        private const val TAG = "DataService"

        const val ACTION_IDENTIFY = "action_identify"
        const val ACTION_SEND = "action_send"
        const val ACTION_LOST = "action_lost"

        const val UPDATE_SERVICE_INFO = "update_service_info"

        private val JOB_ID = Random().nextInt()
        @JvmStatic fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, DataService::class.java, JOB_ID, work)
        }
    }

    private var secretKey: SecretKeySpec? = null
    private var cipher: Cipher? = null

    override fun onHandleWork(intent: Intent) {
        if (secretKey == null) {
            secretKey = SecretKeySpec(Base64.decode(getString(R.string.network_service_secret), Base64.NO_CLOSE or Base64.NO_WRAP), "AES")
            try {
                cipher = Cipher.getInstance("AES")
            } catch (err: NoSuchAlgorithmException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: NoSuchPaddingException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            }

        }
        try {
            when(intent.action) {
                ACTION_IDENTIFY -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, ACTION_IDENTIFY)
                    val name = intent.getStringExtra("name")
                    val host = intent.getStringExtra("host")
                    val port = intent.getIntExtra("port", 0)
                    TCP_identify(name, host, port)
                }
                ACTION_SEND -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, ACTION_SEND)
                    val host = intent.getStringExtra("host")
                    val port = intent.getIntExtra("port", 0)
                    val data = intent.getStringExtra("data")
                    TCP_send(host, port, data)
                }
                ACTION_LOST -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, ACTION_LOST)
                    val name = intent.getStringExtra("name")
                    TCP_lost(name)
                }
            }
        } catch (err: Exception) {
            err.printStackTrace()
        }

    }

    private fun TCP_identify(name: String, host: String, port: Int) {
        var socket: Socket? = null

        try {
            socket = Socket(host, port)

            val outgoing = PrintWriter(socket.getOutputStream(), true)
            val incoming = BufferedReader(InputStreamReader(socket.getInputStream()))

            try {
                if (cipher != null) {
                    cipher!!.init(Cipher.ENCRYPT_MODE, secretKey)

                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_IDENTIFY")
                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(Server.SIGNAL_IDENTIFY.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))

                    val sigIdentify = "{\"name\":\"$name\",\"host\":\"$host\",\"port\":$port}"
                    if (BuildConfig.DEBUG) Log.d("SIGNAL_IDENTIFY", sigIdentify)
                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(sigIdentify.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))

                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(Server.SIGNAL_CLOSE.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE sent")

                }
            } catch (err: BadPaddingException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: IllegalBlockSizeException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: InvalidKeyException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            }


            var data: String = "BEGIN"
            while (!data.isEmpty()) {
                data = incoming.readLine()
                try {
                    if (cipher != null) {
                        cipher?.init(Cipher.DECRYPT_MODE, secretKey)
                        data = String(cipher!!.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                        if (BuildConfig.DEBUG) Log.d(TAG, data)
                    }
                } catch (err: BadPaddingException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                } catch (err: IllegalBlockSizeException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                } catch (err: InvalidKeyException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                }

                if (data.equals(Server.SIGNAL_CLOSE)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE received")
                    break
                }
                if (data.equals(Server.SIGNAL_IDENTIFY)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_IDENTIFY received")
                    data = incoming.readLine()
                    if (!data.isEmpty()) {

                        try {
                            if (cipher != null) {
                                cipher!!.init(Cipher.DECRYPT_MODE, secretKey)
                                data = String(cipher!!.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                                if (BuildConfig.DEBUG) Log.d("SIGNAL_IDENTIFY", data)
                            }
                        } catch (err: BadPaddingException) {
                            if (BuildConfig.DEBUG) err.printStackTrace()
                        } catch (err: IllegalBlockSizeException) {
                            if (BuildConfig.DEBUG) err.printStackTrace()
                        } catch (err: InvalidKeyException) {
                            if (BuildConfig.DEBUG) err.printStackTrace()
                        }

                        handleServiceInfo(JSONObject(data))
                    }
                }

            }

            if (BuildConfig.DEBUG) Log.d(TAG, "TCP_identify success")

        } catch (err: IOException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        } catch (err: JSONException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        } finally {
            if (socket != null && !socket.isClosed) {
                try {
                    socket.close()
                    if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")
                } catch (err: IOException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                }

            }
        }

    }

    private fun TCP_send(host: String, port: Int, jsonString: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "TCP_send")
        var socket: Socket? = null

        try {
            socket = Socket(host, port)

            val outgoing = PrintWriter(socket.getOutputStream(), true)
            val incoming = BufferedReader(InputStreamReader(socket.getInputStream()))

            try {
                if (cipher != null) {
                    cipher!!.init(Cipher.ENCRYPT_MODE, secretKey)

                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(Server.SIGNAL_JSON.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_JSON sent")

                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(jsonString.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                    if (BuildConfig.DEBUG) Log.d(TAG, "jsonString sent")

                    outgoing.println(Base64.encodeToString(cipher!!.doFinal(Server.SIGNAL_CLOSE.toByteArray()), Base64.NO_CLOSE or Base64.NO_WRAP))
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE sent")
                }
            } catch (err: BadPaddingException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: IllegalBlockSizeException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            } catch (err: InvalidKeyException) {
                if (BuildConfig.DEBUG) err.printStackTrace()
            }

            var data: String = "BEGIN"
            while (!data.isEmpty()) {
                data = incoming.readLine()
                try {
                    if (cipher != null) {
                        cipher!!.init(Cipher.DECRYPT_MODE, secretKey)
                        data = String(cipher!!.doFinal(Base64.decode(data, Base64.NO_CLOSE or Base64.NO_WRAP)))
                        if (BuildConfig.DEBUG) Log.d(TAG, data)
                    }
                } catch (err: BadPaddingException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                } catch (err: IllegalBlockSizeException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                } catch (err: InvalidKeyException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                }

                if (data.equals(Server.SIGNAL_CLOSE)) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "SIGNAL_CLOSE received")
                    break
                }
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "TCP_send complete")

        } catch (err: IOException) {
            err.printStackTrace()
        } finally {
            if (socket != null && !socket.isClosed) {
                try {
                    socket.close()
                    if (BuildConfig.DEBUG) Log.d(TAG, "Socket closed")
                } catch (err: IOException) {
                    if (BuildConfig.DEBUG) err.printStackTrace()
                }

            }
        }

    }

    private fun TCP_lost(name: String?) {
        val affected_rows = if (name == null) {
            val db = GIFTVDB(this).writableDatabase
            val orm = TCPServiceORM(db)
            val num = orm.deleteWhere(null)
            db.close()
            num
        } else {
            val db = GIFTVDB(this).writableDatabase
            val orm = TCPServiceORM(db)
            val num = orm.deleteWhere("${TCPServiceORM.COL_NAME}='$name'")
            db.close()
            num
        }

        if (affected_rows > 0) LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(DataService.UPDATE_SERVICE_INFO))
    }

    @Throws(JSONException::class)
    private fun handleServiceInfo(json: JSONObject): Boolean {
        if (json.has(StatusORM.TABLE)) {
            val statusModel = handleStatus(json.getJSONObject(StatusORM.TABLE))
            if (statusModel != null) {
                if (json.has("name") && json.has("host") && json.has("port")) {
                    val serviceInfoModel = TCPServiceModel()
                    serviceInfoModel.name = json.getString("name")
                    serviceInfoModel.host = json.getString("host")
                    serviceInfoModel.port = json.getInt("port")
                    serviceInfoModel.id_status = statusModel.getUUID()
                    val db = GIFTVDB(this).writableDatabase
                    val orm = TCPServiceORM(db)
                    if (orm.save(serviceInfoModel)) {
                        db.close()
                        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(UPDATE_SERVICE_INFO))
                    } else {
                        db.close()
                    }

                }
            }
        }
        return true
    }

    @Throws(JSONException::class)
    private fun handleStatus(json: JSONObject): StatusModel? {
        val model = StatusModel(json)

        val db = GIFTVDB(this).writableDatabase
        val statusORM = StatusORM(db)
        val mbSaved = statusORM.save(model)
        db.close()

        return if (mbSaved) model else null
    }
}