package kopac.core.flow

import kopac.core.engine.Worker
import kopac.core.util.ByRef

abstract class KJob<T> {
    internal abstract fun doJob(worker: ByRef<Worker>, cont: Cont<T>)
}
