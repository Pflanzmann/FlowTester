# FlowTester

A Kotlin DSL to help test flows.

This is an example of a scenario to test a flow.
```kotlin
@Test
fun flowTest() = runBlocking {
    val testFlow = flowOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

    testFlow testScenario {
        take = 4

        doAt(0) { Assertions.assertEquals(0, pollValue) }
        doAt(1) { Assertions.assertEquals(1, pollValue) }
        then { Assertions.assertEquals(2, pollValue) }
        then { Assertions.assertEquals(3, pollValue) }
    
        afterAll { Assertions.assertEquals(0, numberOfUnconsumedValues()) }
    }
}
```

## Using in your projects

The libraries are published to [FlowTester](https://bintray.com/beta/#/pflanzmann/flow-tester/FlowTester) bintray repository and
linked to [JCenter](https://bintray.com/bintray/jcenter?filterByPkgName=kotlinx.coroutines).

### Maven

Add dependencies:

```xml
<dependency>
    <groupId>org.pflanzmann</groupId>
    <artifactId>flow-tester</artifactId>
    <version>0.2.0</version>
</dependency>
```

### Gradle

Add dependencies:

```groovy
dependencies {
    implementation 'org.pflanzmann:flow-tester:0.2.0'
}
```

Make sure that you have `jcenter()` in the list of repositories:
```
repository {
    jcenter()
}
```
