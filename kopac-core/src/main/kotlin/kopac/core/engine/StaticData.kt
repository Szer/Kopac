package kopac.core.engine

import kopac.core.flow.KJob

object StaticData {
    private var isInit = false
    lateinit var writeLine: (String) -> Unit
    var globalScheduler: Scheduler? = null
    var createScheduler: ((Boolean, KJob<Int>?, Long, Int, ((Throwable) -> KJob<Unit>)?) -> Scheduler)? = null
    fun init() {
        if (!isInit) {
            createScheduler = Scheduler.Companion.Global::defaultCreate
            writeLine = ::println
            isInit = true
        }
    }
}
