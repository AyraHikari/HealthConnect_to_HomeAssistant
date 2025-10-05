package me.ayra.ha.healthconnect.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.ayra.ha.healthconnect.databinding.ItemStatHeartRateBinding

class StatsAdapter : ListAdapter<StatsUiModel, RecyclerView.ViewHolder>(StatsDiffCallback()) {
    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is StatsUiModel.HeartRate -> VIEW_TYPE_HEART_RATE
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

            else -> error("Unknown view type: $viewType")
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = getItem(position)) {
            is StatsUiModel.HeartRate -> (holder as HeartRateViewHolder).bind(item)
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

    companion object {
        private const val VIEW_TYPE_HEART_RATE = 0
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
