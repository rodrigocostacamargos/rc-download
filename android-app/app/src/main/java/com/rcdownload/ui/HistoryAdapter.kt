package com.rcdownload.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rcdownload.data.db.DownloadHistoryEntity
import com.rcdownload.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<DownloadHistoryEntity, HistoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadHistoryEntity) {
            binding.tvTitle.text   = item.title
            binding.tvChannel.text = item.channelTitle
            binding.tvFormat.text  = if (item.format == "video") "Vídeo (MP4)" else "Áudio (MP3)"
            binding.tvLicense.text = when (item.license) {
                "creativeCommon" -> "CC BY"
                else             -> "Licença YouTube"
            }
            binding.tvDate.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(item.downloadedAt))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadHistoryEntity>() {
        override fun areItemsTheSame(old: DownloadHistoryEntity, new: DownloadHistoryEntity) =
            old.id == new.id

        override fun areContentsTheSame(old: DownloadHistoryEntity, new: DownloadHistoryEntity) =
            old == new
    }
}
