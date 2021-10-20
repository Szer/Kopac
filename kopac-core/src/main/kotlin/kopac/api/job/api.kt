package kopac.api.job

import kopac.api.initGlobalScheduler
import kopac.core.engine.Worker
import kopac.core.flow.Cont
import kopac.core.flow.KJob
import kopac.scheduler.run

fun <T> KJob<T>.run() = initGlobalScheduler().run(this)

object Job {
    fun <T> result(x: T): KJob<T> =
        object : KJob<T>() {
            override fun doJob(worker: Worker, cont: Cont<T>) {
                cont.doCont(worker, x)
            }
        }
}
