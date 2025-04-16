package me.ayra.ha.healthconnect.data

import android.content.Context
import me.ayra.ha.healthconnect.utils.DataStore.getKey
import me.ayra.ha.healthconnect.utils.DataStore.removeKey
import me.ayra.ha.healthconnect.utils.DataStore.setKey
import me.ayra.ha.healthconnect.utils.DataStore.toJson
import me.ayra.ha.healthconnect.utils.DataStore.tryParseJson

const val DATA = "DATA"

object Settings {
    data class SyncError(
        val timestamp: Long,
        val message: String
    )

    fun Context.getSettings(path: String): String? {
        return getKey<String>(DATA, path)
    }
    fun Context.setSettings(path: String, value: String) {
        return setKey(DATA, path, value)
    }

    fun Context.getLastSync(): Long? {
        return getKey<Long>(DATA, "lastSync")
    }
    fun Context.setLastSync(value: Long) {
        return setKey(DATA, "lastSync", value)
    }

    fun Context.getLastError(): SyncError? {
        return tryParseJson(getKey<String>(DATA, "lastError"))
    }
    fun Context.setLastError(value: SyncError) {
        return setKey(DATA, "lastError", value.toJson())
    }
    fun Context.removeLastError() {
        removeKey(DATA, "lastError")
    }

    fun Context.getAutoSync(): Boolean? {
        return getKey<Boolean>(DATA, "AutoSync", true)
    }
    fun Context.setAutoSync(value: Boolean) {
        return setKey(DATA, "AutoSync", value)
    }
}