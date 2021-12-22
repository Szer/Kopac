package kopac.api

import kopac.core.engine.Scheduler
import kopac.core.engine.StaticData
import kopac.api.scheduler.createScheduler
import kotlin.concurrent.withLock

fun reallyInitGlobalScheduler(): Scheduler {
    StaticData.init()
    return Scheduler.globalLock.withLock {
        StaticData.globalScheduler ?: run {
            val sr = createScheduler(Scheduler.Companion.Global.create)
            StaticData.globalScheduler = sr
            sr
        }
    }
}

fun initGlobalScheduler(): Scheduler =
    StaticData.globalScheduler ?: reallyInitGlobalScheduler ()
