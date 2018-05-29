package com.icenerd.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

object Installation {
    private var uniqueID: String? = null
    private val INSTALLATION = "INSTALLATION"
    @JvmStatic
    @Synchronized
    fun getUUID(context: Context): String? {
        if (uniqueID == null) {
            val installation = File(context.filesDir, INSTALLATION)
            try {
                if (!installation.exists()) {
                    writeInstallationFile(installation)
                }
                uniqueID = readInstallationFile(installation)
            } catch (err: Exception) {
                throw RuntimeException(err)
            }

        }
        return uniqueID
    }
    @Throws(IOException::class)
    private fun readInstallationFile(installFile: File): String {
        val f = RandomAccessFile(installFile, "r")
        val bytes = ByteArray(f.length().toInt())
        f.readFully(bytes)
        f.close()
        return String(bytes)
    }
    @Throws(IOException::class)
    private fun writeInstallationFile(installFile: File) {
        val out = FileOutputStream(installFile)
        val id = UUID.randomUUID().toString()
        out.write(id.toByteArray())
        out.close()
    }
}