package com.example.carplaytest.trafficsign

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carplaytest.databinding.ActivityTrafficSignBinding
import com.example.carplaytest.utils.Utilities

class TrafficSignActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrafficSignBinding
    private lateinit var trafficSignAdapter: TrafficSignAdapter
    private var signs: List<TrafficSignModelItem> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrafficSignBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signs = Utilities.parseJsonToExercisesModel(
            this,
            "traffic_signs.json",
            Array<TrafficSignModelItem>::class.java
        )?.toList() ?: listOf()

        Log.d("list", "$signs")
        trafficSignAdapter = TrafficSignAdapter(this, signs.toMutableList())
        binding.recyclerView.adapter = trafficSignAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.apply {
            val signNames = signs.map { it.name }

            editSearch.setAdapter(
                ArrayAdapter(
                    this@TrafficSignActivity, android.R.layout.simple_dropdown_item_1line, signNames
                )
            )

            fun filterSigns(query: String) {
                val filteredSigns = signs.filter { it.name.equals(query, ignoreCase = true) }
                if (filteredSigns.isNotEmpty()) {
                    trafficSignAdapter.updateSigns(filteredSigns)
                } else {
                    Toast.makeText(
                        this@TrafficSignActivity,
                        "No sign found with the name '$query'",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            btnSearch.setOnClickListener {
                val searchQuery = editSearch.text.toString().trim()
                if (searchQuery.isEmpty()) {
                    Toast.makeText(
                        this@TrafficSignActivity,
                        "Please enter a sign name to search",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    filterSigns(searchQuery)
                }
            }
            binding.editSearch.setThreshold(1)

            editSearch.setOnItemClickListener { _, _, _, _ ->
                filterSigns(editSearch.text.toString().trim())
            }

            editSearch.setOnDismissListener {
                trafficSignAdapter.updateSigns(signs)
            }
        }

        binding.backArrow.setOnClickListener {
            finish()
        }
    }
}
