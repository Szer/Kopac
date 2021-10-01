package kopac.core.engine

abstract class Work : Handler() {
    internal var next: Work? = null
}

internal class FailWork(internal val e: Exception, internal val hr: Handler?) : Work() {
    constructor(next: Work?, e: Exception, hr: Handler?) : this(e, hr) {
        this.next = next
    }
}
