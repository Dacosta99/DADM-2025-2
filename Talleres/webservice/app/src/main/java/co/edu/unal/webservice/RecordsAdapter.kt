package co.edu.unal.webservice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class RecordsAdapter(private val onClick: (Record) -> Unit) :
    ListAdapter<Record, RecordsAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record) = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: Record, newItem: Record) = oldItem == newItem
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleText)
        private val descView: TextView = itemView.findViewById(R.id.descText)

        fun bind(record: Record) {
            titleView.text = record.title
            descView.text = record.description
            itemView.setOnClickListener { onClick(record) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
