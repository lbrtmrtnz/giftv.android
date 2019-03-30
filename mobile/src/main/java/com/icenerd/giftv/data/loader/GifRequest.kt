package com.icenerd.giftv.data.loader

import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import pl.droidsonroids.gif.GifDrawable
import java.io.IOException

class GifRequest(url: String, private val mListener: Response.Listener<GifDrawable>, errorListener: Response.ErrorListener) : Request<GifDrawable>(Request.Method.GET, url, errorListener) {
    companion object {
        private const val IMAGE_TIMEOUT_MS = 1000
        private const val IMAGE_MAX_RETRIES = 2
        private const val IMAGE_BACKOFF = 2f
        private val sDecodeLock = Any()
    }

    init {
        retryPolicy = DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF)
    }

    override fun getPriority(): Request.Priority {
        return Request.Priority.LOW
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<GifDrawable> {
        synchronized(sDecodeLock) {
            return try {
                doParse(response)
            } catch (err: OutOfMemoryError) {
                if (BuildConfig.DEBUG) err.printStackTrace()
                VolleyLog.e("Caught OOM for ${response.data.size} byte gif, url=$url")
                Response.error(ParseError(err))
            }
        }
    }

    override fun deliverResponse(response: GifDrawable) {
        mListener.onResponse(response)
    }

    private fun doParse(response: NetworkResponse): Response<GifDrawable> {
        val data = response.data
        var drawable: GifDrawable? = null

        try {
            drawable = GifDrawable(data)
        } catch (err: IOException) {
            if (BuildConfig.DEBUG) err.printStackTrace()
        }

        return when (drawable) {
            null -> Response.error(ParseError(response))
            else -> Response.success(drawable, HttpHeaderParser.parseCacheHeaders(response))
        }
    }
}