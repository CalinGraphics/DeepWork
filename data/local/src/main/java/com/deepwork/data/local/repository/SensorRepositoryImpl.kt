package com.deepwork.data.local.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.deepwork.domain.model.GestureType
import com.deepwork.domain.repository.SensorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SensorRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SensorRepository {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override fun startListening() {
        // Managed internally by flow
    }

    override fun stopListening() {
        // Managed internally by flow
    }

    override fun getGestureFlow(): Flow<GestureType> = callbackFlow {
        val accel = accelerometer
        if (accel == null) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val z = event.values[2]
                    if (z < -8.0) {
                        trySend(GestureType.FACE_DOWN)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
