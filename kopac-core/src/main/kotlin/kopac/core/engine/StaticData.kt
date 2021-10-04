package kopac.core.engine

object StaticData {
    lateinit var writeLine: (String) -> Unit
    var globalScheduler: Scheduler? = null
    fun init() {
        // TODO reset of init
    }
}
