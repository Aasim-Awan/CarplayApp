package com.example.carplaytest.carmaintenance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.carplaytest.databinding.ItemMaintenanceBinding

class MaintenanceAdapter(
    private val maintenanceList: List<String>,
    private val icons: List<Int>,
    private val onClick: (String, Int) -> Unit
) : RecyclerView.Adapter<MaintenanceAdapter.MaintenanceViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val binding = ItemMaintenanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaintenanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        holder.bind(maintenanceList[position], icons[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = maintenanceList.size

    inner class MaintenanceViewHolder(private val binding: ItemMaintenanceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String, iconRes: Int, isSelected: Boolean) {
            binding.tvName.text = name
            binding.imgIcon.setImageResource(iconRes)

            binding.carCard.setCardBackgroundColor(
                if (isSelected) Color.parseColor("#0066FF")
                else Color.parseColor("#FFFFFF")
            )
            binding.tvName.setTextColor(
                if (isSelected) Color.parseColor("#FFFFFF")
                else Color.parseColor("#000000")
            )
            binding.imgIcon.setColorFilter(
                if (isSelected) Color.parseColor("#FFFFFF")
                else Color.parseColor("#000000")
            )

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                notifyItemChanged(previousPosition)
                notifyItemChanged(adapterPosition)

                onClick(name, iconRes)
            }
        }
    }
}
