package kopac.core.engine

import kopac.core.flow.KJob

object StaticData {
    lateinit var writeLine: (String) -> Unit
    var globalScheduler: Scheduler? = null
    var createScheduler: ((Boolean, KJob<Int>?, Int, Int, ((Exception) -> KJob<Unit>)?) -> Scheduler)? = null
    fun init() {
        // TODO reset of init
    }
}
