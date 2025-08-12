package com.example.dash

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class InfoActivity : AppCompatActivity() {

    private lateinit var tvCaida: TextView
    private lateinit var tvVelocidadSuperada: TextView
    private lateinit var tvPosibleCaida: TextView
    private lateinit var barChart: BarChart
    private lateinit var scatterChart: ScatterChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        scatterChart = findViewById(R.id.scatterChart)
        tvCaida = findViewById(R.id.tv_caida)
        tvVelocidadSuperada = findViewById(R.id.tv_velocidad_superada)
        tvPosibleCaida = findViewById(R.id.tv_posible_caida)
        barChart = findViewById(R.id.barChart)

        cargarDatos()
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                val alertas = RetrofitClient.alertaService.obtenerAlertas()

                // Mostrar datos en texto
                tvCaida.text = "CAIDA: ${alertas.CAIDA}"
                tvVelocidadSuperada.text = "VELOCIDAD SUPERADA: ${alertas.VELOCIDAD_SUPERADA}"
                tvPosibleCaida.text = "POSIBLE CAIDA: ${alertas.POSIBLE_CAIDA}"

                // Graficar barras
                mostrarGraficaBarras(
                    alertas.CAIDA.toFloat(),
                    alertas.VELOCIDAD_SUPERADA.toFloat(),
                    alertas.POSIBLE_CAIDA.toFloat()
                )

                // Para la gráfica de puntos, aquí necesitarías otro endpoint
                // que te devuelva los valores históricos de velocidades.
                // Ejemplo ficticio:
                val velocidades = listOf(6.3f, 5.8f, 7.1f, 4.9f, 6.7f)
                mostrarGraficaPuntos(velocidades)

            } catch (e: Exception) {
                Toast.makeText(this@InfoActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun mostrarGraficaBarras(caida: Float, velocidad: Float, posibleCaida: Float) {
        val entries = listOf(
            BarEntry(0f, caida),
            BarEntry(1f, velocidad),
            BarEntry(2f, posibleCaida)
        )

        val dataSet = BarDataSet(entries, "Alertas Totales").apply {
            colors = listOf(Color.RED, Color.YELLOW, Color.MAGENTA)
            valueTextColor = Color.WHITE
            valueTextSize = 14f
        }

        val data = BarData(dataSet)
        barChart.data = data

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.granularity = 1f
        xAxis.labelCount = 3
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
            listOf("Caída", "Velocidad", "Posible Caída")
        )

        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.textColor = Color.WHITE
        barChart.setBackgroundColor(Color.parseColor("#1E2733"))
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun mostrarGraficaPuntos(velocidades: List<Float>) {
        val entries = velocidades.mapIndexed { index, vel ->
            Entry(index.toFloat(), vel)
        }

        val dataSet = ScatterDataSet(entries, "Variación de Velocidades").apply {
            color = Color.CYAN
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            scatterShapeSize = 10f
        }

        val data = ScatterData(dataSet)
        scatterChart.data = data

        val xAxis = scatterChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(false)

        scatterChart.axisLeft.textColor = Color.WHITE
        scatterChart.axisRight.isEnabled = false
        scatterChart.description.isEnabled = false
        scatterChart.legend.textColor = Color.WHITE
        scatterChart.setBackgroundColor(Color.parseColor("#1E2733"))
        scatterChart.animateY(1000)
        scatterChart.invalidate()
    }

    // Retrofit
    interface AlertaApiService {
        @GET("api/alertas/eventos/3f6ac224-179a-4f10-b733-c342b1b90bb3")
        suspend fun obtenerAlertas(): AlertasResponse
    }

    data class AlertasResponse(
        val CAIDA: Int,
        val POSIBLE_CAIDA: Int,
        val VELOCIDAD_SUPERADA: Int,
        val IR_AL_BANIO: Int,
        val HORA_DE_COMIDA: Int,
        val TERMINAR_TURNO: Int
    )

    object RetrofitClient {
        private const val BASE_URL = "https://lanube-280581492272.us-central1.run.app/"

        val alertaService: AlertaApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AlertaApiService::class.java)
        }
    }
}
