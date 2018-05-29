package com.icenerd.adapter

import android.database.Cursor
import android.support.v7.widget.RecyclerView

abstract class CursorRecyclerAdapter<VH : RecyclerView.ViewHolder>(private var mCursor: Cursor?) : RecyclerView.Adapter<VH>() {
    override fun getItemCount(): Int { return mCursor?.count?:0 }
    fun changeCursor(cursor: Cursor?) {
        val oldCursor = swapCursor(cursor)
        oldCursor?.close()
    }
    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (mCursor == newCursor) return null
        val oldCursor = mCursor
        mCursor = newCursor
        notifyDataSetChanged()
        return oldCursor
    }
    open protected fun getItem(position: Int): Cursor? {
        mCursor?.moveToPosition(position)
        return mCursor
    }
}