package com.example.imrsaaes.cryptography.core








import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Arrays
import kotlin.math.min















class AES {
    // current round index
    private var actual = 0








    private var Nk = 0








    // number of rounds for current AES
    private var Nr = 0








    // state
    private lateinit var state: Array<Array<IntArray>>








    // key stuff
    private lateinit var w: IntArray
    private lateinit var key: IntArray








    // Initialization vector (only for CBC)
    private var iv: ByteArray? = null








    constructor(key: ByteArray) {
        init(key, null)
    }








    constructor(key: ByteArray, iv: ByteArray?) {
        init(key, iv)
    }








    private fun init(key: ByteArray, iv: ByteArray?) {
        Log.d("AES ENCRYPTION PROCESS", "=== STEP 1: Initialization ===")
        Log.d("AES ENCRYPTION PROCESS", "------------------------------------------------------")
        Log.d("AES ENCRYPTION PROCESS", "Input Key  : ${key.joinToString(", ") { it.toString().padStart(3, ' ') }}")
        Log.d("AES ENCRYPTION PROCESS", "Input IV   : ${iv?.joinToString(", ") { it.toString().padStart(3, ' ') } ?: "null"}")
        Log.d("AES ENCRYPTION PROCESS", "------------------------------------------------------")




        this.iv = iv
        this.key = IntArray(key.size)




        // Convert key bytes to integers
        for (i in key.indices) {
            this.key[i] = key[i].toInt()
        }
        Log.d("AES ENCRYPTION PROCESS", "Converted Key:")
        Log.d("AES ENCRYPTION PROCESS", "  ${this.key.joinToString(", ") { it.toString().padStart(3, ' ') }}")




        // Determine key length and parameters
        Nb = 4
        when (key.size) {
            16 -> {
                Nr = 10
                Nk = 4
            }
            24 -> {
                Nr = 12
                Nk = 6
            }
            32 -> {
                Nr = 14
                Nk = 8
            }
            else -> throw IllegalArgumentException("It only supports 128, 192 and 256 bit keys!")
        }
        Log.d("AES ENCRYPTION PROCESS", "Key Details:")
        Log.d("AES ENCRYPTION PROCESS", "  Key Size: ${key.size * 8} bits")
        Log.d("AES ENCRYPTION PROCESS", "  Nr      : $Nr")
        Log.d("AES ENCRYPTION PROCESS", "  Nk      : $Nk")




        // Initialize state array
        state = Array(2) { Array(4) { IntArray(Nb) } }
        Log.d("AES ENCRYPTION PROCESS", "State Array Initialized:")
        Log.d("AES ENCRYPTION PROCESS", "  ${state.contentDeepToString()}")




        // Initialize key schedule array
        w = IntArray(Nb * (Nr + 1))
        Log.d("AES ENCRYPTION PROCESS", "Key Schedule Initialized:")
        Log.d("AES ENCRYPTION PROCESS", "  w Size: ${w.size}")




        // Key expansion
        expandKey()
        Log.d("AES ENCRYPTION PROCESS", "Key Expansion Completed")
        Log.d("AES ENCRYPTION PROCESS", "------------------------------------------------------")
    }




















    // The 128 bits of a state are an XOR offset applied to them with the 128 bits of the key expended.
    // s: state matrix that has Nb columns and 4 rows.
    // Round: A round of the key w to be added.
    // s: returns the addition of the key per round
    private fun addRoundKey(s: Array<IntArray>, round: Int): Array<IntArray> {
        Log.d("AES_PROCESS", "=== Round $round Start ===")
        Log.d("AES_PROCESS", "Matrix before AddRoundKey:\n${formatMatrix(s)}")

        for (c in 0 until Nb) {
            // Ambil word dari key schedule
            val word = w[round * Nb + c].toUInt()
            Log.d("AES_PROCESS", "Processing Word at Index: ${round * Nb + c}")
            Log.d("AES_PROCESS", "Word (32-bit): 0x${word.toString(16)}")

            for (r in 0..3) {
                val initialState = s[r][c]

                // Ekstraksi byte tertentu dari word
                val shiftAmount = 24 - r * 8
                val byteMask = 0xFFu
                val keyPart = ((word shr shiftAmount) and byteMask).toInt()

                // Log detail operator bitwise
                Log.d("AES_PROCESS", "=== Column $c Row $r ===")
                Log.d("AES_PROCESS", "Shift Amount: $shiftAmount bits")
                Log.d("AES_PROCESS", "Word >> $shiftAmount: 0x${(word shr shiftAmount).toString(16)}")
                Log.d("AES_PROCESS", "Byte Mask: 0x${byteMask.toString(16)}")
                Log.d("AES_PROCESS", "Extracted KeyPart: 0x${keyPart.toString(16)}")

                // Lakukan XOR
                val xorResult = initialState xor keyPart
                s[r][c] = xorResult

                // Log hasil XOR dengan detail
                Log.d("AES_PROCESS", "Initial State (s[$r][$c]): $initialState (0x${initialState.toString(16)})")
                Log.d("AES_PROCESS", "Key Part: $keyPart (0x${keyPart.toString(16)})")
                Log.d("AES_PROCESS", "Result after XOR: $xorResult (0x${xorResult.toString(16)})")
            }
        }

        Log.d("AES_PROCESS", "Matrix after AddRoundKey:\n${formatMatrix(s)}")
        Log.d("AES_PROCESS", "=== Round $round End ===")
        return s
    }
















    // Cipher/Decipher methods
    private fun cipher(`in`: Array<IntArray>, out: Array<IntArray>): Array<IntArray> {
        // Log to monitor the initialization of the state matrix
        for (i in `in`.indices) {
            for (j in `in`.indices) {
                out[i][j] = `in`[i][j]
                // Log to track the element being processed
            }
        }


        // Log to confirm completion of the initialization process


        actual = 0
        addRoundKey(out, actual)








        actual = 1
        while (actual < Nr) {
            subBytes(out)
            shiftRows(out)
            mixColumns(out)
            addRoundKey(out, actual)
            actual++
        }
        subBytes(out)
        shiftRows(out)
        addRoundKey(out, actual)
        return out
    }








    private fun decipher(`in`: Array<IntArray>, out: Array<IntArray>): Array<IntArray> {
        for (i in `in`.indices) {
            for (j in `in`.indices) {
                out[i][j] = `in`[i][j]
            }
        }
        actual = Nr
        addRoundKey(out, actual)








        actual = Nr - 1
        while (actual > 0) {
            invShiftRows(out)
            invSubBytes(out)
            addRoundKey(out, actual)
            invMixColumnas(out)
            actual--
        }
        invShiftRows(out)
        invSubBytes(out)
        addRoundKey(out, actual)
        return out
    }








    // Main cipher/decipher helper-methods (for 128-bit plain/cipher text in,
    // and 128-bit cipher/plain text out) produced by the encryption algorithm.
    private fun encrypt(text: ByteArray): ByteArray {
        require(text.size == 16) { "Only 16-byte blocks can be encrypted" }
        val out = ByteArray(text.size)








        // Debugging log: start of state matrix construction
        Log.d("Encrypt", "Starting state matrix construction.")


        for (i in 0 until Nb) { // columns
            for (j in 0..3) { // rows
                val index = i * Nb + j // Hitung indeks elemen
                val char = text[index].toInt().toChar() // Ambil karakter asli
                val asciiValue = text[index].toInt() // Konversi ke nilai ASCII
                val transformedValue = asciiValue and 0xff // Operasi bitwise AND 0xff


                // Log detail dari proses perhitungan
                Log.d("Encrypt", "Processing state[0][$j][$i]:")
                Log.d("Encrypt", "  Index: $index")
                Log.d("Encrypt", "  Character: '$char'") // Tampilkan karakter asli
                Log.d("Encrypt", "  ASCII Value: $asciiValue")
                Log.d("Encrypt", "  After bitwise AND 0xff: $transformedValue")


                // Simpan hasil ke matriks state
                state[0][j][i] = transformedValue


                // Log hasil yang dimasukkan ke matriks state
                Log.d(
                    "Encrypt",
                    "  Assigned state[0][$j][$i] = $transformedValue"
                )
            }
        }




        // Debugging log: completion of state matrix construction
        Log.d("Encrypt", "State matrix construction completed.")


        // Debugging log: handoff to the cipher function
        Log.d("Encrypt", "Calling cipher with the constructed state matrix.")








        cipher(state[0], state[1])
        val finalState = StringBuilder()
        for (i in 0 until Nb) {
            for (j in 0..3) {
                val byteValue = (state[1][j][i] and 0xff).toByte()
                out[i * Nb + j] = byteValue
                finalState.append(String.format("%02X ", byteValue)) // Format hexadecimal
            }
            finalState.append("\n") // Pisahkan tiap kolom dengan baris baru untuk visualisasi
        }


        Log.d("AES_DEBUG", "Final state after last round:\n$finalState")


        return out
    }








    private fun decrypt(text: ByteArray): ByteArray {
        require(text.size == 16) { "Only 16-byte blocks can be encrypted" }
        val out = ByteArray(text.size)








        for (i in 0 until Nb) { // columns
            for (j in 0..3) { // rows
                state[0][j][i] = text[i * Nb + j].toInt() and 0xff
            }
        }








        decipher(state[0], state[1])
        for (i in 0 until Nb) {
            for (j in 0..3) {
                out[i * Nb + j] = (state[1][j][i] and 0xff).toByte()
            }
        }
        return out
    }








    // Algorithm's general methods
    private fun invMixColumnas(state: Array<IntArray>): Array<IntArray> {
        var temp0: Int
        var temp1: Int
        var temp2: Int
        var temp3: Int
        for (c in 0 until Nb) {
            temp0 = mult(0x0e, state[0][c]) xor mult(0x0b, state[1][c]) xor mult(
                0x0d,
                state[2][c]
            ) xor mult(0x09, state[3][c])
            temp1 = mult(0x09, state[0][c]) xor mult(0x0e, state[1][c]) xor mult(
                0x0b,
                state[2][c]
            ) xor mult(0x0d, state[3][c])
            temp2 = mult(0x0d, state[0][c]) xor mult(0x09, state[1][c]) xor mult(
                0x0e,
                state[2][c]
            ) xor mult(0x0b, state[3][c])
            temp3 = mult(0x0b, state[0][c]) xor mult(0x0d, state[1][c]) xor mult(
                0x09,
                state[2][c]
            ) xor mult(0x0e, state[3][c])








            state[0][c] = temp0
            state[1][c] = temp1
            state[2][c] = temp2
            state[3][c] = temp3
        }
        return state
    }








    private fun invShiftRows(state: Array<IntArray>): Array<IntArray> {
        // row 1;
        var temp1 = state[1][Nb - 1]
        var i = Nb - 1
        while (i > 0) {
            state[1][i] = state[1][(i - 1) % Nb]
            i--
        }
        state[1][0] = temp1
        // row 2
        temp1 = state[2][Nb - 1]
        var temp2 = state[2][Nb - 2]
        i = Nb - 1
        while (i > 1) {
            state[2][i] = state[2][(i - 2) % Nb]
            i--
        }
        state[2][1] = temp1
        state[2][0] = temp2
        // row 3
        temp1 = state[3][Nb - 3]
        temp2 = state[3][Nb - 2]
        val temp3 = state[3][Nb - 1]
        i = Nb - 1
        while (i > 2) {
            state[3][i] = state[3][(i - 3) % Nb]
            i--
        }
        state[3][0] = temp1
        state[3][1] = temp2
        state[3][2] = temp3








        return state
    }
















    private fun invSubBytes(state: Array<IntArray>): Array<IntArray> {
        for (i in 0..3) {
            for (j in 0 until Nb) {
                state[i][j] = invSubWord(state[i][j]) and 0xFF
            }
        }
        return state
    }
















    private fun expandKey(): IntArray {
        Log.d("expandKey", "Mulai proses key expansion")
        var temp: Int
        var i = 0




        // Bagian pertama: Memasukkan key asli ke dalam w
        while (i < Nk) {
            w[i] = 0x00000000
            w[i] = w[i] or (key[4 * i] shl 24)
            w[i] = w[i] or (key[4 * i + 1] shl 16)
            w[i] = w[i] or (key[4 * i + 2] shl 8)
            w[i] = w[i] or key[4 * i + 3]
            Log.d("expandKey", "w[$i] = ${Integer.toHexString(w[i])}")
            i++
        }




        // Bagian kedua: Memperluas kunci menggunakan algoritma AES
        i = Nk
        while (i < Nb * (Nr + 1)) {
            temp = w[i - 1]
            Log.d("expandKey", "Sebelum proses w[$i], temp = ${Integer.toHexString(temp)}")




            // Jika indeks i adalah kelipatan Nk, lakukan transformasi khusus
            if (i % Nk == 0) {
                temp = subWord(rotWord(temp)) xor (rCon[i / Nk] shl 24)
                Log.d("expandKey", "Transformasi karena i % Nk == 0: temp = ${Integer.toHexString(temp)}")
            } else if (Nk > 6 && (i % Nk == 4)) {
                temp = subWord(temp)
                Log.d("expandKey", "Transformasi karena Nk > 6 dan i % Nk == 4: temp = ${Integer.toHexString(temp)}")
            }




            // Hitung w[i] dengan XOR antara w[i-Nk] dan temp
            w[i] = w[i - Nk] xor temp
            Log.d("expandKey", "w[$i] = ${Integer.toHexString(w[i])}")
            i++
        }




        Log.d("expandKey", "Key expansion selesai")
        return w
    }












    private fun mixColumns(state: Array<IntArray>): Array<IntArray> {
        val logBuilder = StringBuilder()
        logBuilder.appendLine("=== STEP 4: MixColumns ===")
        logBuilder.appendLine("Predefined Matrix (Fixed in AES):")
        logBuilder.appendLine("[ [2, 3, 1, 1],")
        logBuilder.appendLine("  [1, 2, 3, 1],")
        logBuilder.appendLine("  [1, 1, 2, 3],")
        logBuilder.appendLine("  [3, 1, 1, 2] ]")
        logBuilder.appendLine()




        logBuilder.appendLine("Initial State:")
        logBuilder.appendLine(formatState(state))




        var temp0: Int
        var temp1: Int
        var temp2: Int
        var temp3: Int




        for (c in 0 until Nb) {
            logBuilder.appendLine("\n=== Processing Column $c ===")
            logBuilder.appendLine("Initial Column: [${state[0][c]}, ${state[1][c]}, ${state[2][c]}, ${state[3][c]}]")




            // Manual Calculation for Each temp
            temp0 = mult(0x02, state[0][c]) xor mult(0x03, state[1][c]) xor state[2][c] xor state[3][c]
            logBuilder.appendLine(" - temp0 (2×${state[0][c]} XOR 3×${state[1][c]} XOR ${state[2][c]} XOR ${state[3][c]}) = $temp0")




            temp1 = state[0][c] xor mult(0x02, state[1][c]) xor mult(0x03, state[2][c]) xor state[3][c]
            logBuilder.appendLine(" - temp1 (${state[0][c]} XOR 2×${state[1][c]} XOR 3×${state[2][c]} XOR ${state[3][c]}) = $temp1")




            temp2 = state[0][c] xor state[1][c] xor mult(0x02, state[2][c]) xor mult(0x03, state[3][c])
            logBuilder.appendLine(" - temp2 (${state[0][c]} XOR ${state[1][c]} XOR 2×${state[2][c]} XOR 3×${state[3][c]}) = $temp2")




            temp3 = mult(0x03, state[0][c]) xor state[1][c] xor state[2][c] xor mult(0x02, state[3][c])
            logBuilder.appendLine(" - temp3 (3×${state[0][c]} XOR ${state[1][c]} XOR ${state[2][c]} XOR 2×${state[3][c]}) = $temp3")




            // Update state
            state[0][c] = temp0
            state[1][c] = temp1
            state[2][c] = temp2
            state[3][c] = temp3




            logBuilder.appendLine("Updated Column $c:")
            logBuilder.appendLine("[${state[0][c]}, ${state[1][c]}, ${state[2][c]}, ${state[3][c]}]")
        }




        logBuilder.appendLine("\nFinal State after MixColumns:")
        logBuilder.appendLine(formatState(state))
        Log.d("AES ENCRYPTION PROCESS", logBuilder.toString())
        return state
    }








    private fun formatState(state: Array<IntArray>): String {
        return state.joinToString(separator = "\n") { row -> row.joinToString(prefix = "[", postfix = "]", separator = ", ") }
    }
















    // Utility function to format the matrix for logging
    private fun formatMatrix(matrix: Array<IntArray>): String {
        return matrix.joinToString(separator = "\n") { row ->
            row.joinToString(prefix = "[", postfix = "]") { value -> value.toString() }
        }
    }




    private fun shiftRows(state: Array<IntArray>): Array<IntArray> {
        Log.d("AES ENCRYPTION PROCESS", "=== ShiftRows Step ===")
        Log.d("AES_ENCRYPTION_PROCESS", "Initial State:\n${formatMatrix(state)}")




        // Shift Row 1 (Move 1-byte left)
        val temp1Row1 = state[1][0]
        for (i in 0 until Nb - 1) {
            state[1][i] = state[1][(i + 1) % Nb]
        }
        state[1][Nb - 1] = temp1Row1
        Log.d("AES ENCRYPTION PROCESS", "After Row 1 Shift: ${state[1].contentToString()}")




        // Shift Row 2 (Move 2-bytes left)
        val temp1Row2 = state[2][0]
        val temp2Row2 = state[2][1]
        for (i in 0 until Nb - 2) {
            state[2][i] = state[2][(i + 2) % Nb]
        }
        state[2][Nb - 2] = temp1Row2
        state[2][Nb - 1] = temp2Row2
        Log.d("AES ENCRYPTION PROCESS", "After Row 2 Shift: ${state[2].contentToString()}")




        // Shift Row 3 (Move 3-bytes left)
        val temp1Row3 = state[3][0]
        val temp2Row3 = state[3][1]
        val temp3Row3 = state[3][2]
        for (i in 0 until Nb - 3) {
            state[3][i] = state[3][(i + 3) % Nb]
        }
        state[3][Nb - 3] = temp1Row3
        state[3][Nb - 2] = temp2Row3
        state[3][Nb - 1] = temp3Row3
        Log.d("AES ENCRYPTION PROCESS", "After Row 3 Shift: ${state[3].contentToString()}")




        Log.d("AES_ENCRYPTION_PROCESS", "Final State After ShiftRows:\n${formatMatrix(state)}")
        return state
    }












    private fun subBytes(state: Array<IntArray>): Array<IntArray> {
        Log.d("AES_ENCRYPTION_PROCESS", "=== SubBytes ===")
        val formattedLog = StringBuilder()
        formattedLog.appendLine("=== AES SubBytes Transformation ===")




        for (i in 0..3) {
            for (j in 0 until Nb) {
                val before = state[i][j]
                val rowHex = (before shr 4) and 0xF // High nibble (row in hex)
                val colHex = before and 0xF // Low nibble (column in hex)
                val rowChar = rowHex.toString(16).uppercase() // Convert row to char (A-F if applicable)
                val colChar = colHex.toString(16).uppercase() // Convert column to char (A-F if applicable)




                val after = subWord(before) and 0xFF
                state[i][j] = after




                formattedLog.appendLine(
                    "State[$i][$j]: $before → $after (S-Box[row=$rowChar, col=$colChar])"
                )
            }
        }




        Log.d("AES_ENCRYPTION_PROCESS", formattedLog.toString())
        return state
    }
















    // Public methods
    @OptIn(ExperimentalStdlibApi::class)
    fun ECB_encrypt(text: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        Log.d("AES_ENCRYPTION_PROCESS", "Starting ECB encryption")
        while (i < text.size) {
            try {
                val chunk = Arrays.copyOfRange(text, i, i + 16)
                Log.d("AES_ENCRYPTION_PROCESS", "Encrypting block at index $i: ${chunk.toHexString()}")
                val encryptedChunk = encrypt(chunk)
                Log.d("AES_ENCRYPTION_PROCESS", "Encrypted block: ${encryptedChunk.toHexString()}")
                out.write(encryptedChunk)
            } catch (e: IOException) {
                Log.e("AES_ENCRYPTION_PROCESS", "Error writing encrypted block: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("AES_ENCRYPTION_PROCESS", "Encryption failed at index $i: ${e.message}", e)
            }
            i += 16
        }
        val encryptedData = out.toByteArray()
        Log.d("AES_ENCRYPTION_PROCESS", "Encryption complete. Total size: ${encryptedData.size}")
        return encryptedData
    }












    fun ECB_decrypt(text: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < text.size) {
            try {
                out.write(decrypt(Arrays.copyOfRange(text, i, i + 16)))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            i += 16
        }
        return out.toByteArray()
    }








    fun CBC_encrypt(text: ByteArray): ByteArray {
        if (iv == null) {
            throw IllegalStateException("Initialization vector (IV) must be provided for CBC mode.")
        }








        // Print the IV for debugging
//        println("IV (Initialization Vector): ${iv!!.joinToString(separator = " ") { String.format("%02X", it) }}")








        var previousBlock: ByteArray? = null
        val out = ByteArrayOutputStream()








        // Prepend the IV to the output
        try {
            out.write(iv)
        } catch (e: IOException) {
            e.printStackTrace()
        }








        var i = 0
        while (i < text.size) {
            var part = Arrays.copyOfRange(text, i, i + 16)
            try {
                if (previousBlock == null) previousBlock = iv
                part = xor(previousBlock, part)
                previousBlock = encrypt(part)
                out.write(previousBlock)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            i += 16
        }
        return out.toByteArray()
    }
















    fun CBC_decrypt(text: ByteArray): ByteArray {
        if (text.size < 16) {
            throw IllegalArgumentException("The encrypted data is too short to contain an IV and data.")
        }








        // Extract the IV from the beginning of the encrypted text
        val iv = Arrays.copyOfRange(text, 0, 16)
//        println("Extracted IV (Initialization Vector): ${iv.joinToString(separator = " ") { String.format("%02X", it) }}")








        var previousBlock: ByteArray? = iv
        val out = ByteArrayOutputStream()
        var i = 16  // Start after the IV








        while (i < text.size) {
            val part = Arrays.copyOfRange(text, i, i + 16)
            var tmp = decrypt(part)
            try {
                if (previousBlock == null) previousBlock = iv
                tmp = xor(previousBlock, tmp)
                previousBlock = part
                out.write(tmp)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            i += 16
        }
        return out.toByteArray()
    }
















    companion object {
        // number of chars (32 bit)
        private var Nb = 4








        // necessary matrix for AES (sBox + inverted one & rCon)
        private val sBox =
            intArrayOf( //0     1    2      3     4    5     6     7      8    9     A      B    C     D     E     F
                0x63,
                0x7c,
                0x77,
                0x7b,
                0xf2,
                0x6b,
                0x6f,
                0xc5,
                0x30,
                0x01,
                0x67,
                0x2b,
                0xfe,
                0xd7,
                0xab,
                0x76,
                0xca,
                0x82,
                0xc9,
                0x7d,
                0xfa,
                0x59,
                0x47,
                0xf0,
                0xad,
                0xd4,
                0xa2,
                0xaf,
                0x9c,
                0xa4,
                0x72,
                0xc0,
                0xb7,
                0xfd,
                0x93,
                0x26,
                0x36,
                0x3f,
                0xf7,
                0xcc,
                0x34,
                0xa5,
                0xe5,
                0xf1,
                0x71,
                0xd8,
                0x31,
                0x15,
                0x04,
                0xc7,
                0x23,
                0xc3,
                0x18,
                0x96,
                0x05,
                0x9a,
                0x07,
                0x12,
                0x80,
                0xe2,
                0xeb,
                0x27,
                0xb2,
                0x75,
                0x09,
                0x83,
                0x2c,
                0x1a,
                0x1b,
                0x6e,
                0x5a,
                0xa0,
                0x52,
                0x3b,
                0xd6,
                0xb3,
                0x29,
                0xe3,
                0x2f,
                0x84,
                0x53,
                0xd1,
                0x00,
                0xed,
                0x20,
                0xfc,
                0xb1,
                0x5b,
                0x6a,
                0xcb,
                0xbe,
                0x39,
                0x4a,
                0x4c,
                0x58,
                0xcf,
                0xd0,
                0xef,
                0xaa,
                0xfb,
                0x43,
                0x4d,
                0x33,
                0x85,
                0x45,
                0xf9,
                0x02,
                0x7f,
                0x50,
                0x3c,
                0x9f,
                0xa8,
                0x51,
                0xa3,
                0x40,
                0x8f,
                0x92,
                0x9d,
                0x38,
                0xf5,
                0xbc,
                0xb6,
                0xda,
                0x21,
                0x10,
                0xff,
                0xf3,
                0xd2,
                0xcd,
                0x0c,
                0x13,
                0xec,
                0x5f,
                0x97,
                0x44,
                0x17,
                0xc4,
                0xa7,
                0x7e,
                0x3d,
                0x64,
                0x5d,
                0x19,
                0x73,
                0x60,
                0x81,
                0x4f,
                0xdc,
                0x22,
                0x2a,
                0x90,
                0x88,
                0x46,
                0xee,
                0xb8,
                0x14,
                0xde,
                0x5e,
                0x0b,
                0xdb,
                0xe0,
                0x32,
                0x3a,
                0x0a,
                0x49,
                0x06,
                0x24,
                0x5c,
                0xc2,
                0xd3,
                0xac,
                0x62,
                0x91,
                0x95,
                0xe4,
                0x79,
                0xe7,
                0xc8,
                0x37,
                0x6d,
                0x8d,
                0xd5,
                0x4e,
                0xa9,
                0x6c,
                0x56,
                0xf4,
                0xea,
                0x65,
                0x7a,
                0xae,
                0x08,
                0xba,
                0x78,
                0x25,
                0x2e,
                0x1c,
                0xa6,
                0xb4,
                0xc6,
                0xe8,
                0xdd,
                0x74,
                0x1f,
                0x4b,
                0xbd,
                0x8b,
                0x8a,
                0x70,
                0x3e,
                0xb5,
                0x66,
                0x48,
                0x03,
                0xf6,
                0x0e,
                0x61,
                0x35,
                0x57,
                0xb9,
                0x86,
                0xc1,
                0x1d,
                0x9e,
                0xe1,
                0xf8,
                0x98,
                0x11,
                0x69,
                0xd9,
                0x8e,
                0x94,
                0x9b,
                0x1e,
                0x87,
                0xe9,
                0xce,
                0x55,
                0x28,
                0xdf,
                0x8c,
                0xa1,
                0x89,
                0x0d,
                0xbf,
                0xe6,
                0x42,
                0x68,
                0x41,
                0x99,
                0x2d,
                0x0f,
                0xb0,
                0x54,
                0xbb,
                0x16
            )








        private val rsBox = intArrayOf(
            0x52,
            0x09,
            0x6a,
            0xd5,
            0x30,
            0x36,
            0xa5,
            0x38,
            0xbf,
            0x40,
            0xa3,
            0x9e,
            0x81,
            0xf3,
            0xd7,
            0xfb,
            0x7c,
            0xe3,
            0x39,
            0x82,
            0x9b,
            0x2f,
            0xff,
            0x87,
            0x34,
            0x8e,
            0x43,
            0x44,
            0xc4,
            0xde,
            0xe9,
            0xcb,
            0x54,
            0x7b,
            0x94,
            0x32,
            0xa6,
            0xc2,
            0x23,
            0x3d,
            0xee,
            0x4c,
            0x95,
            0x0b,
            0x42,
            0xfa,
            0xc3,
            0x4e,
            0x08,
            0x2e,
            0xa1,
            0x66,
            0x28,
            0xd9,
            0x24,
            0xb2,
            0x76,
            0x5b,
            0xa2,
            0x49,
            0x6d,
            0x8b,
            0xd1,
            0x25,
            0x72,
            0xf8,
            0xf6,
            0x64,
            0x86,
            0x68,
            0x98,
            0x16,
            0xd4,
            0xa4,
            0x5c,
            0xcc,
            0x5d,
            0x65,
            0xb6,
            0x92,
            0x6c,
            0x70,
            0x48,
            0x50,
            0xfd,
            0xed,
            0xb9,
            0xda,
            0x5e,
            0x15,
            0x46,
            0x57,
            0xa7,
            0x8d,
            0x9d,
            0x84,
            0x90,
            0xd8,
            0xab,
            0x00,
            0x8c,
            0xbc,
            0xd3,
            0x0a,
            0xf7,
            0xe4,
            0x58,
            0x05,
            0xb8,
            0xb3,
            0x45,
            0x06,
            0xd0,
            0x2c,
            0x1e,
            0x8f,
            0xca,
            0x3f,
            0x0f,
            0x02,
            0xc1,
            0xaf,
            0xbd,
            0x03,
            0x01,
            0x13,
            0x8a,
            0x6b,
            0x3a,
            0x91,
            0x11,
            0x41,
            0x4f,
            0x67,
            0xdc,
            0xea,
            0x97,
            0xf2,
            0xcf,
            0xce,
            0xf0,
            0xb4,
            0xe6,
            0x73,
            0x96,
            0xac,
            0x74,
            0x22,
            0xe7,
            0xad,
            0x35,
            0x85,
            0xe2,
            0xf9,
            0x37,
            0xe8,
            0x1c,
            0x75,
            0xdf,
            0x6e,
            0x47,
            0xf1,
            0x1a,
            0x71,
            0x1d,
            0x29,
            0xc5,
            0x89,
            0x6f,
            0xb7,
            0x62,
            0x0e,
            0xaa,
            0x18,
            0xbe,
            0x1b,
            0xfc,
            0x56,
            0x3e,
            0x4b,
            0xc6,
            0xd2,
            0x79,
            0x20,
            0x9a,
            0xdb,
            0xc0,
            0xfe,
            0x78,
            0xcd,
            0x5a,
            0xf4,
            0x1f,
            0xdd,
            0xa8,
            0x33,
            0x88,
            0x07,
            0xc7,
            0x31,
            0xb1,
            0x12,
            0x10,
            0x59,
            0x27,
            0x80,
            0xec,
            0x5f,
            0x60,
            0x51,
            0x7f,
            0xa9,
            0x19,
            0xb5,
            0x4a,
            0x0d,
            0x2d,
            0xe5,
            0x7a,
            0x9f,
            0x93,
            0xc9,
            0x9c,
            0xef,
            0xa0,
            0xe0,
            0x3b,
            0x4d,
            0xae,
            0x2a,
            0xf5,
            0xb0,
            0xc8,
            0xeb,
            0xbb,
            0x3c,
            0x83,
            0x53,
            0x99,
            0x61,
            0x17,
            0x2b,
            0x04,
            0x7e,
            0xba,
            0x77,
            0xd6,
            0x26,
            0xe1,
            0x69,
            0x14,
            0x63,
            0x55,
            0x21,
            0x0c,
            0x7d
        )








        private val rCon = intArrayOf(
            0x8d,
            0x01,
            0x02,
            0x04,
            0x08,
            0x10,
            0x20,
            0x40,
            0x80,
            0x1b,
            0x36,
            0x6c,
            0xd8,
            0xab,
            0x4d,
            0x9a,
            0x2f,
            0x5e,
            0xbc,
            0x63,
            0xc6,
            0x97,
            0x35,
            0x6a,
            0xd4,
            0xb3,
            0x7d,
            0xfa,
            0xef,
            0xc5,
            0x91,
            0x39,
            0x72,
            0xe4,
            0xd3,
            0xbd,
            0x61,
            0xc2,
            0x9f,
            0x25,
            0x4a,
            0x94,
            0x33,
            0x66,
            0xcc,
            0x83,
            0x1d,
            0x3a,
            0x74,
            0xe8,
            0xcb,
            0x8d,
            0x01,
            0x02,
            0x04,
            0x08,
            0x10,
            0x20,
            0x40,
            0x80,
            0x1b,
            0x36,
            0x6c,
            0xd8,
            0xab,
            0x4d,
            0x9a,
            0x2f,
            0x5e,
            0xbc,
            0x63,
            0xc6,
            0x97,
            0x35,
            0x6a,
            0xd4,
            0xb3,
            0x7d,
            0xfa,
            0xef,
            0xc5,
            0x91,
            0x39,
            0x72,
            0xe4,
            0xd3,
            0xbd,
            0x61,
            0xc2,
            0x9f,
            0x25,
            0x4a,
            0x94,
            0x33,
            0x66,
            0xcc,
            0x83,
            0x1d,
            0x3a,
            0x74,
            0xe8,
            0xcb,
            0x8d,
            0x01,
            0x02,
            0x04,
            0x08,
            0x10,
            0x20,
            0x40,
            0x80,
            0x1b,
            0x36,
            0x6c,
            0xd8,
            0xab,
            0x4d,
            0x9a,
            0x2f,
            0x5e,
            0xbc,
            0x63,
            0xc6,
            0x97,
            0x35,
            0x6a,
            0xd4,
            0xb3,
            0x7d,
            0xfa,
            0xef,
            0xc5,
            0x91,
            0x39,
            0x72,
            0xe4,
            0xd3,
            0xbd,
            0x61,
            0xc2,
            0x9f,
            0x25,
            0x4a,
            0x94,
            0x33,
            0x66,
            0xcc,
            0x83,
            0x1d,
            0x3a,
            0x74,
            0xe8,
            0xcb,
            0x8d,
            0x01,
            0x02,
            0x04,
            0x08,
            0x10,
            0x20,
            0x40,
            0x80,
            0x1b,
            0x36,
            0x6c,
            0xd8,
            0xab,
            0x4d,
            0x9a,
            0x2f,
            0x5e,
            0xbc,
            0x63,
            0xc6,
            0x97,
            0x35,
            0x6a,
            0xd4,
            0xb3,
            0x7d,
            0xfa,
            0xef,
            0xc5,
            0x91,
            0x39,
            0x72,
            0xe4,
            0xd3,
            0xbd,
            0x61,
            0xc2,
            0x9f,
            0x25,
            0x4a,
            0x94,
            0x33,
            0x66,
            0xcc,
            0x83,
            0x1d,
            0x3a,
            0x74,
            0xe8,
            0xcb,
            0x8d,
            0x01,
            0x02,
            0x04,
            0x08,
            0x10,
            0x20,
            0x40,
            0x80,
            0x1b,
            0x36,
            0x6c,
            0xd8,
            0xab,
            0x4d,
            0x9a,
            0x2f,
            0x5e,
            0xbc,
            0x63,
            0xc6,
            0x97,
            0x35,
            0x6a,
            0xd4,
            0xb3,
            0x7d,
            0xfa,
            0xef,
            0xc5,
            0x91,
            0x39,
            0x72,
            0xe4,
            0xd3,
            0xbd,
            0x61,
            0xc2,
            0x9f,
            0x25,
            0x4a,
            0x94,
            0x33,
            0x66,
            0xcc,
            0x83,
            0x1d,
            0x3a,
            0x74,
            0xe8,
            0xcb,
            0x8d
        )








        private fun invSubWord(word: Int): Int {
            var subWord = 0
            var i = 24
            while (i >= 0) {
                val `in` = word shl i ushr 24
                subWord = subWord or (rsBox[`in`] shl (24 - i))
                i -= 8
            }
            return subWord
        }








        private fun mult(a: Int, b: Int): Int {
            var a = a
            var b = b
            var sum = 0
            while (a != 0) { // while it is not 0
                if ((a and 1) != 0) { // check if the first bit is 1
                    sum = sum xor b // add b from the smallest bit
                }
                b = xtime(b) // bit shift left mod 0x11b if necessary;
                a = a ushr 1 // lowest bit of "a" was used so shift right
            }
            return sum
        }








        private fun rotWord(word: Int): Int {
            return (word shl 8) or ((word and -0x1000000) ushr 24)
        }
















        private fun subWord(word: Int): Int {
            var subWord = 0
            var i = 24
            while (i >= 0) {
                val `in` = word shl i ushr 24
                subWord = subWord or (sBox[`in`] shl (24 - i))
                i -= 8
            }
            return subWord
        }








        private fun xtime(b: Int): Int {
            if ((b and 0x80) == 0) {
                return b shl 1
            }
            return (b shl 1) xor 0x11b
        }








        private fun xor(a: ByteArray?, b: ByteArray): ByteArray {
            val result = ByteArray(
                min(a!!.size.toDouble(), b.size.toDouble())
                    .toInt()
            )
            for (j in result.indices) {
                val xor = a[j].toInt() xor b[j].toInt()
                result[j] = (0xff and xor).toByte()
            }
            return result
        }
    }
}



