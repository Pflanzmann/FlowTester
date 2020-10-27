import org.apache.tools.ant.types.resources.selectors.Date

val ownVersion: String = "0.1.0"

plugins {
    kotlin("jvm") version ("1.4.10")
    id("maven-publish")
    id("com.jfrog.bintray") version "1.8.4"
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val artifactName = "flow-tester"
val artifactGroup = "org.pflanzmann"

val pomUrl = "..."
val pomScmUrl = "..."
val pomIssueUrl = "..."
val pomDesc = "..."
val pomScmConnection = "..."
val pomScmDevConnection = "..."

val githubRepo = "https://github.com/Pflanzmann/FlowTester"
val githubReadme = "..."

val pomLicenseName = "The Apache Software License, Version 2.0"
val pomLicenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
val pomLicenseDist = "repo"

val pomDeveloperId = "Pflanzmann"
val pomDeveloperName = "Ronny Brzeski"

publishing {
    publications {
        create<MavenPublication>(artifactName) {
            groupId = artifactGroup
            artifactId = artifactName
            version = ownVersion
            from(components["java"])
            artifact(sourcesJar)

            pom.withXml {
                asNode().apply {
                    appendNode("description", pomDesc)
                    appendNode("name", rootProject.name)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", pomDeveloperId)
                        appendNode("name", pomDeveloperName)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                        appendNode("connection", pomScmConnection)
                    }
                }
            }
        }
    }
}

bintray {
    user = System.getenv("bintrayUser")
    key = System.getenv("bintrayApiKey")

    publish = true

    setPublications(artifactName)

    pkg.apply {
        repo = "flow-tester"
        name = rootProject.name
        setLicenses("Apache-2.0")
        setLabels("Kotlin")
        vcsUrl = pomScmUrl
        websiteUrl = pomUrl
        issueTrackerUrl = pomIssueUrl
        githubRepo = githubRepo
        githubReleaseNotesFile = githubReadme

        // Configure version
        version.apply {
            name =  ownVersion
            desc = pomDesc
            released = Date().datetime
            vcsTag =  ownVersion
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.0-M1")


    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.0-M1")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}
