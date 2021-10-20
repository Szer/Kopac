package kopac.api.job

import kotlin.test.Test
import kotlin.test.assertEquals

internal class JobTest {
    @Test
    fun `Job-result should return value`() {
        assertEquals(1, Job.result(1).run())
    }
}
