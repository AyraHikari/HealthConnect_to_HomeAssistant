package me.ayra.ha.healthconnect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.data.DEFAULT_SYNC_DAYS
import me.ayra.ha.healthconnect.data.HeartRateSample
import me.ayra.ha.healthconnect.data.StatsData
import me.ayra.ha.healthconnect.data.saveStats
import me.ayra.ha.healthconnect.databinding.FragmentStatsBinding
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var healthConnectManager: HealthConnectManager
    private var fetchJob: Job? = null

    private val timeFormatter by lazy {
        DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        healthConnectManager = HealthConnectManager(requireContext())
        setupHeartRateChart()
    }

    override fun onResume() {
        super.onResume()
        fetchLatestStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchJob?.cancel()
        _binding = null
    }

    private fun fetchLatestStats() {
        fetchJob?.cancel()
        fetchJob =
            viewLifecycleOwner.lifecycleScope.launch {
                binding.loadingIndicator.isVisible = true
                binding.permissionMessage.isVisible = false
                binding.emptyState.isVisible = false
                binding.heartRateChart.isVisible = false
                binding.heartRateHeader.isVisible = false

                if (!healthConnectManager.hasAllPermissions()) {
                    binding.loadingIndicator.isVisible = false
                    binding.permissionMessage.isVisible = true
                    return@launch
                }

                val records =
                    withContext(Dispatchers.IO) {
                        runCatching { healthConnectManager.getAll(DEFAULT_SYNC_DAYS) }.getOrNull()
                    }

                binding.loadingIndicator.isVisible = false

                if (records == null) {
                    showEmptyState(getString(R.string.stats_error_loading))
                    return@launch
                }

                val heartRateRecords = records["HeartRate"] as? List<*> ?: emptyList<Any?>()
                val samples = extractHeartRateSamples(heartRateRecords)
                requireContext().saveStats(StatsData(heartRate = samples))
                updateHeartRateChart(samples)
            }
    }

    private fun extractHeartRateSamples(records: List<*>): List<HeartRateSample> {
        if (records.isEmpty()) return emptyList()

        return records
            .filterIsInstance<HeartRateRecord>()
            .flatMap { record ->
                record.samples.map { sample ->
                    HeartRateSample(
                        timestamp = sample.time.epochSecond,
                        beatsPerMinute = sample.beatsPerMinute.toLong(),
                    )
                }
            }.sortedBy { it.timestamp }
    }

    private fun setupHeartRateChart() {
        val chart = binding.heartRateChart
        val onBackgroundColor =
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnBackground)
        val primaryColor =
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setNoDataText(getString(R.string.stats_no_heart_rate_data))
        chart.setNoDataTextColor(onBackgroundColor)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = onBackgroundColor
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = onBackgroundColor
            setDrawGridLines(false)
            granularity = 1f
        }
        chart.setTouchEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.extraBottomOffset = 12f
        chart.axisLeft.granularity = 1f
        chart.axisLeft.axisLineColor = primaryColor
        chart.xAxis.axisLineColor = primaryColor
    }

    private fun updateHeartRateChart(samples: List<HeartRateSample>) {
        binding.permissionMessage.isVisible = false
        binding.heartRateHeader.isVisible = true

        if (samples.isEmpty()) {
            binding.heartRateChart.clear()
            binding.heartRateChart.isVisible = false
            showEmptyState(getString(R.string.stats_no_heart_rate_data))
            return
        }

        binding.emptyState.isVisible = false
        binding.heartRateChart.isVisible = true

        val entries =
            samples.mapIndexed { index, sample ->
                Entry(index.toFloat(), sample.beatsPerMinute.toFloat())
            }

        val chart = binding.heartRateChart
        val primaryColor =
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorPrimary)
        val onBackgroundColor =
            MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnBackground)

        val dataSet =
            LineDataSet(entries, getString(R.string.stats_heart_rate_label)).apply {
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = primaryColor
                lineWidth = 2f
                highLightColor = primaryColor
            }

        chart.data = LineData(dataSet)
        chart.axisLeft.textColor = onBackgroundColor
        chart.xAxis.textColor = onBackgroundColor
        chart.xAxis.valueFormatter = HeartRateAxisValueFormatter(samples, timeFormatter)
        chart.xAxis.labelRotationAngle = -30f
        chart.invalidate()
    }

    private fun showEmptyState(message: String) {
        binding.permissionMessage.isVisible = false
        binding.heartRateHeader.isVisible = true
        binding.heartRateChart.clear()
        binding.heartRateChart.isVisible = false
        binding.emptyState.text = message
        binding.emptyState.isVisible = true
    }

    private class HeartRateAxisValueFormatter(
        private val samples: List<HeartRateSample>,
        private val formatter: DateTimeFormatter,
    ) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            if (samples.isEmpty()) return ""
            val index = value.roundToInt().coerceIn(0, samples.lastIndex)
            val instant = Instant.ofEpochSecond(samples[index].timestamp)
            return formatter.format(instant.atZone(ZoneId.systemDefault()))
        }
    }
}
