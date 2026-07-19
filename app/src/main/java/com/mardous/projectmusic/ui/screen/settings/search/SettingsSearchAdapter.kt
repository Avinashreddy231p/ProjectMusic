package com.mardous.projectmusic.ui.screen.settings.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mardous.projectmusic.databinding.ItemSettingsSearchBinding

class SettingsSearchAdapter(
    private val onClick: (SettingsSearchResult) -> Unit
) : RecyclerView.Adapter<SettingsSearchAdapter.ViewHolder>() {

    private var items: List<SettingsSearchResult> = emptyList()

    fun submitList(newItems: List<SettingsSearchResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingsSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemSettingsSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: SettingsSearchResult) {
            binding.titleText.text = result.title
            binding.summaryText.text = if (result.summary.isNotEmpty()) "${result.parentTitle} > ${result.summary}" else result.parentTitle
            binding.root.setOnClickListener { onClick(result) }
        }
    }
}
