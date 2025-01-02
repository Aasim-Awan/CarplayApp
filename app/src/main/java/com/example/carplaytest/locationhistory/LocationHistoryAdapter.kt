package com.example.carplaytest.locationhistory

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.carplaytest.databinding.ItemLocationHistoryBinding
import com.example.carplaytest.locationhistory.database.LocationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationHistoryAdapter(
    private val context: Context,
    private val entityList: MutableList<LocationEntity>,
    private val onEntitySelected: (LocationEntity) -> Unit,
    private val onEntityDeleted: (LocationEntity) -> Unit
) : RecyclerView.Adapter<LocationHistoryAdapter.LocationViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding =
            ItemLocationHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val locationEntity = entityList[position]
        holder.bind(locationEntity, position == selectedPosition)
    }

    override fun getItemCount(): Int = entityList.size

    inner class LocationViewHolder(private val binding: ItemLocationHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: LocationEntity, isSelected: Boolean) {

            binding.startTimeTextView.text = location.startTime?.let {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
            } ?: "N/A"

            binding.endTimeTextView.text = location.endTime?.let {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
            } ?: "N/A"

            binding.createdDateTextView.text = location.createdDate?.let {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
            } ?: "N/A"


//            binding.deleteButton.setBackgroundColor(
//                if (isSelected) Color.parseColor("#000000")
//                else Color.parseColor("#0066FF")
//            )
//            binding.cardView.setBackgroundColor(
//                if (isSelected) Color.parseColor("#0066FF")
//                else Color.parseColor("#FFFFFF")
//            )

            binding.deleteButton.setOnClickListener {
                onEntityDeleted(location)
                removeItem(adapterPosition)
            }

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(previousPosition)
                notifyItemChanged(adapterPosition)
                notifyDataSetChanged() // Notify to update the UI
                onEntitySelected(location) // Notify the selection
            }
        }
    }

    private fun removeItem(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            entityList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
