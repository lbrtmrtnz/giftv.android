package com.icenerd.adapter

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.icenerd.giftv.BuildConfig

abstract class RecyclerPageListener constructor(llMan: LinearLayoutManager) : RecyclerView.OnScrollListener() {
    companion object {
        val TAG = "RecyclerPage"
        private val THRESHOLD_VISIBLE = 12
    }

    private val mLinearLayoutMan = llMan
    private var mPreviousTotal = 0 // Item count after the last load
    private var mbLoading = true // true = still waiting for data to load
    private var mCurrentPage = 0

    // ------ REQUIRED ------ //
    abstract fun onLoadMore(current_page: Int)
    // ------ REQUIRED ------ //

    fun onRefresh() {
        mCurrentPage = 0
        mPreviousTotal = 0
        mbLoading = true
    }

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val count_item = mLinearLayoutMan.itemCount
        val count_child = recyclerView!!.childCount
        val position_first = mLinearLayoutMan.findFirstVisibleItemPosition()

        if (mbLoading) {
            if (count_item > mPreviousTotal) {
                if (BuildConfig.DEBUG) Log.d(TAG, "DATA LOAD COMPLETE")
                mbLoading = false
                mPreviousTotal = count_item
            }
        }

        // Condition: Threshold has been reached, signal load more data
        if (!mbLoading && count_item - count_child <= position_first + THRESHOLD_VISIBLE) {
            if (BuildConfig.DEBUG) Log.d(TAG, "LOAD MORE DATA")
            mCurrentPage++
            onLoadMore(mCurrentPage)
            mbLoading = true
        }
    }

}