# TestNG Fixtures
This repo was designed after we encountered an issue in our daily work, that we cannot use custom fixtures for TestNG.
We had the issue that we used setup code in the test itself which is considered bad test design.  
So after some research we found this [repo](https://github.com/Top-Q/testng-fixture).  
Since our tech stack is mostly Kotlin we decided to move this repo to Kotlin and do some minor tweaks and improvements.

## Features

* Allows creating test fixtures as separated classes
* Allows linking fixtures to tests using annotations
* Allows sending responses from fixtures to tests
* Running all test fixtures concurrently
* Running all teardowns at the end of runs, also concurrently

## How to get it

Since this repo isn't uploaded to `maven Central` you have to import it in a different way.  
You just have to add  
```kotlin
repositories {
    maven { url 'https://jitpack.io' }
}
```
 and 
```kotlin
dependencies {
    implementation 'com.github.tim-hepster:TestNGFixtures:208fcf8b31'
}
```
 to your `build.gradle` file. After this just rebuild your gradle project and you should be able to use it.
 
## Usage
First you need to create a separate class which has the `@Listener` annotation. This class has to have some specific code 
in it as well to work correctly.

```kotlin
open class TemplateClass {
    var fixtureResult: Any? = null

    @BeforeMethod
    @Throws(Exception::class)
    fun setup(method: Method) {
        FixtureManager.getInstance()?.startFixtureSetupRuns()
        FixtureManager.getInstance()?.waitForAllFixtureSetupRunsToEnd()
        if (!FixtureManager.getInstance()?.isMethodAndHasFixture(method)!!) {
            return
        }
        val runResult: FixtureManager.FixtureRunResult = FixtureManager.getInstance()?.getFixtureRunResult(method)!!
        if (!runResult.getStatus()) {
            throw Exception("Test failed in fixture phase", runResult.getThrowable())
        }
        fixtureResult = runResult.getResult()
    }
}
```

To create a fixture you simply create a fixture class which holds all your fixture code. Below you can find an example
```kotlin
class FixtureExample : Fixture {

    @Throws(FixtureException::class)
    override fun setup(vararg params: String?): Any {
        if (params.isNotEmpty()) {
            println("In setup phase with params: " + params.contentToString())
            // some setup code
        } else {
            println("In setup phase")
            // some setup code
        }
    }

    override fun teardown() {
        println("Teardown after successful setup")
        // some teardown code
    }

    override fun failedTeardown() {
        println("Teardown after failed setup")
    }

    fun calculation(a: Int, b: Int): Int {
        return a + b
    }
}
```

To use this fixture you have to add it via an annotation to your test. Below you can find an example
```kotlin
class FixtureTest : AbstractTestCase() {
    @Test
    @WithFixture(value = FixtureExample::class)
    fun something() {
        println(FixtureExample().calculation(1, 3))
        // as you can see you can use functions which were implemented in the fixture
    }
}
```

**Important Limitation:** The fixtures will not work on tests that are part of a dependency chain since TestNG will not expose them to the methods list available for the `IMethodInterceptor` that is used to get the list of all discovered test methods.

### Ideas for the future
* make it work with parameter