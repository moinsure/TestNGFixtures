package fixture

interface Fixture {

    @Throws(FixtureException::class)
    fun setup(vararg params: String?): Any?

    fun teardown()

    fun failedTeardown()
}
