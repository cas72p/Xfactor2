package com.www.xfactor

import android.os.Bundle
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color

class SettingsActivity : AppCompatActivity() {

    object GlobalState {
        var mapDarkMode: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        val mapMode: ToggleButton = findViewById(R.id.map_mode)

        mapMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == false){
                mapMode.setBackgroundColor(0xFF000000.toInt())
                mapMode.setTextColor(0xFFFFFFFF.toInt())
            }
            else{
                mapMode.setBackgroundColor(0xFFF44336.toInt())
                mapMode.setTextColor(0xFF000000.toInt())
            }
            GlobalState.mapDarkMode = isChecked
        }
    }
}
