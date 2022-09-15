import fixture.WithFixture
import fixtures.FixtureExample
import org.testng.annotations.Test

class FixtureTest : AbstractTestCase() {
    @Test
    @WithFixture(value = FixtureExample::class, params = ["1.4", "1.3"])
    fun something() {
        println("yay")
    }
}
