package kopac.scheduler

import kopac.core.engine.Scheduler
import kopac.core.engine.StaticData
import kopac.core.engine.Worker
import kopac.core.flow.Cont
import kopac.core.flow.ContState
import kopac.core.flow.KJob
import java.util.concurrent.atomic.AtomicInteger

private var warned = false

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
