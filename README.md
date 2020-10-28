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

### Introduction
This Kotlin DSL was made to test flows which emit the values of `BroadcastReceiverChannels`. Here the problem was that you could just test value the 
flow emitted at a fix time, but if you wanted to make sure that the flow did not emit something between your checks or maybe some value twice 
you had to cache all values asynchronously or do a clunky `when` statement to handle different emits.

FlowScenarios allows you to test every kind of flow synchronous with a Kotlin DSL and react for each emitted value on its own, without knowing
before what you expect. 

A `FlowScenario` collects on a flow and caches all values. You can then consume those values via polling or popping them at any given time
inside of this Scenario. You´re also able to call different useful methods as the count of unconsumed values, the count of invoked steps or 
if the flow finished with an `Exception`. The style of this library is heavily inspired by other Kotlin libraries and JUnit so that the use 
should be more instinctive. 

### More Examples

```
@Test
fun `testScenario 08`() = runBlockingTest {
   val testFlow = MutableStateFlow("")

   testFlow.testScenario {

       beforeAll {
           testFlow.emit("SETUP")
       }

       doAt(0) { value ->
           Assertions.assertEquals("SETUP", value)
           testFlow.emit("IDLE")
       }

       then {value ->
           Assertions.assertEquals("IDLE", value)
           testFlow.emit("REQUEST INFORMATION")
       }

       then {value ->
           Assertions.assertEquals("REQUEST INFORMATION", value)
           testFlow.emit("FETCHED INFORMATIONS")
       }

       doAt(3) {value ->
           Assertions.assertEquals("FETCHED INFORMATIONS", value)
           testFlow.emit("PROCESS INFORMATIONS")
       }

       then {value ->
           Assertions.assertEquals("PROCESS INFORMATIONS", value)
       }

       afterAll {
           afterAll { Assertions.assertEquals(1, numberOfUnconsumedValues()) }
       }
   }
}
```