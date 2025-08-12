package com.example.dash

import android.content.Intent
import android.os.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.google.gson.Gson
import android.util.Log

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val handler = Handler(Looper.getMainLooper())
    private val markers = mutableListOf<Marker>()
    private val holdMap = mutableMapOf<Marker, Runnable>()
    private var popupWindow: PopupWindow? = null
    private lateinit var mapView: View
    private var activeMarker: Marker? = null
    private val markerUbicaciones = mutableMapOf<Marker, LatLng>()
    private val markerToGuidUsuario = mutableMapOf<Marker, String>() // <-- Aquí guardamos guidUsuario por marker
    private var pollingJob: Job? = null
    private val pollingInterval = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapFragment.view?.let {
            mapView = it
            it.setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_UP) holdMap.values.forEach { handler.removeCallbacks(it) }
                false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        iniciarPolling()
    }

    override fun onStop() {
        super.onStop()
        detenerPolling()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.2, -98.8), 9.5f))
        cargarUbicacionesConUsuario()
        mMap.setOnCameraMoveListener {
            popupWindow?.let { popup ->
                activeMarker?.let { m ->
                    val pt = mMap.projection.toScreenLocation(m.position)
                    popup.update(
                        pt.x - popup.contentView.width / 2,
                        pt.y - popup.contentView.height - 40, -1, -1
                    )
                }
            }
        }
        mMap.setOnMarkerClickListener { marker ->
            val r = Runnable { showInfoPopup(marker) }
            holdMap[marker] = r
            handler.postDelayed(r, 500)
            true
        }
    }

    private fun iniciarPolling() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                cargarUbicacionesConUsuario()
                delay(pollingInterval)
            }
        }
    }

    private fun detenerPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun cargarUbicacionesConUsuario() {
        lifecycleScope.launch {
            try {
                val ubicaciones = RetrofitClient.ubicacionService.obtenerUltimasUbicaciones()
                if (ubicaciones.isEmpty()) {
                    Toast.makeText(this@MapActivity, "No hay ubicaciones para mostrar", Toast.LENGTH_LONG).show()
                    return@launch
                }

                markers.forEach { it.remove() }
                markers.clear()
                markerUbicaciones.clear()
                markerToGuidUsuario.clear()  // Limpiar también el mapa de guids

                val deferreds = ubicaciones.map { alerta ->
                    async {
                        var usuario: Usuario? = null
                        try {
                            if (!alerta._id.isNullOrEmpty()) {
                                usuario = RetrofitClient.usuarioService.obtenerUsuario(alerta._id)
                            }
                        } catch (e: Exception) {
                            usuario = null
                            Log.e("MapActivity", "Error al obtener usuario con id=${alerta._id}: ${e.message}")
                        }
                        Pair(alerta, usuario)
                    }
                }

                val alertasConUsuarios = deferreds.map { it.await() }

                alertasConUsuarios.forEach { (alerta, usuario) ->
                    val latlng = parseUbicacion(alerta.ubicacion)
                    if (latlng.latitude != 0.0 && latlng.longitude != 0.0
                        && !alerta.dato.isNullOrEmpty()
                        && alerta.dato != "Terminar turno"
                    ) {
                        val nombreUsuario = usuario?.let { "${it.nombre} ${it.apellidos}" } ?: "Usuario desconocido"
                        val snippet = alerta.descripcionGenerada ?: "Sin descripción"

                        val colorMarcador = when {
                            "velocidad superad" in alerta.dato.lowercase() -> BitmapDescriptorFactory.HUE_YELLOW
                            "posible caida" in alerta.dato.lowercase() -> BitmapDescriptorFactory.HUE_ORANGE
                            "caida" in alerta.dato.lowercase() -> BitmapDescriptorFactory.HUE_RED
                            else -> BitmapDescriptorFactory.HUE_GREEN
                        }

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(latlng)
                                .title(nombreUsuario)
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(colorMarcador))
                        )
                        marker?.let {
                            markers.add(it)
                            markerUbicaciones[it] = latlng
                            // Guardar el _id del usuario obtenido, no del alerta
                            usuario?._id?.let { idUsuario ->
                                markerToGuidUsuario[it] = idUsuario
                            }
                        }
                    }
                }


            } catch (e: Exception) {
                Toast.makeText(this@MapActivity, "Error al cargar ubicaciones: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun parseUbicacion(u: String?): LatLng {
        if (u.isNullOrEmpty()) return LatLng(0.0, 0.0)
        val m = Regex("""lat=([-0-9.]+), lon=([-0-9.]+)""").find(u)
        return if (m != null) {
            val lat = m.groupValues[1].toDoubleOrNull() ?: 0.0
            val lon = m.groupValues[2].toDoubleOrNull() ?: 0.0
            LatLng(lat, lon)
        } else {
            LatLng(0.0, 0.0)
        }
    }

    private fun showInfoPopup(marker: Marker) {
        val pt = mMap.projection.toScreenLocation(marker.position)
        val popup = layoutInflater.inflate(R.layout.marker_info_popup, null)
        val txt = popup.findViewById<TextView>(R.id.text_info)
        val coords = markerUbicaciones[marker]
        val latLngText = coords?.let { "Lat: ${it.latitude}, Lon: ${it.longitude}" } ?: "Ubicación desconocida"
        txt.text = "Usuario: ${marker.title}\nDescripción: ${marker.snippet}\n$latLngText"
        popup.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            val guidUsuario = markerToGuidUsuario[marker]
            if (guidUsuario != null) {
                intent.putExtra("GUID_USUARIO", guidUsuario)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se encontró el ID de usuario", Toast.LENGTH_SHORT).show()
            }
        }

        popupWindow?.dismiss()
        popupWindow = PopupWindow(popup, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            isOutsideTouchable = true
            isFocusable = true
            popup.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            showAtLocation(
                mapView, Gravity.NO_GRAVITY,
                pt.x - popup.measuredWidth / 2,
                pt.y - popup.measuredHeight - 40
            )
        }
        activeMarker = marker
    }

    private fun hidePopup() {
        popupWindow?.dismiss()
        popupWindow = null
        activeMarker = null
    }

    data class UbicacionAlerta(
        val _id: String?,
        val guidUsuario: String?,
        val dato: String?,
        val ubicacion: String?,
        val descripcionGenerada: String?,
        val fecha: String?,
        val idAlerta: String?
    )

    data class Usuario(
        val _id: String,
        val guid: String,
        val usuario: String,
        val nombre: String,
        val apellidos: String,
        val correo: String?,
        val telefono: String?,
        val domicilio: String?,
        val familiar: String?
    )

    interface UbicacionApiService {
        @GET("api/alertas/ultimas-ubicaciones")
        suspend fun obtenerUltimasUbicaciones(): List<UbicacionAlerta>
    }

    interface UsuarioApiService {
        @GET("api/auth/usuarios/{guid}")
        suspend fun obtenerUsuario(@Path("guid") guid: String): Usuario

        @GET("api/auth/usuarios")
        suspend fun obtenerTodosUsuarios(): List<Usuario>
    }

    object RetrofitClient {
        private const val BASE_URL1 = "https://lanube-280581492272.us-central1.run.app/"
        private const val BASE_URL2 = "https://laniebla-vol2.onrender.com/"
        val ubicacionService: UbicacionApiService by lazy {
            Retrofit.Builder().baseUrl(BASE_URL1)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(UbicacionApiService::class.java)
        }
        val usuarioService: UsuarioApiService by lazy {
            Retrofit.Builder().baseUrl(BASE_URL2)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(UsuarioApiService::class.java)
        }
    }

    private fun agregarMarcador(alerta: UbicacionAlerta) {
        val latlng = parseUbicacion(alerta.ubicacion)
        if (latlng.latitude != 0.0 && latlng.longitude != 0.0 && !alerta.dato.isNullOrEmpty()) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(latlng)
                    .title(alerta.dato)
                    .snippet(alerta.descripcionGenerada ?: "Sin descripción")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            marker?.let {
                markers.add(it)
                markerUbicaciones[it] = latlng
                alerta.guidUsuario?.let { guid ->
                    markerToGuidUsuario[it] = guid
                }
            }
        }
    }
}
