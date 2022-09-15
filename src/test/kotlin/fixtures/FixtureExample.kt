package fixtures

import fixture.Fixture
import fixture.FixtureException

class FixtureExample : Fixture {

    @Throws(FixtureException::class)
    override fun setup(vararg params: String?): Any {
        if (params.isNotEmpty()) {
            println("In setup phase with params: " + params.contentToString())
        } else {
            println("In setup phase")
        }
        return "Message from fixture"
    }

    override fun teardown() {
        println("Teardown after successful setup")
    }

    override fun failedTeardown() {
        println("Teardown after failed setup")
    }
}
