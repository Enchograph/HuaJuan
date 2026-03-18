package com.huajuan.aispace.data.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object KnowledgeVectorUtils {
    fun floatsToBytes(values: List<Float>): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun bytesToFloats(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(bytes.size / 4)
        var index = 0
        while (buffer.remaining() >= 4 && index < out.size) {
            out[index++] = buffer.getFloat()
        }
        return out
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val dim = minOf(a.size, b.size)
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in 0 until dim) {
            val av = a[i]
            val bv = b[i]
            dot += (av * bv)
            normA += (av * av)
            normB += (bv * bv)
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }
}

