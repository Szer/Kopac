package kopac.core.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal open class ManualResetEvent {
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  @Volatile
  private var open = false

  @Throws(InterruptedException::class)
  fun waitOne() {
    lock.withLock {
      while(!open) {
        condition.await()
      }
    }
  }

  @Throws(InterruptedException::class)
  fun waitOne(milliseconds: Long): Boolean {
    lock.withLock {
      if (open) return true
      condition.await(milliseconds, TimeUnit.MILLISECONDS)
      return open
    }
  }

  fun set() { //open start
    lock.withLock {
      open = true
      condition.signalAll()
    }
  }

  fun reset() { //close stop
    open = false
  }

  fun isSet() = open
}
