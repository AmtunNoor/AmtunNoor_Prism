package com.noor.prism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noor.prism.R

data class MenuItem(val title: String, val url: String)

class MenuAdapter(
    private var items: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // MATCHED: Now correctly finds tileTitle from your MaterialCardView layout
        val textView: TextView = view.findViewById(R.id.tileTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // MATCHED: Now correctly inflates item_menu_tile instead of menu_item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.title

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MenuItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
