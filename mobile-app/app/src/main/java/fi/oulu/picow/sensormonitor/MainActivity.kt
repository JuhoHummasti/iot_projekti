package fi.oulu.picow.sensormonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import fi.oulu.picow.sensormonitor.ui.MeasurementViewModel
import fi.oulu.picow.sensormonitor.ui.SensorMonitorApp


class MainActivity : ComponentActivity() {

    private val viewModel: MeasurementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorMonitorApp(viewModel = viewModel)
        }
    }
}