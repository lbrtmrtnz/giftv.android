package com.icenerd.giftv.ui

import android.os.Handler
import android.os.Looper
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.icenerd.giftv.data.loader.GifRequest
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.util.*

class GifLoader(private val mRequestQueue: RequestQueue, private val mCache: GifCache) {
    companion object {
        fun getImageListener(view: GifImageView, defaultImageResId: Int, errorImageResId: Int): GifListener {
            return object : GifListener {
                override fun onErrorResponse(error: VolleyError) {
                    if (errorImageResId != 0) {
                        view.setImageResource(errorImageResId)
                    }
                }
                override fun onResponse(response: GifContainer, isImmediate: Boolean) {
                    if (response.gif != null) {
                        view.setImageDrawable(response.gif)
                    } else if (defaultImageResId != 0) {
                        view.setImageResource(defaultImageResId)
                    }
                }
            }
        }
    }

    private var mBatchResponseDelayMs = 100

    private val mInFlightRequests = HashMap<String, BatchedGifRequest>()
    private val mBatchedResponses = HashMap<String, BatchedGifRequest>()
    private val mHandler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null


    interface GifCache {
        fun getGif(url: String): GifDrawable?
        fun putGif(url: String, gif: GifDrawable)
    }

    interface GifListener : Response.ErrorListener {
        fun onResponse(response: GifContainer, isImmediate: Boolean)
    }

    operator fun get(requestUrl: String, imageListener: GifListener?): GifContainer {
        throwIfNotOnMainThread()

        val cachedGif = mCache.getGif(requestUrl)
        if (cachedGif != null) {
            val container = GifContainer(cachedGif, requestUrl, null)
            imageListener?.onResponse(container, true)
            return container
        }
        val imageContainer = GifContainer(null, requestUrl, imageListener)
        imageListener?.onResponse(imageContainer, true)
        val request = mInFlightRequests[requestUrl]
        if (request != null) {
            request.addContainer(imageContainer)
            return imageContainer
        }
        val newRequest = makeGifRequest(requestUrl)

        mRequestQueue.add(newRequest)
        mInFlightRequests[requestUrl] = BatchedGifRequest(newRequest, imageContainer)
        return imageContainer
    }

    protected fun makeGifRequest(requestUrl: String): Request<GifDrawable> {
        return GifRequest(requestUrl,
                Response.Listener { response -> onGetGifSuccess(requestUrl, response) },
                Response.ErrorListener { error -> onGetGifError(requestUrl, error) })
    }

    fun setBatchedResponseDelay(newBatchedResponseDelayMs: Int) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs
    }

    protected fun onGetGifSuccess(requestUrl: String, response: GifDrawable) {
        mCache.putGif(requestUrl, response)
        val request = mInFlightRequests.remove(requestUrl)

        if (request != null) {
            request.mResponseGif = response
            batchResponse(requestUrl, request)
        }
    }

    protected fun onGetGifError(requestUrl: String, error: VolleyError) {
        val request = mInFlightRequests.remove(requestUrl)

        if (request != null) {
            request.error = error
            batchResponse(requestUrl, request)
        }
    }

    inner class GifContainer(var gif: GifDrawable?, val requestUrl: String, val mListener: GifListener?) {
        fun cancelRequest() {
            if (mListener == null) {
                return
            }
            var request: BatchedGifRequest? = mInFlightRequests[requestUrl]
            if (request != null) {
                val canceled = request.removeContainerAndCancelIfNecessary(this)
                if (canceled) {
                    mInFlightRequests.remove(requestUrl)
                }
            } else {
                request = mBatchedResponses[requestUrl]
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this)
                    if (request.mContainers.size == 0) {
                        mBatchedResponses.remove(requestUrl)
                    }
                }
            }
        }
    }

    private inner class BatchedGifRequest(private val mRequest: Request<*>, container: GifContainer) {
        var mResponseGif: GifDrawable? = null
        var error: VolleyError? = null
        val mContainers = LinkedList<GifContainer>()

        init {
            mContainers.add(container)
        }

        fun addContainer(container: GifContainer) {
            mContainers.add(container)
        }

        fun removeContainerAndCancelIfNecessary(container: GifContainer): Boolean {
            mContainers.remove(container)
            if (mContainers.size == 0) {
                mRequest.cancel()
                return true
            }
            return false
        }
    }

    private fun batchResponse(cacheKey: String, request: BatchedGifRequest) {
        mBatchedResponses[cacheKey] = request
        if (mRunnable == null) {
            mRunnable = Runnable {
                for (bir in mBatchedResponses.values) {
                    for (container in bir.mContainers) {
                        if (container.mListener == null) {
                            continue
                        }
                        if (bir.error == null) {
                            container.gif = bir.mResponseGif
                            container.mListener.onResponse(container, false)
                        } else {
                            container.mListener.onErrorResponse(bir.error)
                        }
                    }
                }
                mBatchedResponses.clear()
                mRunnable = null
            }
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs.toLong())
        }
    }

    private fun throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("GIPHYLoader must be invoked from the main thread.")
        }
    }
}