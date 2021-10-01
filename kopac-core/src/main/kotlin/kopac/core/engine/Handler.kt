package kopac.core.engine

import java.util.concurrent.locks.ReentrantLock

abstract class Handler {
    internal val lock = ReentrantLock()
    internal val lockCond = lock.newCondition()
}
