package com.example.carplaytest.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.carplaytest.databinding.ItemCardBinding

class FeatureCardAdapter(
    private val featureCards: List<FeatureCard>
) : RecyclerView.Adapter<FeatureCardAdapter.FeatureCardViewHolder>() {

    inner class FeatureCardViewHolder(private val binding: ItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(featureCard: FeatureCard) {
            binding.tvTitle.text = featureCard.title
            binding.tvDescription.text = featureCard.description
            binding.imgFeatureIcon.setImageResource(featureCard.iconRes)

            binding.cardView.setOnClickListener {
                featureCard.onClickAction()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureCardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeatureCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureCardViewHolder, position: Int) {
        holder.bind(featureCards[position])
    }

    override fun getItemCount(): Int = featureCards.size
}
