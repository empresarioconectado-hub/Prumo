package com.example.data

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrumoBackupCryptoTest {
    @Test
    fun encryptedBackupRoundTripsWithTheSamePassword() {
        val source = """{"format":"prumo-backup","schemaVersion":1,"cashEntries":[]}"""

        val encrypted = PrumoBackupCrypto.encrypt(source, "senha-forte-de-teste-2026")
        val restored = PrumoBackupCrypto.decrypt(encrypted, "senha-forte-de-teste-2026")

        assertEquals(source, restored)
    }

    @Test
    fun encryptedBackupRejectsAnIncorrectPassword() {
        val encrypted = PrumoBackupCrypto.encrypt("dados financeiros", "senha-forte-de-teste-2026")

        assertThrows(AEADBadTagException::class.java) {
            PrumoBackupCrypto.decrypt(encrypted, "senha-incorreta-2026")
        }
    }

    @Test
    fun encryptionRejectsPasswordsShorterThanTwelveCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            PrumoBackupCrypto.encrypt("dados", "curta")
        }
    }
}
