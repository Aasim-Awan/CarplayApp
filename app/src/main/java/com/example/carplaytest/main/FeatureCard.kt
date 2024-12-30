package com.example.carplaytest.main

data class FeatureCard(
    val iconRes: Int,
    val title: String,
    val description: String,
    val onClickAction: () -> Unit
)
