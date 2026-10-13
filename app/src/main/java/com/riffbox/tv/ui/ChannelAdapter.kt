package com.riffbox.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.riffbox.tv.R
import com.riffbox.tv.model.Channel

class ChannelAdapter(
    private var channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    private var selectedChannelId: Int = -1

    fun updateList(newList: List<Channel>, currentPlayingId: Int = selectedChannelId) {
        channels = newList
        selectedChannelId = currentPlayingId
        notifyDataSetChanged()
    }

    fun setSelectedChannel(channelId: Int) {
        selectedChannelId = channelId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel, position + 1, channel.id == selectedChannelId)
    }

    override fun getItemCount(): Int = channels.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText: TextView = itemView.findViewById(R.id.channelNumber)
        private val nameText: TextView = itemView.findViewById(R.id.channelName)
        private val badgeText: TextView = itemView.findViewById(R.id.channelBadge)

        fun bind(channel: Channel, index: Int, isSelected: Boolean) {
            numberText.text = "%02d".format(index)
            nameText.text = channel.name
            badgeText.text = channel.badge
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                onChannelClick(channel)
            }
        }
    }
}
