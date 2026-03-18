package com.huajuan.aispace.data

import com.huajuan.aispace.HuaJuanApplication
import com.huajuan.aispace.R
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.huajuan.aispace.i18n.LocalizedResources
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ModelErrorParser {
    private const val LEGACY_ERROR_PREFIX_FULL = "\u9519\u8bef\uff1a"
    private const val LEGACY_ERROR_PREFIX_SHORT = "\u9519\u8bef:"
    private const val LEGACY_MISSING_MODEL = "\u672a\u9009\u62e9\u6a21\u578b\uff0c\u8bf7\u5148\u9009\u62e9/\u6dfb\u52a0\u6a21\u578b"
    private const val LEGACY_MISSING_API_KEY_MESSAGE = "\u0041\u0050\u0049\u5bc6\u94a5\u672a\u8bbe\u7f6e\uff0c\u8bf7\u5148\u586b\u5199\u5bc6\u94a5"
    private const val LEGACY_API_KEY_MISSING = "\u0041\u0050\u0049\u5bc6\u94a5\u672a\u8bbe\u7f6e"
    private const val LEGACY_INVALID_API_URL = "\u65e0\u6548\u7684\u0041\u0050\u0049\u5730\u5740"
    private const val LEGACY_EMPTY_RESPONSE = "\u54cd\u5e94\u4e3a\u7a7a"
    private const val LEGACY_EMPTY_RESPONSE_BODY = "\u54cd\u5e94\u4f53\u4e3a\u7a7a"
    private const val LEGACY_NO_CONTENT = "\u65e0\u8fd4\u56de\u5185\u5bb9"
    private const val LEGACY_PARSE_FAILED = "\u89e3\u6790\u54cd\u5e94\u5931\u8d25"
    private const val LEGACY_EMBEDDING_UNSUPPORTED = "\u5f53\u524d\u670d\u52a1\u5546\u4e0d\u652f\u6301\u5411\u91cf\u63a5\u53e3"
    private const val LEGACY_IMAGE_UNSUPPORTED = "\u5f53\u524d\u670d\u52a1\u5546\u4e0d\u652f\u6301\u751f\u56fe\u63a5\u53e3"
    private const val LEGACY_IMAGE_PARSE_FAILED_PREFIX = "\u56fe\u50cf\u751f\u6210\u5b8c\u6210\uff0c\u4f46\u89e3\u6790\u7ed3\u679c\u5931\u8d25\uff1a"
    private const val LEGACY_LOCAL_MODEL_LOAD_FAILED_PREFIX = "\u672c\u5730\u6a21\u578b\u52a0\u8f7d\u5931\u8d25 "

    fun isErrorMessage(message: String?): Boolean {
        val text = message?.trim().orEmpty()
        if (text.isBlank()) return false
        val localizedPrefix = localized(R.string.error_prefix, "").trim()
        return text.startsWith(LEGACY_ERROR_PREFIX_FULL) ||
            text.startsWith(LEGACY_ERROR_PREFIX_SHORT) ||
            text.startsWith(localizedPrefix)
    }

    fun normalizeUserError(message: String?): String {
        val normalized = message
            ?.trim()
            ?.removePrefix(LEGACY_ERROR_PREFIX_FULL)
            ?.removePrefix(LEGACY_ERROR_PREFIX_SHORT)
            ?.trim()
            .orEmpty()
            .ifBlank { localized(R.string.error_request_failed_retry) }
            .let(::localizeKnownMessage)
        return localized(R.string.error_prefix, normalized)
    }

    fun parseThrowable(throwable: Throwable): String {
        val msg = when (throwable) {
            is UnknownHostException -> localized(R.string.error_network_unavailable)
            is SocketTimeoutException -> localized(R.string.error_request_timeout)
            is ConnectException -> localized(R.string.error_connection_failed)
            is SSLException -> localized(R.string.error_ssl_failed)
            is InterruptedIOException -> localized(R.string.error_request_interrupted)
            else -> throwable.message ?: throwable.javaClass.simpleName
        }
        return normalizeUserError(msg)
    }

    fun parseHttpError(code: Int, statusMessage: String?, errorBody: String?): String {
        val detail = extractErrorFromJson(errorBody).orEmpty().ifBlank {
            errorBody
                ?.trim()
                ?.take(180)
                ?.replace('\n', ' ')
                .orEmpty()
        }
        val fallback = when (code) {
            400 -> localized(R.string.error_http_400)
            401 -> localized(R.string.error_http_401)
            403 -> localized(R.string.error_http_403)
            404 -> localized(R.string.error_http_404)
            408 -> localized(R.string.error_http_408)
            413 -> localized(R.string.error_http_413)
            429 -> localized(R.string.error_http_429)
            in 500..599 -> localized(R.string.error_http_5xx)
            else -> statusMessage?.takeIf { it.isNotBlank() } ?: localized(R.string.error_request_failed)
        }
        val merged = if (detail.isNotBlank()) {
            localized(R.string.error_http_with_detail, fallback, code, detail)
        } else {
            localized(R.string.error_http_no_detail, fallback, code)
        }
        return normalizeUserError(merged)
    }

    fun extractErrorFromJson(jsonText: String?): String? {
        if (jsonText.isNullOrBlank()) return null
        return try {
            val element = JsonParser.parseString(jsonText)
            if (!element.isJsonObject) return null
            extractErrorFromObject(element.asJsonObject)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractErrorFromObject(obj: JsonObject): String? {
        val error = obj.get("error")
        if (error != null) {
            if (error.isJsonPrimitive) {
                return error.asString.takeIf { it.isNotBlank() }
            }
            if (error.isJsonObject) {
                val errorObj = error.asJsonObject
                val message = errorObj.get("message")?.asString
                    ?: errorObj.get("detail")?.asString
                    ?: errorObj.get("msg")?.asString
                if (!message.isNullOrBlank()) return message
            }
        }

        obj.get("message")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        obj.get("detail")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        obj.get("msg")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        obj.get("error_message")?.asString?.takeIf { it.isNotBlank() }?.let { return it }

        val errors = obj.getAsJsonArray("errors")
        val first = errors?.firstOrNull()
        if (first != null && first.isJsonObject) {
            first.asJsonObject.get("message")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun localizeKnownMessage(message: String): String {
        return when {
            message == LEGACY_MISSING_MODEL -> localized(R.string.model_requirement_missing_model_message)
            message == LEGACY_MISSING_API_KEY_MESSAGE -> localized(R.string.model_requirement_missing_api_key_message)
            message == LEGACY_API_KEY_MISSING -> localized(R.string.error_api_key_missing)
            message == LEGACY_INVALID_API_URL -> localized(R.string.error_invalid_api_url)
            message == LEGACY_EMPTY_RESPONSE -> localized(R.string.error_empty_response)
            message == LEGACY_EMPTY_RESPONSE_BODY -> localized(R.string.error_empty_response_body)
            message == LEGACY_NO_CONTENT -> localized(R.string.error_no_content)
            message == LEGACY_PARSE_FAILED -> localized(R.string.error_parse_response_failed)
            message == LEGACY_EMBEDDING_UNSUPPORTED -> localized(R.string.error_embedding_not_supported)
            message == LEGACY_IMAGE_UNSUPPORTED -> localized(R.string.error_image_generation_not_supported)
            message.startsWith(LEGACY_IMAGE_PARSE_FAILED_PREFIX) ->
                localized(R.string.error_image_generation_parse_failed, message.removePrefix(LEGACY_IMAGE_PARSE_FAILED_PREFIX))
            message.startsWith(LEGACY_LOCAL_MODEL_LOAD_FAILED_PREFIX) ->
                localized(R.string.error_local_model_load_failed, message.removePrefix(LEGACY_LOCAL_MODEL_LOAD_FAILED_PREFIX))
            else -> message
        }
    }

    private fun localized(resId: Int, vararg args: Any): String =
        LocalizedResources.getString(HuaJuanApplication.instance, resId, null, *args)
}
