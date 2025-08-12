package com.example.dash

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import kotlin.math.hypot

class MapTouchFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var googleMap: GoogleMap? = null
    var onMarkerPressStart: ((Marker) -> Unit)? = null
    var onMarkerPressEnd: (() -> Unit)? = null

    // Radio táctil en píxeles (configurable)
    var touchRadiusPx: Float = 0f

    private var activeMarker: Marker? = null
    private var pointerDown = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val map = googleMap ?: return super.dispatchTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerDown = true
                val pressMarker = hitTestMarker(map, ev.x, ev.y)
                if (pressMarker != null) {
                    activeMarker = pressMarker
                    onMarkerPressStart?.invoke(pressMarker)
                    // Consumimos parcialmente el evento para mantenerlo, pero dejamos que el mapa reciba para gestos
                    // Si quieres bloquear gestos mientras presionas, retorna true aquí sin llamar super.
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerDown) {
                    val m = activeMarker
                    if (m != null) {
                        val stillOver = isPointNearMarker(map, m, ev.x, ev.y)
                        if (!stillOver) {
                            // dedo se movió fuera
                            onMarkerPressEnd?.invoke()
                            activeMarker = null
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                pointerDown = false
                if (activeMarker != null) {
                    onMarkerPressEnd?.invoke()
                    activeMarker = null
                }
            }
        }

        // Dejamos que el mapa siga recibiendo eventos (pan/zoom)
        return super.dispatchTouchEvent(ev)
    }

    private fun hitTestMarker(map: GoogleMap, x: Float, y: Float): Marker? {
        // Podrías optimizar almacenando lista de marcadores en MapActivity y pasándola aquí.
        // Como no tenemos acceso directo a ellos desde el map, se espera que MapActivity use su lista.
        // Para simplicidad, usaremos un callback-based pattern: MapActivity setea un método que nos da la lista.
        return (markerProvider?.invoke() ?: emptyList()).firstOrNull {
            isPointNearMarker(map, it, x, y)
        }
    }

    // Proveedor de marcadores (inyectado desde MapActivity)
    var markerProvider: (() -> List<Marker>)? = null

    private fun isPointNearMarker(map: GoogleMap, marker: Marker, x: Float, y: Float): Boolean {
        val proj = map.projection
        val markerPoint: Point = proj.toScreenLocation(marker.position)
        val dx = x - markerPoint.x
        val dy = y - markerPoint.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        return dist <= touchRadiusPx
    }
}
