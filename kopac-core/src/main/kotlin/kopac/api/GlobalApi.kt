package kopac.api

import kopac.core.engine.Scheduler
import kopac.core.engine.StaticData
import kotlin.concurrent.withLock

fun reallyInitGlobalScheduler(): Scheduler {
    StaticData.init()
    Scheduler.globalLock.withLock {
        StaticData.globalScheduler ?: run {
            val sr = Scheduler.create()
            StaticData.globalScheduler = sr
            sr
        }
    }
}

fun initGlobalScheduler(): Scheduler =
    StaticData.globalScheduler ?: reallyInitGlobalScheduler ()
