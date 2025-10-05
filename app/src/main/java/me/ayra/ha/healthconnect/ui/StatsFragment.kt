package me.ayra.ha.healthconnect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
import me.ayra.ha.healthconnect.data.Settings.getSyncDays
import me.ayra.ha.healthconnect.data.StatsData
import me.ayra.ha.healthconnect.data.getStats
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
    private lateinit var statsAdapter: StatsAdapter
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
        setupStatsList()
        binding.swipeRefresh.setOnRefreshListener { fetchLatestStats() }
        loadCachedStats()
    }

    override fun onResume() {
        super.onResume()
        loadCachedStats()
        fetchLatestStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchJob?.cancel()
        binding.statsList.adapter = null
        _binding = null
    }

    private fun setupStatsList() {
        statsAdapter = StatsAdapter()
        binding.statsList.apply {
            layoutManager =
                GridLayoutManager(requireContext(), 2).apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int = statsAdapter.currentList.getOrNull(position)?.spanSize ?: 1
                        }
                }
            adapter = statsAdapter
        }
    }

    private fun fetchLatestStats() {
        fetchJob?.cancel()
        fetchJob =
            viewLifecycleOwner.lifecycleScope.launch {
                binding.permissionMessage.isVisible = false
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.swipeRefresh.isRefreshing = true
                }

                if (!healthConnectManager.hasAllPermissions()) {
                    binding.swipeRefresh.isRefreshing = false
                    statsAdapter.submitList(emptyList())
                    binding.permissionMessage.isVisible = true
                    binding.statsList.isVisible = false
                    binding.emptyState.isVisible = false
                    return@launch
                }

                val records =
                    withContext(Dispatchers.IO) {
                        runCatching { healthConnectManager.getAll(requireContext().getSyncDays()) }
                            .getOrNull()
                    }

                binding.swipeRefresh.isRefreshing = false

                if (records == null) {
                    val cachedStats = requireContext().getStats()
                    if (cachedStats?.heartRate.isNullOrEmpty()) {
                        showEmptyState(getString(R.string.stats_error_loading))
                    }
                    return@launch
                }

                val heartRateRecords = records["HeartRate"] as? List<*> ?: emptyList<Any?>()
                val samples = extractHeartRateSamples(heartRateRecords)
                requireContext().saveStats(StatsData(heartRate = samples))
                updateStats(samples)
            }
    }

    private fun loadCachedStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val hasPermissions = healthConnectManager.hasAllPermissions()
            if (!hasPermissions) {
                statsAdapter.submitList(emptyList())
                binding.permissionMessage.isVisible = true
                binding.statsList.isVisible = false
                binding.emptyState.isVisible = false
                return@launch
            }

            val cachedStats = requireContext().getStats()
            if (cachedStats != null) {
                updateStats(cachedStats.heartRate)
            } else {
                showEmptyState(getString(R.string.stats_pull_to_refresh_hint))
            }
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

    private fun updateStats(samples: List<HeartRateSample>) {
        binding.permissionMessage.isVisible = false

        val statsItems = buildHeartRateItems(samples)
        if (statsItems.isEmpty()) {
            showEmptyState(getString(R.string.stats_no_heart_rate_data))
        } else {
            showStats(statsItems)
        }
    }

    private fun buildHeartRateItems(samples: List<HeartRateSample>): List<StatsUiModel> {
        if (samples.isEmpty()) return emptyList()

        val min = samples.minOf { it.beatsPerMinute }.toInt()
        val max = samples.maxOf { it.beatsPerMinute }.toInt()
        val latest = samples.last()
        val average = samples.map { it.beatsPerMinute }.average().roundToInt()
        val lastRecorded =
            timeFormatter.format(Instant.ofEpochSecond(latest.timestamp).atZone(ZoneId.systemDefault()))

        val heartRateItem =
            StatsUiModel.HeartRate(
                minMaxHeartBeat = getString(R.string.stats_range_format, min, max),
                heartStatus = getString(R.string.stats_heart_rate_average, average),
                heartBeat = latest.beatsPerMinute.toString(),
                lastHeartBeat = lastRecorded,
                iconRes = R.drawable.ic_ecg_heart_24px,
                statusIconRes = R.drawable.ic_check_circle_24px,
            )

        return listOf(heartRateItem)
    }

    private fun showStats(items: List<StatsUiModel>) {
        val adjustedItems =
            if (items.size == 1) {
                items.map {
                    when (it) {
                        is StatsUiModel.HeartRate -> it.copy(spanSize = 2)
                    }
                }
            } else {
                items
            }

        statsAdapter.submitList(adjustedItems)
        binding.statsList.isVisible = true
        binding.emptyState.isVisible = false
    }

    private fun showEmptyState(message: String) {
        statsAdapter.submitList(emptyList())
        binding.statsList.isVisible = false
        binding.emptyState.text = message
        binding.emptyState.isVisible = true
    }
}
