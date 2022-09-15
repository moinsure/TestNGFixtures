import fixture.WithFixture
import fixtures.FixtureExample
import org.testng.annotations.Test

class FixtureTest : AbstractTestCase() {
    @Test
    @WithFixture(value = FixtureExample::class)
    fun something() {
        println(FixtureExample().calculation(1, 3))
        println("yay")
    }
}
