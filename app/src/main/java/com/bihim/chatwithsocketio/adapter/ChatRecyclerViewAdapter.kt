package com.bihim.chatwithsocketio.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bihim.chatwithsocketio.R
import com.bihim.chatwithsocketio.global.GlobalValues
import com.bihim.chatwithsocketio.model.ChatViewModel

class ChatRecyclerViewAdapter(
    private val context: Context,
    private val list: ArrayList<ChatViewModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            GlobalValues().SENDER -> {
                SenderViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.sender_view, parent, false)
                )
            }
            else -> {
                ReceiverViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.receiver_view, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (list[position].viewType == GlobalValues().SENDER) {
            val holders = holder as SenderViewHolder
            holders.textView.text = list[position].message
        } else if (list[position].viewType == GlobalValues().RECEIVER) {
            val holders = holder as ReceiverViewHolder
            holders.textView.text = list[position].message
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].viewType
    }


    class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.sender_text)
    }

    class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.receiver_text)
    }

}