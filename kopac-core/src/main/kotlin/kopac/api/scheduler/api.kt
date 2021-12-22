package kopac.api.scheduler

import kopac.core.engine.*
import kopac.core.engine.FailWork
import kopac.core.engine.KillException
import kopac.core.engine.Worker
import kopac.core.flow.Cont
import kopac.core.flow.ContState
import kopac.core.flow.KJob
import kopac.core.util.ByRef
import java.util.concurrent.atomic.AtomicInteger

data class Create(
    val isDaemon: Boolean? = null,
    val idleHandler: KJob<Int>? = null,
    val maxStackSize: Long? = null,
    val numWorkers: Int? = null,
    val topLevelHandler: ((Throwable) -> KJob<Unit>)? = null
)

private class SchedulerStateMachine(val sr: Scheduler, val me: Int) {

    private val wr = Worker(sr, sr.events[me])
    private val wrByRef = ByRef(wr)
    private val iK = IdleCont()
    private val wdm = (1L shl 32) / sr.events.size

    private enum class STATE {
        RESTART,
        WORKER_LOOP,
        ENTER_SCHEDULER,
        ENTERED_SCHEDULER,
        SCHEDULER_GOT_SOME,
        EXIT_AND_TRY_IDLE,
        TRY_IDLE,
    }

    private var isKilled = false
    private var state = STATE.RESTART
    private var work = wr.workStack

    fun start() {
        while (!isKilled) {
            try {
                when (state) {
                    STATE.RESTART -> {
                        work = wr.workStack
                        if (work == null) {
                            state = STATE.ENTER_SCHEDULER
                        } else {
                            state = STATE.WORKER_LOOP
                        }
                    }
                    STATE.WORKER_LOOP -> {
                        assert(work != null)

                        wr.handler = work
                        var next = work!!.next
                        if (next != null && sr.workStack == null) {
                            sr.pushAll(next)
                            next = null
                        }
                        wr.workStack = next
                        work!!.doWork(wrByRef)
                        work = wr.workStack
                        if (work != null)
                            state = STATE.WORKER_LOOP
                        else {
                            wr.handler = null
                            state = STATE.ENTER_SCHEDULER
                        }
                    }
                    STATE.ENTER_SCHEDULER -> {
                        if (work == null) {
                            state = STATE.TRY_IDLE
                        } else {
                            sr.spinlock.enter()
                            state = STATE.ENTERED_SCHEDULER
                        }
                    }
                    STATE.ENTERED_SCHEDULER -> {
                        work = sr.workStack
                        if (work == null)
                            state = STATE.EXIT_AND_TRY_IDLE
                        else
                            state = STATE.SCHEDULER_GOT_SOME
                    }
                    STATE.SCHEDULER_GOT_SOME -> {
                        assert(work != null)
                        var last = work
                        val numWorkStack = sr.numWorkStack
                        var n = ((numWorkStack) * wdm shr 32).toInt() + 1
                        sr.numWorkStack = numWorkStack - n
                        n -= 1
                        while (n > 0) {
                            last = last!!.next
                            n -= 1
                        }
                        val next = last!!.next
                        last.next = null
                        sr.workStack = null
                        if (next != null) {
                            sr.unsafeSignal()
                        }
                        sr.spinlock.exit()
                        state = STATE.WORKER_LOOP
                    }
                    STATE.EXIT_AND_TRY_IDLE -> {
                        sr.spinlock.exit()
                        state = STATE.TRY_IDLE
                    }
                    STATE.TRY_IDLE -> {
                        iK.value = Int.MAX_VALUE
                        val iJ = sr.idleHandler
                        if (iJ != null) {
                            wr.handler = iK
                            iJ.doJob(wrByRef, iK)
                        }

                        if (iK.value == 0)
                            state = STATE.RESTART
                        else {
                            sr.spinlock.enter()
                            work = sr.workStack
                            if (work != null)
                                state = STATE.SCHEDULER_GOT_SOME
                            else {
                                sr.unsafeWait(iK.value, wr.event!!)
                                state = STATE.ENTERED_SCHEDULER
                            }
                        }
                    }
                }
            } catch (e: KillException) {
                sr.kill()
                sr.dec()
                isKilled = true
            } catch (e: Exception) {
                wr.workStack = FailWork(wr.workStack, e, wr.handler)
            }
        }
    }
}

fun Scheduler.run(me: Int) {
    Worker.runningWork.set(1)
    SchedulerStateMachine(this, me).start()
}

fun <T> Scheduler.run(xJ: KJob<T>): T {
    val xK = object : ContState<T, Throwable, AtomicInteger, Cont<Unit>>(state2 = AtomicInteger(0)) {
        override fun doHandle(worker: ByRef<Worker>, e: Throwable) {
            terminate(worker, state3)
            state1 = e
            pulse(state2!!)
        }

        override fun doWork(worker: ByRef<Worker>) {
            terminate(worker, state3)
            pulse(state2!!)
        }

        override fun doCont(worker: ByRef<Worker>, value: T) {
            terminate(worker, state3)
            this.value = value
            pulse(state2!!)
        }
    }

    this.runOnThisThread(xJ, xK)
    xK.wait(xK.state2!!)

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
        c.isDaemon ?: true,
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
