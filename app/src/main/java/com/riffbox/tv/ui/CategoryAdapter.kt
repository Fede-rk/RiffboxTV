package com.riffbox.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.riffbox.tv.R

class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    var selectedIndex = 0
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cat = categories[position]
        holder.bind(cat, position == selectedIndex)
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<String>) {
        categories = newCategories
        if (selectedIndex >= categories.size) selectedIndex = 0
        notifyDataSetChanged()
    }

    fun selectCategory(index: Int) {
        if (index in categories.indices) {
            val prev = selectedIndex
            selectedIndex = index
            notifyItemChanged(prev)
            notifyItemChanged(index)
            onCategoryClick(categories[index])
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.categoryTitle)

        fun bind(category: String, isSelected: Boolean) {
            title.text = category
            itemView.isSelected = isSelected

            itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
                    // Auto-scroll so focused item is centered and visible on TV
                    (itemView.parent as? RecyclerView)?.smoothScrollToPosition(bindingAdapterPosition)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }
            }

            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val prev = selectedIndex
                    selectedIndex = pos
                    notifyItemChanged(prev)
                    notifyItemChanged(pos)
                    onCategoryClick(category)
                }
            }
        }
    }
}