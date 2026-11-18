package com.example.evoke.nllb

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NLLBDebug {
    private const val TAG = "NLLB_TEST"

    fun run(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val input = "Hello, how are you? I would like to see the doctor tomorrow."
            val start = SystemClock.elapsedRealtime()
            Log.d(TAG, "START input = $input")
            try {
                val translator = NLLBTranslator(context.applicationContext)
                try {
                    val urdu = translator.translate(input)
                    val secs = (SystemClock.elapsedRealtime() - start) / 1000.0
                    Log.d(TAG, "SUCCESS in ${secs}s")
                    Log.d(TAG, "OUTPUT = $urdu")
                } finally {
                    translator.close()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "FAILED: ${t.javaClass.simpleName}: ${t.message}", t)
            }
        }
    }
}