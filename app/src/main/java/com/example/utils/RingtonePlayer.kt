package com.example.utils

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

object RingtonePlayer {
    private var currentRingtone: Ringtone? = null

    fun play(context: Context, uriString: String?) {
        stop() // Stop any existing

        val uri = if (uriString.isNullOrEmpty()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            Uri.parse(uriString)
        }

        currentRingtone = RingtoneManager.getRingtone(context, uri)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            currentRingtone?.isLooping = true
        }
        
        currentRingtone?.play()
    }

    fun stop() {
        currentRingtone?.stop()
        currentRingtone = null
    }
}
