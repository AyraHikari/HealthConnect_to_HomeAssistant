package me.ayra.ha.healthconnect.ui

import android.icu.text.NumberFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import me.ayra.ha.healthconnect.data.SleepStageDuration
import me.ayra.ha.healthconnect.data.SleepStats
import me.ayra.ha.healthconnect.data.StatsData
import me.ayra.ha.healthconnect.data.StepsStats
import me.ayra.ha.healthconnect.data.getStats
import me.ayra.ha.healthconnect.data.saveStats
import me.ayra.ha.healthconnect.databinding.FragmentStatsBinding
import me.ayra.ha.healthconnect.ui.StatsUiModel.Sleep.StageType
import me.ayra.ha.healthconnect.utils.HealthConnectManager
import me.ayra.ha.healthconnect.utils.TimeUtils.toTimeCount
import java.time.Duration
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

    private companion object {
        private const val STEPS_GOAL = 5_000
        private const val AVERAGE_STEP_LENGTH_METERS = 0.762
        private const val CALORIES_PER_STEP = 0.04
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
                GridLayoutManager(requireContext(), 1).apply {
                    orientation = RecyclerView.VERTICAL
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
                    if (cachedStats?.hasData() != true) {
                        showEmptyState(getString(R.string.stats_error_loading))
                    }
                    return@launch
                }

                val heartRateRecords = records["HeartRate"] as? List<*> ?: emptyList<Any?>()
                val sleepRecords = records["SleepSession"] as? List<*> ?: emptyList<Any?>()
                val stepsRecords = records["Steps"] as? List<*> ?: emptyList<Any?>()
                val samples = extractHeartRateSamples(heartRateRecords)
                val sleepStats = extractSleepStats(sleepRecords)
                val stepsStats = extractStepsStats(stepsRecords)
                val statsData = StatsData(heartRate = samples, sleep = sleepStats, steps = stepsStats)
                requireContext().saveStats(statsData)
                updateStats(statsData)
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
            if (cachedStats != null && cachedStats.hasData()) {
                updateStats(cachedStats)
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

    private fun updateStats(statsData: StatsData) {
        binding.permissionMessage.isVisible = false

        val statsItems = buildStatsItems(statsData)
        if (statsItems.isEmpty()) {
            showEmptyState(getString(R.string.stats_no_data))
        } else {
            showStats(statsItems)
        }
    }

    private fun buildStatsItems(statsData: StatsData): List<StatsUiModel> {
        val items = mutableListOf<StatsUiModel>()

        statsData.steps?.let { stepsStats ->
            buildStepsItem(stepsStats)?.let(items::add)
        }

        statsData.sleep?.let { sleepStats ->
            buildSleepItem(sleepStats)?.let(items::add)
        }

        buildHeartRateItem(statsData.heartRate)?.let(items::add)

        if (items.isEmpty()) return emptyList()

        if (items.size == 1) {
            return items.map { item ->
                when (item) {
                    is StatsUiModel.HeartRate -> item.copy(spanSize = 2)
                    is StatsUiModel.Sleep -> item.copy(spanSize = 2)
                    is StatsUiModel.Steps -> item.copy(spanSize = 2)
                }
            }
        }

        val singleSpanItems = items.filter { it.spanSize == 1 }
        if (singleSpanItems.size == 1 && items.size == 2) {
            return items.map { item ->
                when (item) {
                    is StatsUiModel.HeartRate -> item.copy(spanSize = 2)
                    else -> item
                }
            }
        }

        return items
    }

    private fun buildStepsItem(stepsStats: StepsStats): StatsUiModel.Steps? {
        if (stepsStats.totalSteps <= 0L) return null

        val goal = stepsStats.goal.takeIf { it > 0 } ?: STEPS_GOAL
        val totalSteps = stepsStats.totalSteps

        val stepCountText =
            NumberFormat.getIntegerInstance().format(totalSteps.coerceAtLeast(0L))
        val goalText =
            getString(
                R.string.stats_steps_goal_format,
                NumberFormat.getIntegerInstance().format(goal),
            )
        val caloriesText =
            getString(
                R.string.stats_steps_calories_format,
                NumberFormat.getIntegerInstance().format(stepsStats.caloriesBurned.roundToInt()),
            )
        val distanceFormat =
            NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
        val distanceText =
            getString(
                R.string.stats_steps_distance_format,
                distanceFormat.format(stepsStats.distanceKilometers.coerceAtLeast(0.0)),
            )

        val progress = totalSteps.coerceAtMost(goal.toLong()).toInt()

        return StatsUiModel.Steps(
            stepCount = stepCountText,
            goalText = goalText,
            caloriesText = caloriesText,
            distanceText = distanceText,
            progress = progress,
            goal = goal,
        )
    }

    private fun buildHeartRateItem(samples: List<HeartRateSample>): StatsUiModel.HeartRate? {
        if (samples.isEmpty()) return null

        val min = samples.minOf { it.beatsPerMinute }.toInt()
        val max = samples.maxOf { it.beatsPerMinute }.toInt()
        val latest = samples.last()
        val average = samples.map { it.beatsPerMinute }.average().roundToInt()
        val lastRecorded =
            timeFormatter.format(Instant.ofEpochSecond(latest.timestamp).atZone(ZoneId.systemDefault()))

        return StatsUiModel.HeartRate(
            minMaxHeartBeat = getString(R.string.stats_range_format, min, max),
            heartStatus = getString(R.string.stats_heart_rate_average, average),
            heartBeat = latest.beatsPerMinute.toString(),
            lastHeartBeat = lastRecorded,
            iconRes = R.drawable.ic_ecg_heart_24px,
            statusIconRes = R.drawable.ic_check_circle_24px,
        )
    }

    private fun extractStepsStats(records: List<*>): StepsStats? {
        val stepsRecords = records.filterIsInstance<StepsRecord>()
        if (stepsRecords.isEmpty()) return null

        val zoneId = ZoneId.systemDefault()
        val latestRecord = stepsRecords.maxByOrNull { it.endTime } ?: return null
        val targetDate = latestRecord.endTime.atZone(zoneId).toLocalDate()

        val totalSteps =
            stepsRecords
                .filter { it.endTime.atZone(zoneId).toLocalDate() == targetDate }
                .sumOf { it.count }

        if (totalSteps <= 0L) return null

        val distance = totalSteps * AVERAGE_STEP_LENGTH_METERS / 1000.0
        val calories = totalSteps * CALORIES_PER_STEP

        return StepsStats(
            totalSteps = totalSteps,
            distanceKilometers = distance,
            caloriesBurned = calories,
            goal = STEPS_GOAL,
        )
    }

    private fun buildSleepItem(sleepStats: SleepStats): StatsUiModel.Sleep? {
        val totalDuration = sleepStats.totalDurationSeconds
        if (totalDuration <= 0L) return null
        val effectiveSleepDuration =
            if (sleepStats.sleepDurationSeconds > 0L) {
                sleepStats.sleepDurationSeconds
            } else {
                totalDuration
            }

        val sleepPercentage =
            ((effectiveSleepDuration.toFloat() / totalDuration.toFloat()) * 100f)
                .coerceIn(0f, 100f)
        val awakePercentage = (100f - sleepPercentage).coerceAtLeast(0f)
        val qualityText = getString(R.string.stats_sleep_quality, sleepPercentage.roundToInt())
        val sleepTimeText =
            getString(R.string.stats_sleep_duration, effectiveSleepDuration.toTimeCount())

        val restfulStages = sleepStats.stageDurations.filterNot { isAwakeStage(it.stageType) }
        val restfulTotal = restfulStages.sumOf { it.durationSeconds }
        val stagePercentages =
            if (restfulTotal > 0L) {
                restfulStages
                    .map { stage ->
                        val percentage =
                            (stage.durationSeconds.toFloat() / restfulTotal.toFloat()) * 100f
                        StatsUiModel.Sleep.SleepStagePercentage(
                            type = stage.stageType.toStageType(),
                            label = stage.stageType.toStageLabel(),
                            percentage = percentage,
                        )
                    }.filter { it.percentage > 0f }
            } else {
                emptyList()
            }

        return StatsUiModel.Sleep(
            sleepQualityText = qualityText,
            sleepTimeText = sleepTimeText,
            sleepPercentage = sleepPercentage,
            awakePercentage = awakePercentage,
            stagePercentages = stagePercentages,
        )
    }

    private fun extractSleepStats(records: List<*>): SleepStats? {
        val sessions = records.filterIsInstance<SleepSessionRecord>()
        if (sessions.isEmpty()) return null

        val latestSession = sessions.maxByOrNull { it.endTime } ?: return null
        val totalDuration =
            Duration.between(latestSession.startTime, latestSession.endTime).seconds.coerceAtLeast(0L)
        if (totalDuration <= 0L) return null

        val stageDurations = mutableMapOf<Int, Long>()
        var sleepDuration = 0L

        latestSession.stages.forEach { stage ->
            val duration =
                Duration.between(stage.startTime, stage.endTime).seconds.coerceAtLeast(0L)
            if (duration <= 0L) return@forEach

            stageDurations[stage.stage] = stageDurations.getOrDefault(stage.stage, 0L) + duration

            if (!isAwakeStage(stage.stage)) {
                sleepDuration += duration
            }
        }

        val aggregatedStages =
            stageDurations.map { (stageType, duration) ->
                SleepStageDuration(stageType = stageType, durationSeconds = duration)
            }

        val effectiveSleepDuration = sleepDuration.coerceAtMost(totalDuration)

        return SleepStats(
            totalDurationSeconds = totalDuration,
            sleepDurationSeconds = if (effectiveSleepDuration > 0L) effectiveSleepDuration else totalDuration,
            stageDurations = aggregatedStages,
        )
    }

    private fun Int.toStageType(): StageType =
        when (this) {
            SleepSessionRecord.STAGE_TYPE_DEEP -> StageType.DEEP
            SleepSessionRecord.STAGE_TYPE_REM -> StageType.REM
            SleepSessionRecord.STAGE_TYPE_LIGHT -> StageType.LIGHT
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> StageType.SLEEP
            else -> StageType.OTHER
        }

    private fun Int.toStageLabel(): String =
        when (this) {
            SleepSessionRecord.STAGE_TYPE_DEEP -> getString(R.string.stats_sleep_stage_deep)
            SleepSessionRecord.STAGE_TYPE_REM -> getString(R.string.stats_sleep_stage_rem)
            SleepSessionRecord.STAGE_TYPE_LIGHT -> getString(R.string.stats_sleep_stage_light)
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> getString(R.string.stats_sleep_stage_sleep)
            else -> getString(R.string.stats_sleep_stage_other)
        }

    private fun isAwakeStage(stageType: Int): Boolean =
        stageType == SleepSessionRecord.STAGE_TYPE_AWAKE ||
            stageType == SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED ||
            stageType == SleepSessionRecord.STAGE_TYPE_OUT_OF_BED

    private fun StatsData.hasData(): Boolean = heartRate.isNotEmpty() || sleep != null || steps != null

    private fun showStats(items: List<StatsUiModel>) {
        statsAdapter.submitList(items)
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
