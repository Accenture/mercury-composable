package com.accenture.demo.start

import org.platformlambda.core.annotations.MainApplication
import org.platformlambda.core.models.EntryPoint
import org.platformlambda.core.system.AutoStart
import org.platformlambda.core.util.CryptoApi
import org.platformlambda.core.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

@MainApplication
class MainApp : EntryPoint {
    override fun start(args: Array<String>) {
        // Create a demo encryption key if not exists
        val folder: File = File(TEMP_KEY_STORE)
        if (!folder.exists() && folder.mkdirs()) {
            log.info("Folder {} created", folder)
        }
        val f = File(folder, DEMO_MASTER_KEY)
        if (!f.exists()) {
            val b64Key: String? = util.bytesToBase64(crypto.generateAesKey(256))
            util.str2file(f, b64Key)
            log.info("Demo encryption key {} created", f.getPath())
        }
        log.info("Started")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MainApp::class.java)
        private val util: Utility = Utility.getInstance()
        private val crypto = CryptoApi()

        private const val TEMP_KEY_STORE = "/tmp/keystore"
        private const val DEMO_MASTER_KEY = "demo.txt"

        @JvmStatic
        fun main(args: Array<String>) {
            AutoStart.main(args)
        }
    }
}
