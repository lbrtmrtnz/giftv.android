package com.icenerd.data

import org.json.JSONObject

abstract class NetworkModel: Model() {
    abstract fun getJSONObject(): JSONObject
    protected fun jsonPutOrNULL(obj: Any?): Any {
        return obj ?: JSONObject.NULL
    }
}
