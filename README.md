# FlowTester

A Kotlin DSL to help testing flows.

This is a example of a scenario to test a flow
```kotlin
@Test
fun flowTest() = runBlocking {
    val testFlow = flowOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    testFlow testScenario {
        take = 4

        doAt(position = 0) { nextValueEquals(0) }
        doAt(position = 1) { nextValueEquals(1) }
        then { nextValueEquals(2) }
        then { nextValueEquals(3) }

        afterAll { remainingValuesCount { 0 } }
    }
}
```

## Using in your projects

The libraries are published to [kotlinx](https://bintray.com/kotlin/kotlinx/kotlinx.coroutines) bintray repository,
linked to [JCenter](https://bintray.com/bintray/jcenter?filterByPkgName=kotlinx.coroutines) and 
pushed to [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3Aorg.jetbrains.kotlinx%20a%3Akotlinx-coroutines*).

### Maven

Add dependencies (you can also add other modules that you need):

```xml
<dependency>
    <groupId>org.pflanzmann</groupId>
    <artifactId>flow-tester</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

Add dependencies (you can also add other modules that you need):

```groovy
dependencies {
    implementation 'org.pflanzmann:flow-tester:0.1.0'
}
```

Make sure that you have either `jcenter()` in the list of repositories:
```
repository {
    jcenter()
}
```
