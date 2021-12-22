package kopac.core.engine

import kopac.core.util.ByRef

abstract class Work : Handler() {
    internal var next: Work? = null

    internal abstract fun doWork(worker: ByRef<Worker>)
}

internal class FailWork(internal val e: Throwable, internal val hr: Handler?) : Work() {
    constructor(next: Work?, e: Exception, hr: Handler?) : this(e, hr) {
        this.next = next
    }

    override fun doWork(worker: ByRef<Worker>) {
        hr.doHandle(worker, e)
    }

    override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
        hr.doHandle(worker, e)
    }
}
