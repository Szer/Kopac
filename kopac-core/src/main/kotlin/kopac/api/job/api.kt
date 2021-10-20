package kopac.api.job

import kopac.api.initGlobalScheduler
import kopac.core.flow.KJob
import kopac.scheduler.run

fun <T> KJob<T>.run() = initGlobalScheduler().run(this)
