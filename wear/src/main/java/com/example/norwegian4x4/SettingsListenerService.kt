package com.example.norwegian4x4

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives settings changed in the phone app and applies them on the watch.
 * Last write wins: you can still change settings on the watch too.
 */
class SettingsListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != PhoneSync.PATH_SETTINGS) continue
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            Prefs.setMaxHr(this, map.getInt("maxHr", Prefs.getMaxHr(this)))
            Prefs.setIntervals(this, map.getInt("intervals", Prefs.getIntervals(this)))
            Prefs.setScreenOn(this, map.getBoolean("screenOn", Prefs.getScreenOn(this)))
        }
    }
}
