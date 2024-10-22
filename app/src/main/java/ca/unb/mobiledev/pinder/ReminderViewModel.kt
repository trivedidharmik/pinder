package ca.unb.mobiledev.pinder

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import ca.unb.mobiledev.pinder.data.ReminderDatabase
import ca.unb.mobiledev.pinder.data.ReminderMapper
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.catch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ReminderViewModel"
    private val database = ReminderDatabase.getDatabase(application)
    private val reminderDao = database.reminderDao()

    private val _reminders = reminderDao.getAllReminders()
        .catch { exception ->
            Log.e(TAG, "Error fetching reminders: ${exception.message}")
            emit(emptyList())
        }
        .map { entities ->
            Log.d(TAG, "Fetched ${entities.size} reminders")
            ReminderMapper.fromEntityList(entities)
        }
    val reminders = _reminders.asLiveData(viewModelScope.coroutineContext)

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Adding reminder: $reminder")
                val entity = ReminderMapper.toEntity(reminder)
                reminderDao.insertReminder(entity)
                Log.d(TAG, "Successfully added reminder")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding reminder: ${e.message}")
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating reminder: $reminder")
                val entity = ReminderMapper.toEntity(reminder)
                reminderDao.updateReminder(entity)
                Log.d(TAG, "Successfully updated reminder")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating reminder: ${e.message}")
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderDao.getReminderById(reminderId)?.let {
                reminderDao.deleteReminder(it)
            }
        }
    }

    fun getReminder(reminderId: Long): LiveData<Reminder?> = liveData(viewModelScope.coroutineContext) {
        try {
            val entity = reminderDao.getReminderById(reminderId)
            emit(entity?.let { ReminderMapper.fromEntity(it) })
        } catch (e: Exception) {
            // Handle any errors here
            emit(null)
        }
    }
}

class ReminderViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}