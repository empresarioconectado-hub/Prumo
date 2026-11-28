package com.example.data

import android.os.Build
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
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

    fun encrypt(plainText: String, password: String): String {
        require(password.length >= 12) { "A senha do backup deve ter pelo menos 12 caracteres." }
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val kdf = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val keyBytes = deriveKey(password, salt, ITERATIONS, kdf)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            return JSONObject()
                .put("format", FORMAT)
                .put("encryptionVersion", VERSION)
                .put("kdf", kdf)
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
        require(kdf in setOf("PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1") &&
            envelope.getString("cipher") == "AES-256-GCM") { "Algoritmo de criptografia não suportado." }
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

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int, algorithm: String): ByteArray {
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            passwordChars.fill(0.toChar())
        }
    }
}
