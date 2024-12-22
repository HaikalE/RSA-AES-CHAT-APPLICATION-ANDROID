package com.example.imrsaaes.cryptography


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigInteger
import kotlin.math.sqrt
import kotlin.random.Random


// Struktur data untuk kunci publik
@Serializable
data class PublicKey(val e: Long, val n: Long)


// Struktur data untuk menyimpan pasangan kunci publik dan privat
@Serializable
data class KeyPair(val publicKey: PublicKey, val privateKey: PrivateKey)








// Struktur data untuk kunci privat (menggunakan CRT)
@Serializable
data class PrivateKey(
    val p: Long,
    val q: Long,
    val d: Long,
    val dP: Long,
    val dQ: Long,
    val qInv: Long
)


object RSA {


    // Fungsi untuk menghasilkan pasangan kunci RSA
    fun generateKeyPair(): KeyPair {
        val p = generatePrime()
        val q = generatePrime()
        val n = p * q
        val totientFunction = n - (p + q - 1)
        val e = 5483L
        val d = modinv(e, totientFunction)


        // Komponen untuk dekripsi CRT
        val dP = modinv(e, p - 1)
        val dQ = modinv(e, q - 1)
        val qInv = modinv(q, p)


        return KeyPair(
            publicKey = PublicKey(e, n),
            privateKey = PrivateKey(p, q, d, dP, dQ, qInv)
        )
    }


    // Fungsi untuk mengenkripsi pesan menggunakan kunci publik RSA
    fun encrypt(message: String, publicKeyJson: String): LongArray {
        // Parsing JSON ke objek PublicKey
        val publicKey = Json.decodeFromString<PublicKey>(publicKeyJson)


        // Mengonversi pesan menjadi array kode ASCII
        val messageArray = message.map { it.code.toLong() }.toLongArray()


        // Enkripsi setiap karakter menggunakan kunci publik
        return messageArray.map { encryptDecrypt(it, publicKey.e, publicKey.n) }.toLongArray()
    }


    // Fungsi untuk mendekripsi pesan menggunakan CRT dan kunci privat dalam bentuk JSON String
    fun decrypt(cipherText: LongArray, privateKeyJson: String): String {
        // Parsing JSON ke objek PrivateKey
        val privateKey = Json.decodeFromString<PrivateKey>(privateKeyJson)


        // Mendekripsi setiap karakter menggunakan CRT
        val decryptedChars = cipherText.map { c ->
            val m1 = encryptDecrypt(c, privateKey.dP, privateKey.p)
            val m2 = encryptDecrypt(c, privateKey.dQ, privateKey.q)
            val h = (privateKey.qInv * (m1 - m2) % privateKey.p + privateKey.p) % privateKey.p
            val m = m2 + h * privateKey.q
            m.toInt().toChar()
        }.toCharArray()


        return String(decryptedChars)
    }




    // Fungsi untuk memilih bilangan prima acak
    private fun generatePrime(): Long {
        while (true) {
            val num = Random.nextLong(1009, 9964)
            if (isPrime(num)) return num
        }
    }


    // Fungsi untuk mengecek apakah bilangan merupakan bilangan prima
    private fun isPrime(num: Long): Boolean {
        if (num <= 1) return false
        if (num == 2L) return true
        for (i in 2..sqrt(num.toDouble()).toInt()) {
            if (num % i == 0L) return false
        }
        return true
    }


    // Fungsi untuk menghitung mod inverse
    private fun modinv(a: Long, m: Long): Long {
        val egcd = egcd(a, m)
        return if (egcd.a != 1L) 0 else (m + egcd.b % m) % m
    }


    // Extended Euclidean Algorithm untuk mod inverse
    private fun egcd(a: Long, b: Long): Egcd {
        return if (a == 0L) {
            Egcd(b, 0, 1)
        } else {
            val negcd = egcd(b % a, a)
            Egcd(negcd.a, negcd.c - (b / a) * negcd.b, negcd.b)
        }
    }


    // Fungsi untuk menghitung pangkat (a^b % n)
    private fun encryptDecrypt(a: Long, b: Long, n: Long): Long {
        return BigInteger.valueOf(a).modPow(BigInteger.valueOf(b), BigInteger.valueOf(n)).toLong()
    }


    // Struktur data untuk menyimpan hasil Extended GCD
    data class Egcd(var a: Long = 0, var b: Long = 0, var c: Long = 0)




}

