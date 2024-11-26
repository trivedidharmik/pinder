package ca.unb.mobiledev.pinder

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ReminderViewModel"
    private val database = ReminderDatabase.getDatabase(application)
    private val reminderDao = database.reminderDao()
    private val geofenceHelper = GeofenceHelper(application)

    private val _reminders = reminderDao.getAllReminders()
        .catch { exception ->
            Log.e(TAG, "Error fetching reminders: ${exception.message}")
            emit(emptyList())
        }
        .map { entities ->
            ReminderMapper.fromEntityList(entities)
        }
    val reminders = _reminders.asLiveData()

    fun addReminder(reminder: Reminder, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val entity = ReminderMapper.toEntity(reminder)
                val id = reminderDao.insertReminder(entity)

                val reminderWithId = reminder.copy(id = id)
                geofenceHelper.addGeofence(
                    reminderWithId,
                    onSuccessListener = { onSuccess() },
                    onFailureListener = { exception ->
                        Log.e(TAG, "Failed to add geofence: ${exception.message}")
                        onSuccess() // Still consider it a success as data is saved
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error adding reminder: ${e.message}")
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun updateReminder(reminder: Reminder, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val entity = ReminderMapper.toEntity(reminder)
                reminderDao.updateReminder(entity)

                geofenceHelper.updateGeofence(
                    reminder,
                    onSuccessListener = { onSuccess() },
                    onFailureListener = { exception ->
                        Log.e(TAG, "Failed to update geofence: ${exception.message}")
                        onSuccess() // Still consider it a success as data is saved
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reminder: ${e.message}")
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun deleteReminder(reminderId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val reminder = reminderDao.getReminderById(reminderId)
                if (reminder != null) {
                    reminderDao.deleteReminder(reminder)
                    geofenceHelper.removeGeofence(reminderId.toString())
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting reminder: ${e.message}")
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun getReminder(reminderId: Long): LiveData<Reminder?> = liveData {
        try {
            val entity = reminderDao.getReminderById(reminderId)
            emit(entity?.let { ReminderMapper.fromEntity(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting reminder: ${e.message}")
            emit(null)
        }
    }
}