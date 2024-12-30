package com.example.carplaytest.trafficsign

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.carplaytest.R
import com.example.carplaytest.databinding.SignDetailsSheetBinding
import com.example.carplaytest.databinding.SignItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class TrafficSignAdapter(
    private val context: Context,
    private val signs: MutableList<TrafficSignModelItem>
) : RecyclerView.Adapter<TrafficSignAdapter.TrafficSignViewHolder>() {

    inner class TrafficSignViewHolder(private val binding: SignItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trafficSignModel: TrafficSignModelItem) {

            val imagePath = "file:///android_asset/${trafficSignModel.image}"
            Log.d("TrafficSignAdapter", "Loading image from assets: $imagePath")

            try {
                Glide.with(context)
                    .load(imagePath)
                    .into(binding.signImage)
            } catch (e: Exception) {
                Log.e("TrafficSignAdapter", "Failed to load image from assets: $imagePath", e)
                binding.signImage.setImageResource(R.drawable.car)
            }

            binding.signName.text = trafficSignModel.name ?: "Unknown Sign"

            binding.root.setOnClickListener {
                showSignDetails(trafficSignModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrafficSignViewHolder {
        val binding = SignItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrafficSignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrafficSignViewHolder, position: Int) {
        holder.bind(signs[position])
    }

    override fun getItemCount() = signs.size

    private fun showSignDetails(sign: TrafficSignModelItem) {
        val dialog = BottomSheetDialog(context)
        val sheetBinding = SignDetailsSheetBinding.inflate(LayoutInflater.from(context))

        sign.image.let { path ->
            val imagePath = "file:///android_asset/$path"
            Log.d("SignDetailsDialog", "Loading detail image from assets: $imagePath")

            try {
                Glide.with(context)
                    .load(imagePath)
                    .into(sheetBinding.serviceImage)
            } catch (e: Exception) {
                Log.e("SignDetailsDialog", "Failed to load detail image from assets: $imagePath", e)
                sheetBinding.serviceImage.setImageResource(R.drawable.car)
            }
        } ?: run {
            sheetBinding.serviceImage.setImageResource(R.drawable.car)
        }

        sheetBinding.signTitle.text = sign.name
        sheetBinding.signDescription.text = "Description: ${sign.description}"
       // sheetBinding.signUsage.text = "Placement: ${sign.placement}"
        sheetBinding.signCountrySpecificNotes.text = "Instructions: ${sign.instructions}"
        sheetBinding.signPenalties.text = "Consequences: ${sign.penalties}"
        sheetBinding.signRelated.text = "Related Signs: ${sign.relatedSigns.joinToString(", ")}"

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    fun updateSigns(newSigns: List<TrafficSignModelItem>) {
        signs.clear()
        signs.addAll(newSigns)
        notifyDataSetChanged()
    }
}
