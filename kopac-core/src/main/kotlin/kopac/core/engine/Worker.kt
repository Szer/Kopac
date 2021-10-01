package kopac.core.engine

import java.util.concurrent.Semaphore

internal class Worker(internal val sr: Scheduler) {
    internal var workStack: Work? = null
    internal var handler: Handler? = null

    companion object {
        internal val runningWork: ThreadLocal<Int> = run {
            val tl = ThreadLocal<Int>()
            tl.set(0)
            tl
        }
    }
}

internal class WorkerEvent(internal val me: Int) : Semaphore(-1) {
    internal val next: Int = -1
}
