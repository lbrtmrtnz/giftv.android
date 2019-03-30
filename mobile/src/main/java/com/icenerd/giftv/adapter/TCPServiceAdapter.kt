package com.icenerd.giftv.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import com.icenerd.adapter.CursorRecyclerAdapter
import com.icenerd.giftv.R
import com.icenerd.giftv.data.model.TCPServiceModel

class TCPServiceAdapter(private val actionListener: ActionListener) : CursorRecyclerAdapter<TCPServiceViewHolder>(null) {

    interface ActionListener {
        fun onItemClick(model: TCPServiceModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TCPServiceViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_status, parent, false)
        return TCPServiceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TCPServiceViewHolder, position: Int) {
        getItem(position)?.let {
            val model = TCPServiceModel(it)
            holder.bind(model, actionListener)
        }
    }

}