package kopac.core.engine

import kopac.core.flow.Cont
import java.util.concurrent.Semaphore

internal class Worker(internal val sr: Scheduler) {
    internal var workStack: Work? = null
    internal var handler: Handler? = null
    internal var event: WorkerEvent? = null

    companion object {
        internal val runningWork: ThreadLocal<Int> = run {
            val tl = ThreadLocal<Int>()
            tl.set(0)
            tl
        }
    }
}

internal class WorkerEvent(internal var me: Int) : Semaphore(-1) {
    internal var next: Int = -1
}

internal class IdleCont : Cont<Int>() {
    override fun doCont(worker: Worker, value: Int) {
        this.value = value
    }
}
