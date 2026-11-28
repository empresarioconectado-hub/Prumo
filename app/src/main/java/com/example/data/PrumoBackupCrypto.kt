package com.example.data

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/**
 * Password-based authenticated encryption for portable backup files.
 * The password is never persisted by the application.
 */
internal object PrumoBackupCrypto {
    private const val FORMAT = "prumo-encrypted-backup"
    private const val VERSION = 1
    private const val ITERATIONS = 250_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val KDF = "PBKDF2-HMAC-SHA256"
    private val LEGACY_KDFS = setOf("PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1")

    fun encrypt(plainText: String, password: String): String {
        require(password.length >= 12) { "A senha do backup deve ter pelo menos 12 caracteres." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val keyBytes = deriveKey(password, salt, ITERATIONS, KDF)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            return JSONObject()
                .put("format", FORMAT)
                .put("encryptionVersion", VERSION)
                .put("kdf", KDF)
                .put("iterations", ITERATIONS)
                .put("cipher", "AES-256-GCM")
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .toString(2)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(envelopeJson: String, password: String): String {
        require(password.isNotEmpty()) { "Informe a senha utilizada ao criar este backup." }
        val envelope = JSONObject(envelopeJson)
        require(envelope.getString("format") == FORMAT) { "Formato de backup criptografado inválido." }
        require(envelope.getInt("encryptionVersion") == VERSION) { "Versão de criptografia não suportada." }
        val kdf = envelope.getString("kdf")
        require((kdf == KDF || kdf in LEGACY_KDFS) && envelope.getString("cipher") == "AES-256-GCM") {
            "Algoritmo de criptografia não suportado."
        }
        val iterations = envelope.getInt("iterations")
        require(iterations in 100_000..1_000_000) { "Parâmetros de derivação de chave inválidos." }
        val salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT)
        val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.DEFAULT)
        require(salt.size == SALT_BYTES && iv.size == IV_BYTES && ciphertext.size >= 16) {
            "Arquivo de backup criptografado corrompido."
        }
        val keyBytes = deriveKey(password, salt, iterations, kdf)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        } finally {
            keyBytes.fill(0)
        }
    }

    internal fun deriveKey(password: String, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        require(iterations in 1..1_000_000) { "Parâmetros de derivação de chave inválidos." }
        if (algorithm != KDF) {
            require(algorithm in LEGACY_KDFS) { "Algoritmo de derivação de chave não suportado." }
            val passwordChars = password.toCharArray()
            val spec = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
            return try {
                SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
                passwordChars.fill(0.toChar())
            }
        }

        // PBKDF2-HMAC-SHA256 implemented over the platform HMAC primitive so
        // backups remain portable between Android 7 and newer Android releases.
        val passwordChars = password.toCharArray()
        val encodedPassword = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(passwordChars))
        val passwordBytes = ByteArray(encodedPassword.remaining())
        encodedPassword.get(passwordBytes)
        val blockInput = ByteArray(salt.size + 4)
        System.arraycopy(salt, 0, blockInput, 0, salt.size)
        blockInput[blockInput.lastIndex] = 1
        var u = ByteArray(0)
        val derived = ByteArray(KEY_LENGTH_BITS / 8)
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(passwordBytes, "HmacSHA256"))
            u = mac.doFinal(blockInput)
            u.copyInto(derived)
            for (round in 1 until iterations) {
                val next = mac.doFinal(u)
                for (index in derived.indices) {
                    derived[index] = (derived[index].toInt() xor next[index].toInt()).toByte()
                }
                u.fill(0)
                u = next
            }
            return derived
        } finally {
            passwordChars.fill(0.toChar())
            passwordBytes.fill(0)
            if (encodedPassword.hasArray()) encodedPassword.array().fill(0)
            blockInput.fill(0)
            u.fill(0)
        }
    }
}
