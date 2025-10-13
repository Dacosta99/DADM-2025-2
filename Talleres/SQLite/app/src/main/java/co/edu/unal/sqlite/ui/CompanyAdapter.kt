// -----------------------------
// ui/CompanyAdapter.kt
// -----------------------------
package co.edu.unal.sqlite.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.sqlite.R
import co.edu.unal.sqlite.model.Company

class CompanyAdapter(
    private var items: MutableList<Company>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<CompanyAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(company: Company)
        fun onItemLongClick(company: Company)
    }

    fun updateData(newItems: List<Company>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_company, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        holder.bind(c)
        holder.itemView.setOnClickListener { listener.onItemClick(c) }
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(c)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)

        fun bind(c: Company) {
            tvName.text = c.name
            tvCategory.text = c.category
            tvPhone.text = c.phone
        }
    }
}

