package kopac.core.flow

import kopac.core.engine.Handler
import kopac.core.engine.Handler.Companion.doHandle
import kopac.core.engine.Worker
import kopac.core.util.ByRef

abstract class KJob<T> {
    internal abstract fun doJob(worker: ByRef<Worker>, cont: Cont<T>)
}

abstract class JobCont<X, Y>: KJob<Y>() {
    private var xJ: KJob<X>? = null

    internal fun internalInit(xJ: KJob<X>): JobCont<X, Y> {
        this.xJ = xJ
        return this
    }

    abstract fun doMore(): JobContCont<X, Y>

    override fun doJob(worker: ByRef<Worker>, cont: Cont<Y>) {
        val xK = doMore()
        xK.yK = cont
        this.xJ!!.doJob(worker, xK)
    }

}

abstract class JobContCont<X, Y>: Cont<X>() {
    internal var yK: Cont<Y>? = null
}

abstract class ContBind<X, Y>: JobContCont<X, Y>() {
    abstract fun doMore(x: X): KJob<Y>

    override fun doWork(worker: ByRef<Worker>) {
        doMore(value).doJob(worker, yK!!)
    }

    override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
        yK.doHandle(worker, e)
    }

    override fun doCont(worker: ByRef<Worker>, value: X) {
        doMore(value).doJob(worker, yK!!)
    }

    // TODO getProc
}

abstract class ContMap<X, Y>: JobContCont<X, Y>() {
    abstract fun doMore(x: X): Y

    override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
        yK.doHandle(worker, e)
    }

    override fun doWork(worker: ByRef<Worker>) {
        yK!!.doCont(worker, doMore(value))
    }

    override fun doCont(worker: ByRef<Worker>, value: X) {
        yK!!.doCont(worker, doMore(value))
    }

    // TODO getProc
}
