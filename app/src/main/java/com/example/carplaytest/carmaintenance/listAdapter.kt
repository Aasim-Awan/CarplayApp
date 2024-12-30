package com.example.carplaytest.carmaintenance

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.carplaytest.R
import com.example.carplaytest.carmaintenance.database.CarMaintenance
import com.example.carplaytest.databinding.ItemListRecylerBinding

class ListRecyclerAdapter(
    private val onViewClicked: (CarMaintenance) -> Unit,
    private val onUpdateClicked: (CarMaintenance) -> Unit,
    private val onDeleteClicked: (CarMaintenance) -> Unit
) : ListAdapter<CarMaintenance, ListRecyclerAdapter.MaintenanceViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CarMaintenance>() {
            override fun areItemsTheSame(
                oldItem: CarMaintenance,
                newItem: CarMaintenance
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: CarMaintenance,
                newItem: CarMaintenance
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class MaintenanceViewHolder(private val binding: ItemListRecylerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(maintenance: CarMaintenance) {
            binding.imgIcon.setImageResource(maintenance.iconRes)
            binding.tvMaintenanceName.text = maintenance.maintenanceName
            binding.tvDate.text = maintenance.date

            binding.btnEdit.setOnClickListener { view ->
                val popupMenu = PopupMenu(binding.root.context, view)
                popupMenu.menuInflater.inflate(R.menu.menu_list, popupMenu.menu)

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_view -> {
                            onViewClicked(maintenance)
                            true
                        }

                        R.id.action_update -> {
                            onUpdateClicked(maintenance)
                            true
                        }

                        R.id.action_delete -> {
                            onDeleteClicked(maintenance)
                            true
                        }

                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaintenanceViewHolder {
        val binding = ItemListRecylerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MaintenanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaintenanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
