package com.example.carplaytest.trafficsign

data class TrafficSignModelItem(
    val consequences: String,
    val description: String,
    val id: Int,
    val image: String,
    val instructions: String,
    val name: String,
    val penalties: String,
    val placement: String,
    val purpose: String,
    val relatedSigns: List<String>
)