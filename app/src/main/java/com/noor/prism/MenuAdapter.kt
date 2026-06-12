package com.noor.prism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private var menuItems: List<MenuModel>,
    private val onItemClick: (MenuModel) -> Unit
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = menuItems[position]
        holder.titleTextView.text = item.title
        
        // Setup remote control click listeners natively
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = menuItems.size

    fun updateData(newItems: List<MenuModel>) {
        this.menuItems = newItems
        notifyDataSetChanged()
    }
}
