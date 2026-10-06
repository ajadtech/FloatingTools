package com.azad.floatingtools.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.azad.floatingtools.R
import com.azad.floatingtools.data.AppItem

/**
 * Home-এর তিনটা সেকশনেই (Your own apps / All online apps / Your own web app supported)
 * একই অ্যাডাপ্টারটা ব্যবহার করা যাবে — শুধু আলাদা আলাদা লিস্ট পাঠালেই হবে।
 */
class AppListAdapter(
    private var items: List<AppItem>,
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgIcon)
        val name: TextView = view.findViewById(R.id.txtName)
        val status: TextView = view.findViewById(R.id.txtStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.name.text = item.name
        holder.status.text = if (item.hasWeb) "Web ✓" else "Web নেই"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<AppItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
