package fixtures

import fixture.Fixture
import fixture.FixtureException

class FixtureExample : Fixture {

    @Throws(FixtureException::class)
    override fun setup(vararg params: String?): Any {
        val i = 1
        val b = 2
        if (params.isNotEmpty()) {
            println("In setup phase with params: " + params.contentToString())
        } else {
            println("In setup phase")
        }
        return b + i
    }

    override fun teardown() {
        println("Teardown after successful setup")
    }

    override fun failedTeardown() {
        println("Teardown after failed setup")
    }

    fun calculation(a: Int, b: Int): Int {
        return a + b
    }
}
