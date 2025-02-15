package com.azad.customequalizer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.azad.customequalizer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private val bandsName = arrayListOf(101,102,103,104,105,106)
    private val bandLevels = arrayListOf(10,20,30,40,50,60)
    private val maxBandLevels = arrayListOf(50,100,70,80,100,100)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.equalizer.setBands(bandsName)
        binding.equalizer.setMax(maxBandLevels)

    }

    override fun onStart() {
        super.onStart()
        bandLevels.forEachIndexed { index, i ->
            binding.equalizer.setBandLevel(index,i)
        }
    }
}