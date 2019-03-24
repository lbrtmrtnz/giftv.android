package com.icenerd.giftv.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.icenerd.giftv.BuildConfig
import com.icenerd.giftv.data.loader.GifRequest
import pl.droidsonroids.gif.GifDrawable
import java.util.*

class GifLoader(private val requestQueue: RequestQueue?, private val cache: GifCache) {
    private fun throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("GifLoader must be invoked from the main thread.")
        }
    }

    private var batchResponseDelay = 100

    private val requestsInFlight = HashMap<String, BatchedGifRequest>()
    private val responseBatch = HashMap<String, BatchedGifRequest>()
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    interface GifCache {
        fun getGif(url: String): GifDrawable?
        fun putGif(url: String, gif: GifDrawable)
    }

    interface GifListener : Response.ErrorListener {
        fun onResponse(response: GifContainer, isImmediate: Boolean)
    }

    operator fun get(requestUrl: String, imageListener: GifListener?): GifContainer {
        throwIfNotOnMainThread()

        val cachedGif = cache.getGif(requestUrl)
        if (cachedGif != null) {
            val container = GifContainer(cachedGif, requestUrl, null)
            imageListener?.onResponse(container, true)
            return container
        } else {
            val imageContainer = GifContainer(null, requestUrl, imageListener)
            imageListener?.onResponse(imageContainer, true)
            val request = requestsInFlight[requestUrl]
            if (request != null) {
                request.addContainer(imageContainer)
                return imageContainer
            }
            val newRequest = makeGifRequest(requestUrl)

            requestQueue?.add(newRequest)
            requestsInFlight[requestUrl] = BatchedGifRequest(imageContainer)
            return imageContainer
        }

    }

    private fun makeGifRequest(requestUrl: String): Request<GifDrawable> {
        return GifRequest(requestUrl,
                Response.Listener { response -> onGetGifSuccess(requestUrl, response) },
                Response.ErrorListener { error -> onGetGifError(requestUrl, error) })
    }

    private fun onGetGifSuccess(requestUrl: String, response: GifDrawable) {
        cache.putGif(requestUrl, response)
        val request = requestsInFlight.remove(requestUrl)
        request?.responseGif = response
        batchResponse(requestUrl, request)
    }

    protected fun onGetGifError(requestUrl: String, error: VolleyError) {
        val request = requestsInFlight.remove(requestUrl)
        request?.responseError = error
        batchResponse(requestUrl, request)
    }

    data class GifContainer(var gif: GifDrawable?, val requestUrl: String, val mListener: GifListener?)

    private inner class BatchedGifRequest(container: GifContainer) {
        var responseGif: GifDrawable? = null
        var responseError: VolleyError? = null
        val listContainers = LinkedList<GifContainer>()
        init { listContainers.add(container) }
        fun addContainer(container: GifContainer) {
            listContainers.add(container)
        }
    }

    private fun batchResponse(cacheKey: String, request: BatchedGifRequest?) {
        if (request == null ) {
            if (BuildConfig.DEBUG) Log.w( "GifLoader", "batchResponse(request) = null")
        } else {
            responseBatch[cacheKey] = request
            if (runnable == null) {
                runnable = Runnable {
                    for (bir in responseBatch.values) {
                        for (container in bir.listContainers) {
                            if (container.mListener == null) {
                                continue
                            }
                            if (bir.responseError == null) {
                                container.gif = bir.responseGif!!
                                container.mListener.onResponse(container, false)
                            } else {
                                container.mListener.onErrorResponse(bir.responseError)
                            }
                        }
                    }
                    responseBatch.clear()
                    runnable = null
                }
                handler.postDelayed(runnable, batchResponseDelay.toLong())
            }
        }
    }
}