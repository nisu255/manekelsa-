package com.manekelsa.app.ui.directory

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manekelsa.app.R
import com.manekelsa.app.data.model.Worker
import com.manekelsa.app.databinding.ItemWorkerBinding

class WorkerAdapter(
    private val onThumbsUp: (Worker) -> Unit,
    private val onAvailabilityToggle: (Worker, Boolean) -> Unit,
    private val currentUserId: String,
    var isKannada: Boolean
) : ListAdapter<Worker, WorkerAdapter.WorkerViewHolder>(WorkerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkerViewHolder(private val binding: ItemWorkerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(worker: Worker) {
            binding.apply {

                // ✅ NAME (SAFE FALLBACK)
                val displayName = if (isKannada) {
                    worker.nameKn.takeIf { it.isNotBlank() } ?: worker.nameEn
                } else {
                    worker.nameEn.takeIf { it.isNotBlank() } ?: worker.nameKn
                }

                // ✅ SKILL
                val displaySkill = if (isKannada) {
                    worker.skillKn.takeIf { it.isNotBlank() } ?: worker.skillEn
                } else {
                    worker.skillEn.takeIf { it.isNotBlank() } ?: worker.skillKn
                }

                // ✅ AREA
                val displayArea = if (isKannada) {
                    worker.areaKn.takeIf { it.isNotBlank() } ?: worker.areaEn
                } else {
                    worker.areaEn.takeIf { it.isNotBlank() } ?: worker.areaKn
                }

                // ✅ TEXT SET
                tvWorkerName.text = displayName
                tvSkill.text = "${worker.skillIcon} $displaySkill"

                // ✅ DISTANCE FIX (NO INFINITY BUG)
                val distanceText = when {
                    worker.distance == Double.MAX_VALUE -> "--"
                    worker.distance <= 0.0 -> "--"
                    worker.distance.isNaN() -> "--"
                    else -> String.format("%.1f km", worker.distance)
                }

                tvArea.text = "📍 $displayArea · $distanceText"

                // ✅ RATE FIX (avoid ₹₹ bug)
                val rateText = worker.rate.replace("₹", "")
                tvRate.text = "₹$rateText/day"

                tvRatingCount.text = worker.rating.toString()

                // ✅ AVATAR
                tvAvatarInitial.text = displayName
                    .firstOrNull()
                    ?.uppercase()
                    ?: "?"

                // ✅ AVAILABILITY (CORRECT FIELD)
                if (worker.isAvailable) {
                    tvAvailBadge.text =
                        if (isKannada) "● ಇಂದು ಲಭ್ಯ" else "● Available Today"
                    tvAvailBadge.setBackgroundResource(R.drawable.bg_available_true)
                    tvAvailBadge.setTextColor(itemView.context.getColor(R.color.available_green))
                } else {
                    tvAvailBadge.text =
                        if (isKannada) "○ ಲಭ್ಯವಿಲ್ಲ" else "○ Not Available"
                    tvAvailBadge.setBackgroundResource(R.drawable.bg_available_false)
                    tvAvailBadge.setTextColor(itemView.context.getColor(R.color.unavailable_red))
                }

                // ✅ CALL BUTTON
                btnCall.setOnClickListener {
                    if (worker.phone.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}"))
                        itemView.context.startActivity(intent)
                    }
                }

                // ✅ THUMBS UP
                llThumbsUp.setOnClickListener {
                    onThumbsUp(worker)
                }

                // ✅ SWITCH (OWNER ONLY)
                if (worker.ownerId == currentUserId && worker.ownerId.isNotBlank()) {

                    switchAvail.visibility = View.VISIBLE

                    // ❗ IMPORTANT: remove old listener before setting state
                    switchAvail.setOnCheckedChangeListener(null)

                    switchAvail.isChecked = worker.isAvailable

                    switchAvail.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != worker.isAvailable) {
                            onAvailabilityToggle(worker, isChecked)
                        }
                    }

                } else {
                    switchAvail.visibility = View.GONE
                }
            }
        }
    }
}

/* ✅ DIFF UTIL */
class WorkerDiffCallback : DiffUtil.ItemCallback<Worker>() {

    override fun areItemsTheSame(oldItem: Worker, newItem: Worker) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Worker, newItem: Worker) =
        oldItem == newItem
}