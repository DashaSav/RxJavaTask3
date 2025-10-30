package com.example.rxjavatask3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private val items = listOf("Элемент 1", "Элемент 2", "Элемент 3", "Элемент 4", "Элемент 5")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.itemText)
        val numberView: TextView = itemView.findViewById(R.id.itemNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = "Элемент"
        holder.numberView.text = position.toString()

        holder.itemView.setOnClickListener {
            EventBus.itemClickSubject.onNext(position)
        }
    }

    override fun getItemCount() = items.size
}