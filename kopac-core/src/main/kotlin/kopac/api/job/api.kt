package kopac.api.job

import kopac.api.initGlobalScheduler
import kopac.core.engine.Worker
import kopac.api.scheduler.run
import kopac.core.flow.*
import kopac.core.util.ByRef

fun <T> KJob<T>.run() = initGlobalScheduler().run(this)

fun <X, Y> KJob<X>.bind(x2yJ: (X) -> KJob<Y>): KJob<Y> =
    object : JobCont<X, Y>() {
        override fun doMore() =
            object : ContBind<X, Y>() {
                override fun doMore(x: X) = x2yJ(x)
            }
    }.internalInit(this)

fun <X, Y> KJob<X>.map(x2y: (X) -> Y): KJob<Y> =
    object : JobCont<X, Y>() {
        override fun doMore() =
            object : ContMap<X, Y>() {
                override fun doMore(x: X) = x2y(x)
            }
    }.internalInit(this)

object Job {
    fun <T> result(x: T): KJob<T> =
        object : KJob<T>() {
            override fun doJob(worker: ByRef<Worker>, cont: Cont<T>) {
                cont.doCont(worker, x)
            }
        }
}
