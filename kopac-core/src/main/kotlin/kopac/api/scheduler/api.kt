package kopac.scheduler

import kopac.core.engine.*
import kopac.core.engine.FailWork
import kopac.core.engine.KillException
import kopac.core.engine.Worker
import kopac.core.flow.Cont
import kopac.core.flow.ContState
import kopac.core.flow.KJob
import java.util.concurrent.atomic.AtomicInteger

private var warned = false

data class Create(
    val foreground: Boolean? = null,
    val idleHandler: KJob<Int>? = null,
    val maxStackSize: Long? = null,
    val numWorkers: Int? = null,
    val topLevelHandler: ((Exception) -> KJob<Unit>)? = null
)

private class WtfStateMachine(val wr: Worker, val sr: Scheduler) {

    private var work = wr.workStack

    fun restart() {
        work = wr.workStack
        if (work == null) {
            enterScheduler()
        }
        else {
            workerLoop()
        }
    }

    fun enterScheduler() {
        work = sr.workStack
        if(work == null) {
            tryIdle()
        }
        sr.withSpinLock {
            enteredScheduler()
        }
    }
}

fun <T> Scheduler.run(me: Int) {
    Worker.runningWork.set(1)

    val wr = Worker(this)
    val iK = IdleCont()
    val wdm = (1L shl 32) / this.events.size
    wr.event = this.events[me]
    var isKilled = false



    while (!isKilled) {
        try {
            val wtf = WtfStateMachine(wr, this)
            wtf.restart()
        } catch (e: KillException) {
            this.kill()
            this.dec()
            isKilled = true
        } catch (e: Exception) {
            wr.workStack = FailWork(wr.workStack, e, wr.handler)
        }
    }
}

fun <T> Scheduler.run(xJ: KJob<T>): T {
    val xK = object : ContState<T, Exception, Int, Cont<Unit>>(state2 = 0) {
    }

    this.runOnThisThread(xJ, xK)

    val v = AtomicInteger(xK.state2!!)
    if (v.get() >= 0) {
        xK.lock.lock()
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
                xK.lock.unlock()
                xK.lockCond.signalAll()
                xK.lockCond.await()
            }
        }
    }

    return when (val e = xK.state1) {
        null -> xK.value // cont
        else -> throw Exception("Exception raised by KJob", e)
    }
}

fun createScheduler(c: Create): Scheduler {
    val create =
        StaticData.createScheduler ?: run {
            StaticData.init()
            StaticData.createScheduler!!
        }
    return create.invoke(
        c.foreground ?: false,
        c.idleHandler,
        c.maxStackSize ?: 0,
        when (val n = c.numWorkers) {
            null -> Runtime.getRuntime().availableProcessors()
            else -> {
                if (n < 1) {
                    error("Invalid number of workers specified: $n")
                }
                n
            }
        },
        c.topLevelHandler
    )
}
