package me.ayra.ha.healthconnect.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.nicehttp.ResponseParser
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.reflect.KClass

const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"

class Requests(
    allowInsecure: Boolean,
) {
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .apply {
                if (allowInsecure) {
                    val trustAllCerts =
                        arrayOf<TrustManager>(
                            object : X509TrustManager {
                                override fun checkClientTrusted(
                                    chain: Array<out X509Certificate>?,
                                    authType: String?,
                                ) {}

                                override fun checkServerTrusted(
                                    chain: Array<out X509Certificate>?,
                                    authType: String?,
                                ) {}

                                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                            },
                        )
                    val sslContext =
                        SSLContext.getInstance("TLS").apply {
                            init(null, trustAllCerts, SecureRandom())
                        }
                    sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    hostnameVerifier { _, _ -> true }
                }
            }.build()

    var defaultHeaders: Map<String, String> = mapOf("User-Agent" to USER_AGENT)

    private val responseParser =
        object : ResponseParser {
            private val mapper: ObjectMapper =
                jacksonObjectMapper().configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false,
                )

            override fun <T : Any> parse(
                text: String,
                kClass: KClass<T>,
            ): T = mapper.readValue(text, kClass.java)

            override fun <T : Any> parseSafe(
                text: String,
                kClass: KClass<T>,
            ): T? =
                try {
                    mapper.readValue(text, kClass.java)
                } catch (e: Exception) {
                    null
                }

            override fun writeValueAsString(obj: Any): String = mapper.writeValueAsString(obj)
        }

    fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): Response? {
        val request =
            Request
                .Builder()
                .url(url)
                .headers(Headers.Builder().apply { defaultHeaders.plus(headers).forEach { add(it.key, it.value) } }.build())
                .get()
                .build()
        return try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw IllegalStateException("POST request to $url failed: ${e.message}", e)
        }
    }

    fun post(
        url: String,
        json: Any,
        headers: Map<String, String> = defaultHeaders,
    ): Response? {
        val jsonBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), responseParser.writeValueAsString(json))
        val request =
            Request
                .Builder()
                .url(url)
                .headers(Headers.Builder().apply { headers.forEach { add(it.key, it.value) } }.build())
                .post(jsonBody)
                .build()
        return try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw IllegalStateException("POST request to $url failed: ${e.message}", e)
        }
    }
}

var app =
    Requests(allowInsecure = true).apply {
        defaultHeaders = mapOf("User-Agent" to USER_AGENT)
    }
