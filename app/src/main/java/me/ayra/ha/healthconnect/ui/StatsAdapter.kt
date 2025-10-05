package me.ayra.ha.healthconnect.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.color.MaterialColors
import me.ayra.ha.healthconnect.R
import me.ayra.ha.healthconnect.databinding.ItemStatHeartRateBinding
import me.ayra.ha.healthconnect.databinding.ItemStatSleepBinding
import com.google.android.material.R as MaterialR

class StatsAdapter : ListAdapter<StatsUiModel, RecyclerView.ViewHolder>(StatsDiffCallback()) {
    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is StatsUiModel.HeartRate -> VIEW_TYPE_HEART_RATE
            is StatsUiModel.Sleep -> VIEW_TYPE_SLEEP
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
                )
            VIEW_TYPE_SLEEP ->
                SleepViewHolder(
                    ItemStatSleepBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )

            else -> error("Unknown view type: $viewType")
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = getItem(position)) {
            is StatsUiModel.HeartRate -> (holder as HeartRateViewHolder).bind(item)
            is StatsUiModel.Sleep -> (holder as SleepViewHolder).bind(item)
        }
    }

    class HeartRateViewHolder(
        private val binding: ItemStatHeartRateBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StatsUiModel.HeartRate) {
            binding.minMaxHeartBeat.text = item.minMaxHeartBeat
            binding.heartStatus.text = item.heartStatus
            binding.heartBeat.text = item.heartBeat
            binding.lastHeartBeat.text = item.lastHeartBeat
            binding.statIcon.setImageResource(item.iconRes)
            binding.statusIcon.setImageResource(item.statusIconRes)
        }
    }

    class SleepViewHolder(
        private val binding: ItemStatSleepBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StatsUiModel.Sleep) {
            binding.sleepQuality.text = item.sleepQualityText
            binding.sleepTime.text = item.sleepTimeText

            setupQualityChart(item)
            setupStageChart(item)
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
            val entries =
                item.stagePercentages
                    .filter { it.percentage > 0f }
                    .map { PieEntry(it.percentage, it.label) }

            if (entries.isEmpty()) {
                binding.sleepTimeChart.clear()
                binding.sleepTimeChart.invalidate()
                return
            }

            val colors =
                item.stagePercentages
                    .filter { it.percentage > 0f }
                    .map { getStageColor(it.type) }

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

    companion object {
        private const val VIEW_TYPE_HEART_RATE = 0
        private const val VIEW_TYPE_SLEEP = 1
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
        override val spanSize: Int = 1,
    ) : StatsUiModel(id, spanSize)

    data class Sleep(
        override val id: String = "sleep",
        val sleepQualityText: String,
        val sleepTimeText: String,
        val sleepPercentage: Float,
        val awakePercentage: Float,
        val stagePercentages: List<SleepStagePercentage>,
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
