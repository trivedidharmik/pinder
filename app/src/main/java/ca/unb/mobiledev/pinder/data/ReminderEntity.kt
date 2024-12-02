package ca.unb.mobiledev.pinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val geofenceType: String = "ARRIVE_AT",
    val status: String = "PENDING",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)