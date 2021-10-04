package kopac.core.engine

import kopac.core.flow.Cont
import kopac.core.flow.KJob
import kopac.core.util.SpinlockTTAS
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Scheduler {

    companion object {
        internal val globalLock = ReentrantLock()
        internal val globalLockCond = globalLock.newCondition()
    }

    private var workStack: Work? = null
    private val spinlock = SpinlockTTAS()
    private val lock = ReentrantLock()
    private val lockCond = lock.newCondition()
    private var numWorkStack = 0
    private var waiterStack = 0
    private var numActive = 0
    private var numPulseWaiters = 0
    private val events: Array<WorkerEvent> = emptyArray()

    private inline fun withSpinLock(crossinline action: () -> Unit) {
        spinlock.enter()
        action()
        spinlock.exit()
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
