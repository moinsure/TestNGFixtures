import fixture.FixtureListener
import fixture.FixtureManager
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Listeners
import java.lang.reflect.Method

@Listeners(FixtureListener::class)
open class AbstractTestCase {

    var fixtureResult: Any? = null
    @BeforeMethod
    @Throws(Exception::class)
    fun setup(method: Method) {
        FixtureManager.getInstance()?.startFixtureSetupRuns()
        FixtureManager.getInstance()?.waitForAllFixtureSetupRunsToEnd()
        if (!FixtureManager.getInstance()?.isMethodHasFixture(method)!!) {
            return
        }
        val runResult: FixtureManager.FixtureRunResult = FixtureManager.getInstance()?.getFixtureRunResult(method)!!
        if (!runResult.getStatus()) {
            throw Exception("Test failed in fixture phase", runResult.getThrowable())
        }
        fixtureResult = runResult.getResult()
    }
}