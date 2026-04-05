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
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Senzori reali pe hardware:
 * - **Accelerometru**: față pe masă (pornire/revenire din pauză), față spre tavan (pauză), shake (reset), înclinare față/spate (pitch).
 * - Giroscopul nu mai este folosit pentru controlul timerului (funcția Rotate Phone a fost eliminată).
 *
 * Emulatorul adesea nu are giroscop; atunci rămân doar gesturile pe accelerometru.
 */
class SensorRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SensorRepository {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override fun startListening() {
        // Flow-ul din [getGestureFlow] înregistrează listener-ii când e colectat.
    }

    override fun stopListening() {
        // Listener-ii se opresc la awaitClose din flow.
    }

    override fun getGestureFlow(): Flow<GestureType> = callbackFlow {
        val accel = accelerometer
        if (accel == null) {
            awaitClose { }
            return@callbackFlow
        }

        // Minim între două gesturi (evită spam când senzorul zgomotește).
        var lastGestureTime = 0L
        val minGapMs = 450L
        val facePairGapMs = 200L

        fun tryEmit(gesture: GestureType) {
            val now = System.currentTimeMillis()
            val gap = if (gesture == GestureType.FACE_DOWN || gesture == GestureType.FACE_UP) {
                facePairGapMs
            } else {
                minGapMs
            }
            if (now - lastGestureTime < gap) return
            lastGestureTime = now
            trySend(gesture)
        }

        var lastFaceDown = false
        var lastShakeTime = 0L
        val magHistory = ArrayDeque<Float>(5)

        var lastTiltUp = false
        var lastTiltDown = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = event.values[0]
                        val ay = event.values[1]
                        val az = event.values[2]

                        val faceDownNow = az < -8.0f
                        if (faceDownNow && !lastFaceDown) {
                            tryEmit(GestureType.FACE_DOWN)
                        }
                        if (!faceDownNow && lastFaceDown) {
                            tryEmit(GestureType.FACE_UP)
                        }
                        lastFaceDown = faceDownNow

                        val mag = sqrt(ax * ax + ay * ay + az * az)
                        if (magHistory.size >= 5) magHistory.removeFirst()
                        magHistory.addLast(mag)
                        if (magHistory.size == 5) {
                            val minM = magHistory.minOrNull() ?: return
                            val maxM = magHistory.maxOrNull() ?: return
                            val range = maxM - minM
                            val nowMs = System.currentTimeMillis()
                            if (range > 8f && nowMs - lastShakeTime > 1200L) {
                                lastShakeTime = nowMs
                                tryEmit(GestureType.SHAKE)
                            }
                        }

                        // Înclinare doar când telefonul nu e cu fața în jos (altfel pitch e instabil).
                        if (az > -6f) {
                            val pitch = atan2(
                                -ax.toDouble(),
                                sqrt((ay * ay + az * az).toDouble())
                            ).toFloat()
                            val tiltUp = pitch > 0.52f
                            val tiltDown = pitch < -0.52f
                            if (tiltUp && !lastTiltUp) {
                                tryEmit(GestureType.TILT_UP_30)
                            }
                            lastTiltUp = tiltUp
                            if (tiltDown && !lastTiltDown) {
                                tryEmit(GestureType.TILT_DOWN_30)
                            }
                            lastTiltDown = tiltDown
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
