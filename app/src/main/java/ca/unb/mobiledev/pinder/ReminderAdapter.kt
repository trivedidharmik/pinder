package ca.unb.mobiledev.pinder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.pinder.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(private val onItemClick: (Reminder) -> Unit) :
    ListAdapter<Reminder, ReminderAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = getItem(position)
        holder.bind(reminder)
    }

    inner class ReminderViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(reminder: Reminder) {
            binding.apply {
                textViewTitle.text = reminder.title
                textViewDescription.text = reminder.description
                textViewAddress.text = reminder.address

                // Set status indicator color
                val indicatorColor = when (reminder.status) {
                    ReminderStatus.PENDING -> android.R.color.holo_green_light
                    ReminderStatus.COMPLETED -> android.R.color.darker_gray
                    ReminderStatus.EXPIRED -> android.R.color.holo_red_light
                }
                // Create a new colored drawable while maintaining the shape
                val backgroundDrawable = itemView.context.getDrawable(R.drawable.status_indicator_background)?.mutate()
                backgroundDrawable?.setTint(itemView.context.getColor(indicatorColor))
                statusIndicator.background = backgroundDrawable

                // Set status text
                val statusText = when (reminder.status) {
                    ReminderStatus.PENDING -> "Active"
                    ReminderStatus.COMPLETED -> "Completed"
                    ReminderStatus.EXPIRED -> "Expired"
                }
                textViewStatus.text = statusText

                // Optional: Add completion date for completed reminders
                if (reminder.status == ReminderStatus.COMPLETED && reminder.completedAt != null) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val completedDate = dateFormat.format(Date(reminder.completedAt))
                    textViewStatus.text = "Completed on $completedDate"
                }
                root.setOnClickListener { onItemClick(reminder) }
            }
        }
    }

    private class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}