package kopac.core.util

import java.util.concurrent.atomic.AtomicInteger

// Provides a low overhead spinlock that is about as fast as
// possible in case of low contention, but also becomes slow in case of
// contention.  On every change of owner this spinlock implementation
// requires Omega(n) cache line transfers, where n is the number of threads
// waiting for the lock, and is thus inherently unscalable
class SpinlockTTAS {
    private var state: AtomicInteger = AtomicInteger(Open)

    companion object {
        const val Open = 0
        const val Locked = -1
    }

    internal fun enter() {
        var st = state.get()
        while (st == Locked || !state.compareAndSet(Open, Locked)) {
            st = state.get()
        }
    }

    internal fun exit() {
        assert(state.get() == Locked) // Must be locked!
        state.set(Open)
    }
}
