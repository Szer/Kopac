package kopac.core.engine

import kopac.core.flow.Cont
import kopac.core.util.ByRef
import kopac.core.util.ManualResetEvent

internal class Worker(internal val sr: Scheduler, internal var event: WorkerEvent?) {
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

internal class WorkerEvent(internal var me: Int) : ManualResetEvent() {
    internal var next: Int = -1
}

internal class IdleCont : Cont<Int>() {
    override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
        doHandleNull(worker, e)
    }

    override fun doWork(worker: ByRef<Worker>) { }
    override fun doCont(worker: ByRef<Worker>, value: Int) {
        this.value = value
    }
}
