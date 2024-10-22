package ca.unb.mobiledev.pinder.data

import ca.unb.mobiledev.pinder.Reminder

object ReminderMapper {
    fun toEntity(reminder: Reminder) = ReminderEntity(
        id = reminder.id,
        title = reminder.title,
        description = reminder.description,
        address = reminder.address,
        latitude = reminder.latitude,
        longitude = reminder.longitude,
        radius = reminder.radius
    )

    fun fromEntity(entity: ReminderEntity) = Reminder(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        address = entity.address,
        latitude = entity.latitude,
        longitude = entity.longitude,
        radius = entity.radius
    )

    fun fromEntityList(entities: List<ReminderEntity>) = entities.map(::fromEntity)
}