package com.example.ui

import android.app.Application
import android.content.Context
import android.app.NotificationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository
    private val scheduler: AlarmScheduler

    val allAlarms: StateFlow<List<Alarm>>
    
    val ringingAlarmId = MutableStateFlow<Int?>(null)
    val ringingAlarmUri = MutableStateFlow<String?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AlarmRepository(database.alarmDao())
        scheduler = AlarmScheduler(application)

        allAlarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addAlarm(hour: Int, minute: Int, ringtoneUri: String?) {
        viewModelScope.launch {
            val autoCreatedId = repository.insert(Alarm(hour = hour, minute = minute, ringtoneUri = ringtoneUri, isEnabled = true))
            val newAlarm = Alarm(id = autoCreatedId.toInt(), hour = hour, minute = minute, ringtoneUri = ringtoneUri, isEnabled = true)
            scheduler.schedule(newAlarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteById(alarm.id)
        }
    }

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = isEnabled)
            repository.update(updated)
            if (isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun triggerAlarmRinging(alarmId: Int, uri: String?) {
        ringingAlarmId.value = alarmId
        ringingAlarmUri.value = uri
    }

    fun dismissRinging() {
        val id = ringingAlarmId.value
        if (id != null) {
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)

            val alarm = allAlarms.value.find { it.id == id }
            if (alarm != null) {
                toggleAlarm(alarm, false)
            }
        }
        
        ringingAlarmId.value = null
        ringingAlarmUri.value = null
    }
}
