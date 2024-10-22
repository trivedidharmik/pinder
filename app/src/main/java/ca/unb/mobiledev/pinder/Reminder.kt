package ca.unb.mobiledev.pinder

data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
)