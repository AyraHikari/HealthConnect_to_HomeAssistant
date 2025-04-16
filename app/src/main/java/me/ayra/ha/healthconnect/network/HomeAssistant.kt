package me.ayra.ha.healthconnect.network

import me.ayra.ha.healthconnect.utils.TimeUtils.unixTimeMs

object HomeAssistant {
    fun checkHomeAssistant(apiUrl: String, apiToken: String): Pair<Boolean, String> {
        try {
            val headers = mapOf(
                "Authorization" to "Bearer $apiToken",
                "Content-Type" to "application/json"
            )

            val request = app.get("$apiUrl/api/", headers = headers)
            return Pair(request?.isSuccessful == true, "${request?.code} ${request?.message}")
        } catch (e: Exception) {
            return Pair(false, e.message.toString())
        }
    }

    fun sendToHomeAssistant(data: MutableMap<String, Any?>, entityId: String, apiUrl: String, apiToken: String): Pair<Boolean, String> {
        try {
            val headers = mapOf(
                "Authorization" to "Bearer $apiToken",
                "Content-Type" to "application/json"
            )

            val request = app.post(
                "$apiUrl/api/states/sensor.$entityId", headers = headers, json =
                    mapOf(
                        "state" to "synced",
                        "last_updated" to unixTimeMs,
                        "attributes" to data
                    )
            )
            return Pair(request?.isSuccessful == true, "${request?.code} ${request?.message}")
        } catch (e: Exception) {
            return Pair(false, e.message.toString())
        }
    }
}