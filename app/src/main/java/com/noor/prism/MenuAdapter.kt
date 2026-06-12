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

    // FIXED: Maps directly to the TextView inside your custom item_menu_tile.xml
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.titleTextView) // Double check that this ID matches your item_menu_tile.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // FIXED: Swapped out the hidden mobile layout for your custom XML TV tile
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = menuItems[position]
        holder.titleTextView.text = item.title
        
        // CRITICAL FOR TV REMOTES: Configures the tile to accept D-Pad navigation clicks
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
        
        holder.itemView.setOnClickListener { 
            onItemClick(item) 
        }

        // VISUAL FEEDBACK: Changes style properties when the user moves the remote highlight cursor
        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.setBackgroundColor(android.graphics.Color.WHITE)
                holder.titleTextView.setTextColor(android.graphics.Color.BLACK)
                view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start() // Subtle TV pop effect
            } else {
                view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                holder.titleTextView.setTextColor(android.graphics.Color.WHITE)
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }

    override fun getItemCount(): Int = menuItems.size

    fun updateData(newItems: List<MenuModel>) {
        this.menuItems = newItems
        notifyDataSetChanged()
    }
}
