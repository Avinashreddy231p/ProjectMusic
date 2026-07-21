/*
 * Copyright (c) 2025 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.projectmusic.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.ECPoint
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun String.encodeMd5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

fun ByteArray.encodeSha256(): ByteArray {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(this)
}

/**
 * Basic encryption for sensitive strings using a static key.
 * For production, consider using Android Keystore for better security.
 */
object CryptoUtil {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val EC_KEY_ALIAS = "unison_submission_key"

    private val keySpec = SecretKeySpec("ProjectMusic_CUC".toByteArray(), "AES")
    private val ivSpec = IvParameterSpec("1234567890123456".toByteArray())

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(value: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = Base64.decode(value, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted)
    }

    fun getOrCreateUnisonKey(): KeyPair {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(EC_KEY_ALIAS)) {
            val entry = ks.getEntry(EC_KEY_ALIAS, null)
            if (entry is KeyStore.PrivateKeyEntry) {
                return KeyPair(entry.certificate.publicKey, entry.privateKey)
            }
        }

        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            EC_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).run {
            setDigests(KeyProperties.DIGEST_SHA256)
            build()
        }

        kpg.initialize(parameterSpec)
        return kpg.generateKeyPair()
    }

    fun signData(data: ByteArray): String {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entry = ks.getEntry(EC_KEY_ALIAS, null) as KeyStore.PrivateKeyEntry

        val signatureBytes: ByteArray = Signature.getInstance("SHA256withECDSA").run {
            initSign(entry.privateKey)
            update(data)
            sign()
        }

        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    fun getUnisonPublicKeyId(): String {
        val keyPair = getOrCreateUnisonKey()
        val publicKey = keyPair.public as java.security.interfaces.ECPublicKey
        val jwk = buildUnisonJwk(publicKey)
        return jwk.toByteArray(Charsets.UTF_8).encodeSha256().joinToString("") { "%02x".format(it) }
    }

    fun buildUnisonJwk(publicKey: java.security.interfaces.ECPublicKey): String {
        val x = publicKey.w.affineX
        val y = publicKey.w.affineY
        val xBytes = getCoordinateBytes(x, 32)
        val yBytes = getCoordinateBytes(y, 32)
        val xBase64 = Base64.encodeToString(xBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val yBase64 = Base64.encodeToString(yBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        
        // Use a simple JSON string to ensure canonical order (alphabetical)
        // crv, kty, x, y
        return "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"$xBase64\",\"y\":\"$yBase64\"}"
    }

    private fun getCoordinateBytes(coordinate: java.math.BigInteger, length: Int): ByteArray {
        val array = coordinate.toByteArray()
        val fixedArray = ByteArray(length)
        if (array.size > length) {
            System.arraycopy(array, array.size - length, fixedArray, 0, length)
        } else {
            System.arraycopy(array, 0, fixedArray, length - array.size, array.size)
        }
        return fixedArray
    }
}
