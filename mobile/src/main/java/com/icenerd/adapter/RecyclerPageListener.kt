package com.icenerd.adapter

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icenerd.giftv.BuildConfig

abstract class RecyclerPageListener constructor(private val llMan: LinearLayoutManager): RecyclerView.OnScrollListener() {
    companion object {
        const val TAG = "RecyclerPage"
        private const val THRESHOLD_VISIBLE = 12
    }

    private var previousTotal = 0 // Item count after the last load
    private var isLoading = true // true = still waiting for data to load
    private var currentPage = 0

    // ------ REQUIRED ------ //
    abstract fun onLoadMore(current_page: Int)
    // ------ REQUIRED ------ //

    fun onRefresh() {
        currentPage = 0
        previousTotal = 0
        isLoading = true
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val countItem = llMan.itemCount
        val countChild = recyclerView.childCount
        val positionFirst = llMan.findFirstVisibleItemPosition()

        if (isLoading) {
            if (countItem > previousTotal) {
                if (BuildConfig.DEBUG) Log.d(TAG, "DATA LOAD COMPLETE")
                isLoading = false
                previousTotal = countItem
            }
        }

        // Condition: Threshold has been reached, signal load more data
        if (!isLoading && countItem - countChild <= positionFirst + THRESHOLD_VISIBLE) {
            if (BuildConfig.DEBUG) Log.d(TAG, "LOAD MORE DATA")
            currentPage++
            onLoadMore(currentPage)
            isLoading = true
        }
    }

}