package com.stefansturm.ripple.feature.today

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-only adapter for the phone/tablet gravity sensor. It never enters the
 * domain and only exposes a contained visual tilt angle in radians.
 */
class AndroidTiltController(
    context: Context,
    private val rotationProvider: () -> Int
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val _tilt = MutableStateFlow(0f)
    private var running = false
    private var angle = 0f
    private var velocity = 0f
    private var target = 0f
    private var lastTimestampNanos = 0L

    val tilt: StateFlow<Float> = _tilt.asStateFlow()

    fun start() {
        if (running || gravitySensor == null) return
        running = sensorManager?.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME) == true
        lastTimestampNanos = 0L
    }

    fun stop() {
        if (running) sensorManager?.unregisterListener(this)
        running = false
        angle = 0f
        velocity = 0f
        target = 0f
        lastTimestampNanos = 0L
        _tilt.value = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GRAVITY || event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude < 0.1f || abs(z / magnitude) > 0.92f) {
            angle = 0f
            velocity = 0f
            target = 0f
            _tilt.value = 0f
            lastTimestampNanos = event.timestamp
            return
        }

        val (right, down) = screenAlignedGravity(x, y, rotationProvider())
        target = atan2(-right, down)
        val deltaSeconds = if (lastTimestampNanos == 0L) {
            1f / 60f
        } else {
            ((event.timestamp - lastTimestampNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        }
        lastTimestampNanos = event.timestamp

        val shortestDelta = shortestAngleDelta(angle, target)
        val stiffness = 144f
        val damping = 11.52f
        velocity += (shortestDelta * stiffness - velocity * damping) * deltaSeconds
        angle += velocity * deltaSeconds
        if (abs(shortestAngleDelta(angle, target)) < 0.0005f && abs(velocity) < 0.0005f) {
            angle = target
            velocity = 0f
        }
        _tilt.value = wrapAngle(angle)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun screenAlignedGravity(x: Float, y: Float, rotation: Int): Pair<Float, Float> = when (rotation) {
        Surface.ROTATION_90 -> -y to -x
        Surface.ROTATION_180 -> -x to y
        Surface.ROTATION_270 -> y to x
        else -> x to -y
    }

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = (to - from + PI.toFloat()) % (2f * PI.toFloat())
        if (delta < 0f) delta += 2f * PI.toFloat()
        return delta - PI.toFloat()
    }

    private fun wrapAngle(value: Float): Float {
        var wrapped = (value + PI.toFloat()) % (2f * PI.toFloat())
        if (wrapped < 0f) wrapped += 2f * PI.toFloat()
        return wrapped - PI.toFloat()
    }
}
