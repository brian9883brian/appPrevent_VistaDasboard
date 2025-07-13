package com.example.dash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.content.Intent

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Coordenadas aproximadas dentro del estado de Hidalgo
        val puntosRojos = listOf(
            LatLng(20.0971, -98.7622), // Pachuca
            LatLng(20.0831, -98.3601), // Tulancingo
            LatLng(20.2113, -98.5005), // Actopan
            LatLng(20.2726, -98.9382), // Ixmiquilpan
            LatLng(20.4533, -98.9984), // Zacualtipán
            LatLng(20.0923, -99.2151), // Tlahuelilpan
            LatLng(20.0152, -98.9822), // Mixquiahuala
            LatLng(20.0534, -99.3430), // Tula
            LatLng(20.3341, -99.1912), // Tasquillo
            LatLng(20.1749, -98.3955)  // Cuautepec
        )

        val puntosVerdes = listOf(
            LatLng(20.0705, -98.7589),
            LatLng(20.1001, -98.8200),
            LatLng(20.0504, -99.0200),
            LatLng(20.2051, -98.6502),
            LatLng(20.2903, -98.8900),
            LatLng(20.1500, -99.1000),
            LatLng(20.2300, -98.7200),
            LatLng(20.3100, -98.5400),
            LatLng(20.2500, -98.4400),
            LatLng(20.1800, -98.5800)
        )

        // Agregar marcadores rojos
        for ((i, punto) in puntosRojos.withIndex()) {
            mMap.addMarker(
                MarkerOptions()
                    .position(punto)
                    .title("Rojo ${i + 1}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // Agregar marcadores verdes
        for ((i, punto) in puntosVerdes.withIndex()) {
            mMap.addMarker(
                MarkerOptions()
                    .position(punto)
                    .title("Verde ${i + 1}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
        mMap.setOnMarkerClickListener { marker ->
            val intent = Intent(this, InfoActivity::class.java)
            // Puedes pasar datos extra si lo necesitas
            intent.putExtra("titulo", marker.title)
            startActivity(intent)
            true
        }

        // Centrar la cámara en el centro del estado de Hidalgo
        val centroHidalgo = LatLng(20.2, -98.8)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centroHidalgo, 9.5f))
    }


}
