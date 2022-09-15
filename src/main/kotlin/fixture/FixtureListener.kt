package fixture

import org.testng.IMethodInstance
import org.testng.ISuite
import org.testng.ITestContext
import org.testng.internal.ConstructorOrMethod
import java.util.stream.Collectors

class FixtureListener {

    fun intercept(methods: List<IMethodInstance>, context: ITestContext?): List<IMethodInstance>? {
        val testMethodsWithFixture = methods.stream()
            .map { m: IMethodInstance ->
                m.method.constructorOrMethod
            }
            .filter { m: ConstructorOrMethod ->
                m.method.isAnnotationPresent(
                    WithFixture::class.java
                )
            }
            .map { m: ConstructorOrMethod -> m.method }
            .collect(Collectors.toList())
        FixtureManager.getInstance()?.addMethods(testMethodsWithFixture)
        return methods
    }

    fun onFinish(suite: ISuite?) {
        FixtureManager.getInstance()?.startFixtureTeardownRun()
    }

    fun onStart(suite: ISuite?) {}
}
