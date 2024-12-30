package com.example.carplaytest.carparking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.carplaytest.databinding.ActivityCarParkingBinding

class CarParkingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarParkingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarParkingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnParkCar.setOnClickListener {
            val intent = Intent(this, ParkCarActivity::class.java)
            startActivity(intent)
        }
        binding.btnFindCar.setOnClickListener {
            val intent = Intent(this, FindCarActivity::class.java)
            startActivity(intent)
        }
        binding.backArrow.setOnClickListener {
            finish()
        }
    }
}
