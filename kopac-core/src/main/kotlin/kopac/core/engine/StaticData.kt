package kopac.core.engine

import kopac.core.flow.KJob

object StaticData {
    private var isInit = false
    lateinit var writeLine: (String) -> Unit
    var globalScheduler: Scheduler? = null
    var createScheduler: ((Boolean, KJob<Int>?, Int, Int, ((Exception) -> KJob<Unit>)?) -> Scheduler)? = null
    fun init() {
        if (!isInit) {
            StaticData.writeLine = ::println


            isInit = true
        }
    }
}
