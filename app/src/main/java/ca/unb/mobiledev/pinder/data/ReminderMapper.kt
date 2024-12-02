package ca.unb.mobiledev.pinder.data

import ca.unb.mobiledev.pinder.GeofenceType
import ca.unb.mobiledev.pinder.Reminder
import ca.unb.mobiledev.pinder.ReminderPriority
import ca.unb.mobiledev.pinder.ReminderStatus

object ReminderMapper {
    fun toEntity(reminder: Reminder) = ReminderEntity(
        id = reminder.id,
        title = reminder.title,
        description = reminder.description,
        address = reminder.address,
        latitude = reminder.latitude,
        longitude = reminder.longitude,
        radius = reminder.radius,
        geofenceType = reminder.geofenceType.name,
        status = reminder.status.name,
        priority = reminder.priority.name,
        createdAt = reminder.createdAt,
        completedAt = reminder.completedAt
    )

    fun fromEntity(entity: ReminderEntity) = Reminder(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        address = entity.address,
        latitude = entity.latitude,
        longitude = entity.longitude,
        radius = entity.radius,
        geofenceType = GeofenceType.valueOf(entity.geofenceType),
        status = ReminderStatus.valueOf(entity.status),
        priority = ReminderPriority.valueOf(entity.priority),
        createdAt = entity.createdAt,
        completedAt = entity.completedAt
    )

    fun fromEntityList(entities: List<ReminderEntity>) = entities.map(::fromEntity)
}