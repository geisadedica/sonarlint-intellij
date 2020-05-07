package org.sonarlint.intellij.its.utils

import com.intellij.remoterobot.stepsProcessing.StepLogger
import com.intellij.remoterobot.stepsProcessing.StepWorker

object StepsLogger {
    private var initializaed = false
    @JvmStatic
    fun init() = synchronized(initializaed) {
        if (initializaed.not()) {
            StepWorker.registerProcessor(StepLogger())
            initializaed = true
        }
    }
}
