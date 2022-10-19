package dev.epegasus.horizontalwheelview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.epegasus.horizontalwheelview.databinding.ActivityMainBinding
import dev.epegasus.wheel.StraightenWheelView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.swvWheelMain.setListener(object : StraightenWheelView.Listener() {
            override fun onRotationChanged(radians: Double) {
                updateUi()
            }
        })
    }

    private fun updateUi() {
        updateText()
        updateImage()
    }

    private fun updateText() {
        val text = String.format(Locale.US, "%.0f°", binding.swvWheelMain.degreesAngle)
        binding.mtvAngleMain.text = text
    }

    private fun updateImage() {
        val angle: Float = binding.swvWheelMain.degreesAngle.toFloat()
        binding.sivImageMain.rotation = angle
    }
}