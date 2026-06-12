package com.noor.prism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private var menuItems: List<MenuModel>,
    private val onItemClick: (MenuModel) -> Unit
) : RecyclerView.Adapter<MenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: CardView = view.findViewById(R.id.cardContainer)
        val iconTextView: TextView = view.findViewById(R.id.iconTextView)
        val titleTextView: TextView = view.findViewById(R.id.titleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = menuItems[position]
        holder.titleTextView.text = item.title
        
        // Dynamically assigns distinct kid-friendly graphics based on item titles
        when (item.title.lowercase().trim()) {
            "quran" -> holder.iconTextView.text = "📖"
            "salah", "namaz", "prayer" -> holder.iconTextView.text = "🕌"
            else -> holder.iconTextView.text = "✨"
        }

        holder.cardContainer.setOnClickListener { onItemClick(item) }

        // TV MOTION ANIMATION BLOCK
        holder.cardContainer.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Glow & Scale Effect for Kids
                holder.cardContainer.setCardBackgroundColor(android.graphics.Color.parseColor("#89B4FA"))
                holder.titleTextView.setTextColor(android.graphics.Color.parseColor("#11111B"))
                holder.cardContainer.animate().scaleX(1.12f).scaleY(1.12f).translationZ(16f).setDuration(200).start()
            } else {
                // Return to clean rest state
                holder.cardContainer.setCardBackgroundColor(android.graphics.Color.parseColor("#2C2C3E"))
                holder.titleTextView.setTextColor(android.graphics.Color.WHITE)
                holder.cardContainer.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(200).start()
            }
        }
    }

    override fun getItemCount(): Int = menuItems.size

    fun updateData(newItems: List<MenuModel>) {
        this.menuItems = newItems
        notifyDataSetChanged()
    }
}
