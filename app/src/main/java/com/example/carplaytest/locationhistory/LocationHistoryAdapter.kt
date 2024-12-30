package com.example.carplaytest.locationhistory

import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.carplaytest.databinding.ItemLocationHistoryBinding
import com.example.carplaytest.locationhistory.database.LocationLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationHistoryAdapter(
    private val context: Context,
    private val locations: List<LocationLog>,
    ) : RecyclerView.Adapter<LocationHistoryAdapter.LocationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding =
            ItemLocationHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.bind(location)
    }

    override fun getItemCount(): Int = locations.size

    inner class LocationViewHolder(private val binding: ItemLocationHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: LocationLog) {
            binding.latitudeTextView.text = location.latitude.toString()
            binding.longitudeTextView.text = location.longitude.toString()
            binding.timestampTextView.text =
                SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date(location.timestamp)
                )

            val geocoder = Geocoder(context, Locale.getDefault())
            try {
                val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address = addressList[0]
                    binding.locationNameTextView.text = address.getAddressLine(0)
                } else {
                    binding.locationNameTextView.text = "Unknown Location"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.locationNameTextView.text = "Error fetching location"
            }
        }
    }
}
