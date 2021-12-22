package kopac.api.job

import kotlin.test.Test
import kotlin.test.assertEquals

internal class JobTest {
    @Test
    fun `Job-result should return value`() {
        assertEquals(1, Job.result(1).run())
    }

    @Test
    fun `Job-result and bind should return value`() {
        assertEquals(2, Job.result(1).bind { x ->
            Job.result(x + 1)
        }.run())
    }

    @Test
    fun `Job-result and map should return value`() {
        assertEquals(3, Job.result(1).map { x ->
            x + 2
        }.run())
    }
}
