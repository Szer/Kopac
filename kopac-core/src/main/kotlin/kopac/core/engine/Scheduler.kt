package kopac.core.engine

import kopac.core.flow.Cont
import kopac.core.flow.KJob
import kopac.core.util.SpinlockTTAS
import kopac.scheduler.Create
import kopac.scheduler.run
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object KillException : Exception()

internal class AbortWork: Work() {
}

class Scheduler {

    companion object {

        object Global {
            var create = Create()

            fun defaultCreate(
                foreground: Boolean,
                idleHandler: KJob<Int>,
                maxStackSize: Long,
                numWorkers: Int,
                topLevelHandler: (Exception) -> KJob<Unit>
            ): Scheduler {
                val s = Scheduler()
                StaticData.init()
                s.topLevelHandler = topLevelHandler
                s.idleHandler = idleHandler
                s.waiterStack = -1
                s.numActive = numWorkers
                s.events = Array(numWorkers, ::WorkerEvent)
                val threads = Array(numWorkers) { i ->
                    val thread = Thread(
                        null,
                        { s.run(i) },
                        "Hopac.Worker $i/$numWorkers", maxStackSize
                    )
                }

                return s
            }
        }

        internal val globalLock = ReentrantLock()
        internal val globalLockCond = globalLock.newCondition()
    }

    internal var workStack: Work? = null
    private val spinlock = SpinlockTTAS()
    private val lock = ReentrantLock()
    private val lockCond = lock.newCondition()
    private var numWorkStack = 0
    private var waiterStack = 0
    private var numActive = 0
    private var numPulseWaiters = 0
    internal var events: Array<WorkerEvent> = emptyArray()
    internal var topLevelHandler: ((Exception) -> KJob<Unit>)? = null
    internal var idleHandler: KJob<Int>? = null

    internal inline fun withSpinLock(crossinline action: () -> Unit) {
        spinlock.enter()
        action()
        spinlock.exit()
    }

    internal fun kill() {
        val work = AbortWork()
        pushAll(work)
    }

    private fun unsafeSignal() {
        val waiter = waiterStack
        if (waiter >= 0) {
            val ev = events[waiter]
            waiterStack = ev.next
            assert(numActive >= 0)
            numActive += 1
            ev.release(Int.MAX_VALUE)
        }
    }

    internal fun inc() {
        withSpinLock {
            assert(numActive >= 0)
            numActive += 1
        }
    }

    internal fun dec() {
        val numActive = numActive - 1
        this.numActive = numActive
        assert(this.numActive >= 0)
        if (numActive == 0 && numPulseWaiters != 0) {
            lock.withLock {
                lockCond.signalAll()
            }
        }
    }

    internal fun pushAndDec(work: Work, last: Work, n: Int) {
        withSpinLock {
            last.next = workStack
            workStack = work
            numWorkStack += n
            numActive -= 1
            assert(numActive >= 0)
            unsafeSignal()
        }
    }

    internal fun pushAll(work: Work?) {
        if (work == null) return
        var n = 1
        var last: Work = work
        var next = last.next
        while(next != null) {
            n += 1
            last = next
            next = last.next
        }
        push(work, last, n)
    }

    internal fun push(work: Work, last: Work, n: Int) {
        withSpinLock {
            last.next = workStack
            workStack = work
            numWorkStack += n
            unsafeSignal()
        }
    }

    internal fun pushAllAndDec(work: Work?) {
        if (work == null) {
            dec()
        } else {
            var n = 1
            var last: Work = work
            var next: Work? = last.next

            while (next != null) {
                n += 1
                last = next
                next = last.next
            }

            pushAndDec(work, last, n)
        }
    }

    internal fun <T> runOnThisThread(tJ: KJob<T>, tK: Cont<T>) {
        val wr = Worker(this)
        val d = Worker.runningWork.get()
        Worker.runningWork.set(d + 1)
        inc()

        try {
            wr.handler = tK
            tJ.doJob(wr, tK)
        } catch (e: Exception) {
            wr.workStack = FailWork(wr.workStack, e, wr.handler)
        }

        pushAllAndDec(wr.workStack)
        Worker.runningWork.set(d)
    }
}
