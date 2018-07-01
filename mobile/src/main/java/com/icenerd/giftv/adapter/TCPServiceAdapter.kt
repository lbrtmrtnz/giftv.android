package com.icenerd.giftv.adapter

import android.os.Build
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.icenerd.adapter.CursorRecyclerAdapter
import com.icenerd.giftv.R
import com.icenerd.giftv.data.model.TCPServiceModel

class TCPServiceAdapter(private val clickListener: OnItemClickListener) : CursorRecyclerAdapter<TCPServiceAdapter.ViewHolder>(null) {
    interface OnItemClickListener {
        fun onItemClick(model: TCPServiceModel)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_status, parent, false)
        return ViewHolder(itemView)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = TCPServiceModel(getItem(position)!!)
        holder.bind(model, clickListener)
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //var itemStatus = itemView.findViewById<View>(R.id.item_status)
        //var imgIcon = itemView.findViewById<View>(R.id.item_status_icon)
        private val txtName: TextView = itemView.findViewById(R.id.item_status_title)

        fun bind(model: TCPServiceModel, listener: OnItemClickListener) {
            if (Build.VERSION.SDK_INT >= 24) {
                txtName.text = Html.fromHtml(model.name, Html.FROM_HTML_MODE_COMPACT).toString()
            } else {
                @Suppress("DEPRECATION")
                txtName.text = Html.fromHtml(model.name).toString()
            }
            itemView.setOnClickListener { listener.onItemClick(model) }
        }
    }
}