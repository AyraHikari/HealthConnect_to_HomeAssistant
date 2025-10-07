package me.ayra.ha.healthconnect.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.color.MaterialColors
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.databinding.ItemStatHeartRateBinding
import me.ayra.ha.healthconnect.databinding.ItemStatSleepBinding
import me.ayra.ha.healthconnect.databinding.ItemStatStepsBinding
import com.google.android.material.R as MaterialR

class StatsAdapter : ListAdapter<StatsUiModel, RecyclerView.ViewHolder>(StatsDiffCallback()) {
    private var selectedItemId: String? = null

    fun refreshAllCards() {
        if (currentList.isEmpty()) return

        currentList.indices.forEach { index ->
            notifyItemChanged(index)
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is StatsUiModel.HeartRate -> VIEW_TYPE_HEART_RATE
            is StatsUiModel.Sleep -> VIEW_TYPE_SLEEP
            is StatsUiModel.Steps -> VIEW_TYPE_STEPS
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_HEART_RATE ->
                HeartRateViewHolder(
                    ItemStatHeartRateBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                    ::onItemSelected,
                )
            VIEW_TYPE_SLEEP ->
                SleepViewHolder(
                    ItemStatSleepBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                    ::onItemSelected,
                )
            VIEW_TYPE_STEPS ->
                StepsViewHolder(
                    ItemStatStepsBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                    ::onItemSelected,
                )

            else -> error("Unknown view type: $viewType")
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            val item = getItem(position)
            val isSelected = item.id == selectedItemId
            when (holder) {
                is HeartRateViewHolder -> holder.updateSelection(isSelected)
                is SleepViewHolder -> holder.updateSelection(isSelected)
                is StepsViewHolder -> holder.updateSelection(isSelected)
            }
            return
        }

        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = getItem(position)) {
            is StatsUiModel.HeartRate ->
                (holder as HeartRateViewHolder).bind(item, item.id == selectedItemId)
            is StatsUiModel.Sleep ->
                (holder as SleepViewHolder).bind(item, item.id == selectedItemId)
            is StatsUiModel.Steps ->
                (holder as StepsViewHolder).bind(item, item.id == selectedItemId)
        }
    }

    class HeartRateViewHolder(
        private val binding: ItemStatHeartRateBinding,
        private val onItemSelected: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentItemId: String? = null

        init {
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { currentItemId?.let(onItemSelected) }
        }

        fun bind(
            item: StatsUiModel.HeartRate,
            isSelected: Boolean,
        ) {
            currentItemId = item.id
            binding.minMaxHeartBeat.text = item.minMaxHeartBeat
            binding.heartStatus.text = item.heartStatus
            binding.heartBeat.text = item.heartBeat
            binding.lastHeartBeat.text = item.lastHeartBeat
            binding.statIcon.setImageResource(item.iconRes)
            binding.statusIcon.setImageResource(item.statusIconRes)
            renderBackgroundChart(
                binding.statCardBackgroundChart,
                item.chartValues,
                MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimary),
                null,
            )
            updateSelection(isSelected)
        }

        fun updateSelection(isSelected: Boolean) {
            binding.root.isSelected = isSelected
            updateCardSelection(
                binding.statCardOverlay,
                binding.statCardBackgroundChart,
                binding.statCardForeground,
                isSelected,
                binding.root,
            )
        }
    }

    class SleepViewHolder(
        private val binding: ItemStatSleepBinding,
        private val onItemSelected: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentItemId: String? = null

        init {
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { currentItemId?.let(onItemSelected) }
        }

        fun bind(
            item: StatsUiModel.Sleep,
            isSelected: Boolean,
        ) {
            currentItemId = item.id
            binding.sleepTime.text = item.sleepTimeText

            renderBackgroundChart(
                binding.statCardBackgroundChart,
                item.backgroundChartValues,
                MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary),
                100f,
            )

            setupQualityChart(item)
            setupStageChart(item)
            updateSelection(isSelected)
        }

        fun updateSelection(isSelected: Boolean) {
            binding.root.isSelected = isSelected
            updateCardSelection(
                binding.statCardOverlay,
                binding.statCardBackgroundChart,
                binding.statCardForeground,
                isSelected,
                binding.root,
            )
        }

        private fun setupQualityChart(item: StatsUiModel.Sleep) {
            val entries =
                buildList {
                    if (item.sleepPercentage > 0f) {
                        add(
                            PieEntry(
                                item.sleepPercentage,
                                binding.root.context.getString(R.string.stats_sleep_chart_sleep_label),
                            ),
                        )
                    }
                    if (item.awakePercentage > 0f) {
                        add(
                            PieEntry(
                                item.awakePercentage,
                                binding.root.context.getString(R.string.stats_sleep_chart_awake_label),
                            ),
                        )
                    }
                }

            if (entries.isEmpty()) {
                binding.sleepQualityChart.clear()
                binding.sleepQualityChart.invalidate()
                return
            }

            val dataSet =
                PieDataSet(entries, null).apply {
                    colors =
                        listOf(
                            MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimary),
                            MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimaryContainer),
                        )
                    sliceSpace = 2f
                    setDrawValues(false)
                }

            val pieData = PieData(dataSet).apply { setDrawValues(false) }

            binding.sleepQualityChart.apply {
                data = pieData
                description.isEnabled = false
                legend.isEnabled = false
                setDrawEntryLabels(false)
                setUsePercentValues(false)
                setTouchEnabled(false)
                isRotationEnabled = false
                setNoDataText("")
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 75f
                transparentCircleRadius = 78f
                invalidate()
            }
        }

        private fun setupStageChart(item: StatsUiModel.Sleep) {
            val sleepShareFactor = (item.sleepPercentage / 100f).coerceIn(0f, 1f)

            val stageEntriesWithColors =
                item.stagePercentages
                    .mapNotNull { stage ->
                        val scaledValue = stage.percentage * sleepShareFactor
                        if (scaledValue <= 0f) {
                            null
                        } else {
                            PieEntry(scaledValue, stage.label) to getStageColor(stage.type)
                        }
                    }

            val awakeEntry =
                item.awakePercentage.takeIf { it > 0f }?.let {
                    PieEntry(
                        it,
                        binding.root.context.getString(R.string.stats_sleep_chart_awake_label),
                    ) to MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimaryContainer)
                }

            val entriesWithColors =
                buildList {
                    addAll(stageEntriesWithColors)
                    awakeEntry?.let { add(it) }
                }

            if (entriesWithColors.isEmpty()) {
                binding.sleepTimeChart.clear()
                binding.sleepTimeChart.invalidate()
                return
            }

            val entries = entriesWithColors.map { it.first }
            val colors = entriesWithColors.map { it.second }

            val dataSet =
                PieDataSet(entries, null).apply {
                    this.colors = colors
                    sliceSpace = 1.5f
                    setDrawValues(false)
                }

            val pieData = PieData(dataSet).apply { setDrawValues(false) }

            binding.sleepTimeChart.apply {
                data = pieData
                description.isEnabled = false
                legend.isEnabled = false
                setDrawEntryLabels(false)
                setTouchEnabled(false)
                isRotationEnabled = false
                setNoDataText("")
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 55f
                transparentCircleRadius = 58f
                invalidate()
            }
        }

        private fun getStageColor(stageType: StatsUiModel.Sleep.StageType): Int =
            when (stageType) {
                StatsUiModel.Sleep.StageType.DEEP ->
                    MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary)
                StatsUiModel.Sleep.StageType.REM ->
                    MaterialColors.getColor(binding.root, MaterialR.attr.colorSecondary)
                StatsUiModel.Sleep.StageType.LIGHT ->
                    MaterialColors.getColor(binding.root, MaterialR.attr.colorPrimary)
                StatsUiModel.Sleep.StageType.SLEEP ->
                    MaterialColors.getColor(binding.root, MaterialR.attr.colorTertiary)
                StatsUiModel.Sleep.StageType.OTHER ->
                    MaterialColors.getColor(binding.root, MaterialR.attr.colorSurface)
            }
    }

    class StepsViewHolder(
        private val binding: ItemStatStepsBinding,
        private val onItemSelected: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentItemId: String? = null

        init {
            binding.root.isClickable = true
            binding.root.isFocusable = true
            binding.root.setOnClickListener { currentItemId?.let(onItemSelected) }
        }

        fun bind(
            item: StatsUiModel.Steps,
            isSelected: Boolean,
        ) {
            currentItemId = item.id
            binding.stepCount.text = item.stepCount
            binding.stepGoal.text = item.goalText
            binding.stepCalories.text = item.caloriesText
            binding.stepDistance.text = item.distanceText
            binding.stepProgress.max = item.goal
            binding.stepProgress.setProgressCompat(item.progress, true)
            renderBackgroundChart(
                binding.statCardBackgroundChart,
                item.chartValues,
                MaterialColors.getColor(binding.root, MaterialR.attr.colorSecondary),
                item.goal.toFloat(),
            )
            updateSelection(isSelected)
        }

        fun updateSelection(isSelected: Boolean) {
            binding.root.isSelected = isSelected
            updateCardSelection(
                binding.statCardOverlay,
                binding.statCardBackgroundChart,
                binding.statCardForeground,
                isSelected,
                binding.root,
            )
        }
    }

    companion object {
        private const val VIEW_TYPE_HEART_RATE = 0
        private const val VIEW_TYPE_SLEEP = 1
        private const val VIEW_TYPE_STEPS = 2
        private const val PAYLOAD_SELECTION = "payload_selection"
        private const val SELECTION_ANIMATION_DURATION = 250L
        private const val SELECTED_OVERLAY_ALPHA = 0.1f
        private const val UNSELECTED_OVERLAY_ALPHA = 1f
        private const val SELECTED_CHART_ALPHA = 1f
        private const val UNSELECTED_CHART_ALPHA = 0.1f

        private fun renderBackgroundChart(
            chart: LineChart,
            values: List<Float>,
            color: Int,
            maxValue: Float?,
        ) {
            if (values.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return
            }

            val entries =
                values.mapIndexed { index, value -> Entry(index.toFloat(), value) }

            val dataSet =
                LineDataSet(entries, null).apply {
                    this.color = color
                    lineWidth = 2f
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawCircles(false)
                    setDrawValues(false)
                    setDrawFilled(true)
                    fillColor = ColorUtils.setAlphaComponent(color, 120)
                    fillAlpha = 120
                    highLightColor = Color.TRANSPARENT
                }

            val lineData = LineData(dataSet).apply { setDrawValues(false) }

            chart.apply {
                data = lineData
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                isDragEnabled = false
                setPinchZoom(false)
                setNoDataText("")
                axisLeft.apply {
                    isEnabled = false
                    axisMinimum = 0f
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    val maxEntryValue = entries.maxOfOrNull { it.y } ?: 0f
                    val targetMax = maxValue?.let { maxOf(it, maxEntryValue) } ?: maxEntryValue
                    val computedMax =
                        if (targetMax > 0f) {
                            if (maxValue == null) targetMax * 1.05f else targetMax
                        } else {
                            1f
                        }
                    axisMaximum = computedMax
                }
                axisRight.isEnabled = false
                xAxis.apply {
                    isEnabled = false
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                }
                setViewPortOffsets(0f, 0f, 0f, 0f)
                invalidate()
            }
        }

        private fun updateCardSelection(
            overlayView: View,
            backgroundView: View,
            foregroundView: View,
            isSelected: Boolean,
            itemView: View,
        ) {
            val overlayTarget =
                if (isSelected) SELECTED_OVERLAY_ALPHA else UNSELECTED_OVERLAY_ALPHA
            val backgroundTarget =
                if (isSelected) SELECTED_CHART_ALPHA else UNSELECTED_CHART_ALPHA
            val animate = itemView.isAttachedToWindow

            overlayView.animate().cancel()
            backgroundView.animate().cancel()
            foregroundView.animate().cancel()

            if (animate) {
                overlayView
                    .animate()
                    .alpha(overlayTarget)
                    .setDuration(SELECTION_ANIMATION_DURATION)
                    .start()
                backgroundView
                    .animate()
                    .alpha(backgroundTarget)
                    .setDuration(SELECTION_ANIMATION_DURATION)
                    .start()
                foregroundView
                    .animate()
                    .alpha(overlayTarget)
                    .setDuration(SELECTION_ANIMATION_DURATION)
                    .start()
            } else {
                overlayView.alpha = overlayTarget
                backgroundView.alpha = backgroundTarget
                foregroundView.alpha = overlayTarget
            }
        }
    }

    private fun onItemSelected(itemId: String) {
        val previousId = selectedItemId
        selectedItemId = if (previousId == itemId) null else itemId

        previousId?.let { id ->
            val previousIndex = currentList.indexOfFirst { it.id == id }
            if (previousIndex != -1) {
                notifyItemChanged(previousIndex, PAYLOAD_SELECTION)
            }
        }

        val newId = selectedItemId
        if (newId != null) {
            val newIndex = currentList.indexOfFirst { it.id == newId }
            if (newIndex != -1) {
                notifyItemChanged(newIndex, PAYLOAD_SELECTION)
            }
        }
    }
}

sealed class StatsUiModel(
    open val id: String,
    open val spanSize: Int = 1,
) {
    data class HeartRate(
        override val id: String = "heart_rate",
        val minMaxHeartBeat: String,
        val heartStatus: String,
        val heartBeat: String,
        val lastHeartBeat: String,
        @DrawableRes val iconRes: Int,
        @DrawableRes val statusIconRes: Int,
        val chartValues: List<Float> = emptyList(),
        override val spanSize: Int = 1,
    ) : StatsUiModel(id, spanSize)

    data class Sleep(
        override val id: String = "sleep",
        val sleepTimeText: String,
        val sleepPercentage: Float,
        val awakePercentage: Float,
        val stagePercentages: List<SleepStagePercentage>,
        val backgroundChartValues: List<Float> = emptyList(),
        override val spanSize: Int = 2,
    ) : StatsUiModel(id, spanSize) {
        data class SleepStagePercentage(
            val type: StageType,
            val label: String,
            val percentage: Float,
        )

        enum class StageType {
            DEEP,
            REM,
            LIGHT,
            SLEEP,
            OTHER,
        }
    }

    data class Steps(
        override val id: String = "steps",
        val stepCount: String,
        val goalText: String,
        val caloriesText: String,
        val distanceText: String,
        val progress: Int,
        val goal: Int,
        val chartValues: List<Float>,
        override val spanSize: Int = 1,
    ) : StatsUiModel(id, spanSize)
}

private class StatsDiffCallback : DiffUtil.ItemCallback<StatsUiModel>() {
    override fun areItemsTheSame(
        oldItem: StatsUiModel,
        newItem: StatsUiModel,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: StatsUiModel,
        newItem: StatsUiModel,
    ): Boolean = oldItem == newItem
}
