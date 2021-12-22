package kopac.core.engine

import kopac.core.flow.Cont
import kopac.core.util.ByRef
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

abstract class Handler {
    internal val lock = ReentrantLock()
    internal val lockCond = lock.newCondition()

    internal fun pulse(v: AtomicInteger) {
        val w = v.get()
        if (w <= 0 || v.getAndUpdate { w.inv() } != 0) {
            lock.lock()
            lockCond.signalAll()
            lock.unlock()
        }
    }

    internal fun wait(v: AtomicInteger) {
        if (v.get() >= 0) {
            lock.lock()
            val w = AtomicInteger(v.get())
            if (w.get() >= 0) {
                if (v.getAndUpdate { w.get().inv() } == 0) {
                    if (!warned && Worker.runningWork.get() > 0) {
                        warned = true
                        StaticData.writeLine(
                            "WARNING: You are making a blocking call to run a Hopac job " +
                                    "from within a Hopac job, which means that your program may " +
                                    "deadlock."
                        )
                        StaticData.writeLine("First occurrence (there may be others):")
                        StaticData.writeLine(Thread.currentThread().stackTrace.joinToString("\n"))
                    }
                    lockCond.signalAll()
                    lock.unlock()
                    lock.lock()
                }
            }
        }
    }

    companion object {

        private var warned = true

        internal fun printExn(header: String, e: Throwable) {
            var first = true
            var exn: Throwable? = e
            do {
                StaticData.writeLine((if (first) header else "Caused by: ") + e.toString())
                first = false
                exn = exn!!.cause
            } while (exn != null)
            StaticData.writeLine("No other causes.")
        }

        internal fun Handler?.doHandle(worker: ByRef<Worker>, e: Throwable) {
            if (this != null) {
                this.doHandle(worker, e)
            } else {
                doHandleNull(worker, e)
            }
        }

        internal fun doHandleNull(worker: ByRef<Worker>, e: Throwable) {
            val tlh = worker.value.sr.topLevelHandler
            if (tlh == null) {
                printExn("Unhandled exception: ", e)
            } else {
                val uK = UnitCont()
                worker.value.handler = uK
                tlh.invoke(e).doJob(worker, uK)
            }
        }

        internal fun<X> terminate(worker: ByRef<Worker>, xK: Cont<X>?) {
            xK?.doWork(worker)
        }

        private class UnitCont : Cont<Unit>() {
            override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
                printExn("Top level handler raised: ", e)
            }

            override fun doWork(worker: ByRef<Worker>) {}

            override fun doCont(worker: ByRef<Worker>, value: Unit) {}
        }
    }

    internal abstract fun doHandle(worker: ByRef<Worker>, e: Throwable)
}
