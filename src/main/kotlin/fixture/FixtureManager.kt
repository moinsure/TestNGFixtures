package fixture

import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.function.Consumer

/**
 * Responsible for running all the fixtures of the test methods and to allow
 * methods to query the result of the runs
 *
 * @author Itai Agmon
 */
class FixtureManager private constructor(private val numberOfThreads: Int) {
    private val fixtureToMethodsMap: MutableMap<FixtureDetails, MutableList<Method>>
    private val methodToResultMap: MutableMap<Method, Future<FixtureRunResult>>
    private var allProcessIsDone = false

    /**
     * Singleton
     */
    init {
        fixtureToMethodsMap = HashMap()
        methodToResultMap = HashMap()
    }

    /**
     * Add test methods with fixture annotation
     *
     * @param methods
     */
    fun addMethods(methods: List<Method>) {
        methods.forEach(
            Consumer { method: Method ->
                addMethod(
                    method
                )
            }
        )
    }

    /**
     * Add single test methods with fixture annotation
     *
     * @param method
     */
    fun addMethod(method: Method) {
        requireNotNull(method.getAnnotation(WithFixture::class.java)) { "Can't add test method without Fixture" }
        val details = FixtureDetails(method)
        val methodList: MutableList<Method> = if (fixtureToMethodsMap.containsKey(details)) {
            fixtureToMethodsMap[details]!!
        } else {
            ArrayList()
        }
        methodList.add(method)
        fixtureToMethodsMap[details] = methodList
    }

    /**
     * Run in parallel all the setup methods methods of the fixtures.
     */
    fun startFixtureSetupRuns() {
        if (fixtureToMethodsMap.isEmpty()) {
            return
        }
        allProcessIsDone = false
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        for (details in fixtureToMethodsMap.keys) {
            val future: Future<FixtureRunResult> = executor.submit(
                Callable {
                    var fixtureRunResult: FixtureRunResult? = null
                    var fixtureObject: Fixture? = null
                    try {
                        fixtureObject = details.getFixtureClass()!!.newInstance()
                        val result: Any? = details.params?.let { fixtureObject?.setup(*it) }
                        fixtureRunResult = result?.let { FixtureRunResult(true, it) }
                        if (fixtureObject != null) {
                            if (fixtureRunResult != null) {
                                fixtureRunResult.setFixture(fixtureObject)
                            }
                        }
                    } catch (t: Throwable) {
                        fixtureRunResult = FixtureRunResult(false, t)
                        if (fixtureObject != null) {
                            fixtureRunResult.setFixture(fixtureObject)
                        }
                    }
                    fixtureRunResult
                }
            )
            fixtureToMethodsMap[details]!!.forEach(
                Consumer { m: Method ->
                    methodToResultMap[m] = future
                }
            )
        }
        fixtureToMethodsMap.clear()
    }

    fun startFixtureTeardownRun() {
        if (methodToResultMap.isEmpty()) {
            return
        }
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        val teardownFuture: MutableList<Future<Boolean>> = ArrayList()
        for (setupFuture in methodToResultMap.values) {
            if (!setupFuture.isDone) {
                continue
            }
            try {
                if (setupFuture.get().isTeardownScheduled()) {
                    continue
                }
            } catch (e: InterruptedException) {
                continue
            } catch (e: ExecutionException) {
                continue
            }
            val future = executor.submit<Boolean> {
                val fixtureRunResult = setupFuture.get()
                try {
                    if (fixtureRunResult.getStatus()) {
                        fixtureRunResult.getFixture()?.teardown()
                    } else {
                        fixtureRunResult.getFixture()?.failedTeardown()
                    }
                } catch (t: Throwable) {
                    return@submit false
                }
                true
            }
            try {
                setupFuture.get().setTeardownScheduled(true)
            } catch (_: InterruptedException) {
            } catch (_: ExecutionException) {
            }
            teardownFuture.add(future)
        }
        // Waiting for all the tear downs to end
        while (!teardownFuture.stream().map { obj: Future<Boolean> -> obj.isDone }
            .allMatch { t: Boolean? -> t!! }
        ) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
            }
        }
    }

    /**
     * Get the fixture run result of a single test method.
     *
     * @param method
     * @return Fixture run result
     */
    fun getFixtureRunResult(method: Method): FixtureRunResult? {
        if (null == methodToResultMap[method]) {
            return null
        }
        return if (!methodToResultMap[method]!!.isDone) {
            null
        } else try {
            methodToResultMap[method]!!.get()
        } catch (e: InterruptedException) {
            null
        } catch (e: ExecutionException) {
            null
        }
    }

    /**
     * Is the given method was added to the manager, had a fixture attached and the
     * fixture was already executed. **The method will only return true if the
     * method has fixture and the manager started to run the fixtures**
     *
     * @param method
     * @return
     */
    fun isMethodHasFixture(method: Method): Boolean {
        return methodToResultMap.containsKey(method)
    }

    /**
     * Wait for all the fixture runs to finish
     */
    fun waitForAllFixtureSetupRunsToEnd() {
        if (allProcessIsDone) {
            return
        }
        var finished = true
        do {
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
            }
            finished = true
            for (future in methodToResultMap.values) {
                finished = finished and future.isDone
            }
        } while (!finished)
        allProcessIsDone = true
    }

    /**
     * The result of a single fixture run
     *
     * @author Itai Agmon
     */
    class FixtureRunResult {
        /**
         * True if the execution was successful and only if the execution was
         * successful.
         */
        private var status: Boolean = false
        val throwable: Throwable?
        val result: Any?
        private var fixture: Fixture? = null
        private var teardownScheduled = false

        private constructor(status: Boolean, throwable: Throwable) : super() {
            this.status = status
            this.throwable = throwable
            result = null
        }

        constructor(status: Boolean, result: Any) {
            this.status = status
            throwable = null
            this.result = result
        }

        fun getStatus(): Boolean {
            return status
        }

        @JvmName("getThrowable1")
        fun getThrowable(): Throwable? {
            return throwable
        }

        @JvmName("getResult1")
        fun getResult(): Any? {
            return result
        }

        fun getFixture(): Fixture? {
            return fixture
        }

        fun setFixture(fixture: Fixture) {
            this.fixture = fixture
        }

        fun isTeardownScheduled(): Boolean {
            return teardownScheduled
        }

        fun setTeardownScheduled(teardownExecuted: Boolean) {
            this.teardownScheduled = teardownExecuted
        }
    }

    /**
     * Holds the details of the fixture. Allows comparison between fixtures.
     *
     * @author Itai Agmon
     */
    private class FixtureDetails(method: Method?) {
        private val fixtureClass: Class<out Fixture?>?
        val params: Array<String>?

        init {
            requireNotNull(method) { "Method can't be null" }
            requireNotNull(method.getAnnotation(WithFixture::class.java)) { "Method without annotation" }
            val fixture: WithFixture = method.getAnnotation(WithFixture::class.java)
            fixtureClass = fixture.value.java
            params = fixture.params
        }

        fun getFixtureClass(): Class<out Fixture?>? {
            return fixtureClass
        }

        override fun hashCode(): Int {
            var hash = 7
            hash = 31 * hash + (fixtureClass?.name?.hashCode() ?: 0)
            if (params != null) {
                for (param in params) {
                    hash = 31 * hash + (param?.hashCode() ?: 0)
                }
            }
            return hash
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null) return false
            return if (this.javaClass != o.javaClass) false else o.hashCode() == hashCode()
        }
    }

    companion object {
        private const val DEFAULT_NUMBER_OF_THREADS = 30

        /**
         * Singleton
         */
        private var instance: FixtureManager? = null
        fun getInstance(numberOfThreads: Int): FixtureManager? {
            if (null == instance) {
                instance = FixtureManager(numberOfThreads)
            }
            return instance
        }

        fun getInstance(): FixtureManager? {
            if (null == instance) {
                instance = FixtureManager(DEFAULT_NUMBER_OF_THREADS)
            }
            return instance
        }
    }
}
