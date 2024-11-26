package ca.unb.mobiledev.pinder

enum class ReminderStatus {
    PENDING,
    COMPLETED,
    EXPIRED
}

enum class ReminderPriority {
    LOW,
    MEDIUM,
    HIGH
}

data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)