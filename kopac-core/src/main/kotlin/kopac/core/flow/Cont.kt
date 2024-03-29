package kopac.core.flow

import kopac.core.engine.Work
import kopac.core.engine.Worker
import kopac.core.util.ByRef

abstract class Cont<T> : Work() {
    private var maybeValue: T? = null
    internal var value: T
        get() = maybeValue!!
        set(value) {
            maybeValue = value
        }

    internal abstract fun doCont(worker: ByRef<Worker>, value: T)
}

internal abstract class ContState<T, S1, S2, S3>(
    internal var state1: S1? = null,
    internal var state2: S2? = null,
    internal var state3: S3? = null,
) : Cont<T>() {

    fun init(s1: S1?, s2: S2?, s3: S3?): Cont<T> {
        state1 = s1
        state2 = s2
        state3 = s3
        return this
    }
}
