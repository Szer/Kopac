package kopac.core.flow

import kopac.core.engine.Worker

abstract class KJob<T> {
    internal abstract fun doJob(worker: Worker, cont: Cont<T>)
}
