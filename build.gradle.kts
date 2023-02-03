/*
 * Copyright (c) 2022-2023, Valaphee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.palantir.git-version") version "0.15.0"
    kotlin("jvm") version "1.8.10"
}

group = "com.valaphee"
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
version = "${details.lastTag}.${details.commitDistance}"

repositories { mavenCentral() }

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin:3.21.12")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = "17" } }

    build { dependsOn(shadowJar) }

    jar { manifest { attributes(mapOf("Main-Class" to "com.valaphee.protod.MainKt")) } }

    shadowJar {
        archiveFileName.set("protod.jar")
    }
}
