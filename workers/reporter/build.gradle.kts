/*
 * Copyright (C) 2023 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import com.google.cloud.tools.jib.gradle.JibTask

val dockerImagePrefix: String by project
val dockerImageTag: String by project
val dockerBaseImagePrefix: String by project
val dockerBaseImageTag: String by project

plugins {
    // Apply core plugins.
    application

    // Apply precompiled plugins.
    id("ort-server-kotlin-jvm-conventions")
    id("ort-server-publication-conventions")

    // Apply third-party plugins.
    alias(libs.plugins.jib)
}

tasks.withType<JibTask> {
    notCompatibleWithConfigurationCache("https://github.com/GoogleContainerTools/jib/issues/3132")
}

group = "org.eclipse.apoapsis.ortserver.workers"

dependencies {
    implementation(projects.config.configSpi)
    implementation(projects.dao)
    implementation(projects.model)
    implementation(projects.storage.storageSpi)
    implementation(projects.transport.transportSpi)
    implementation(projects.utils.logging)
    implementation(projects.workers.common)

    implementation(platform(libs.ortPackageConfigurationProviders))
    implementation(platform(libs.ortVersionControlSystems))
    implementation(libs.ortConfigUtils)
    implementation(libs.ortDownloader)
    implementation(libs.ortReporter)
    implementation(libs.typesafeConfig)

    runtimeOnly(projects.config.git)
    runtimeOnly(projects.config.github)
    runtimeOnly(projects.config.local)
    runtimeOnly(projects.config.secretFile)
    runtimeOnly(projects.secrets.azureKeyvault)
    runtimeOnly(projects.secrets.file)
    runtimeOnly(projects.secrets.scaleway)
    runtimeOnly(projects.secrets.vault)
    runtimeOnly(projects.storage.azureBlob)
    runtimeOnly(projects.storage.database)
    runtimeOnly(projects.storage.s3)
    runtimeOnly(projects.transport.activemqartemis)
    runtimeOnly(projects.transport.azureServicebus)
    runtimeOnly(projects.transport.kubernetes)
    runtimeOnly(projects.transport.rabbitmq)
    runtimeOnly(projects.transport.sqs)

    runtimeOnly(libs.log4jToSlf4j)
    runtimeOnly(libs.logback)
    runtimeOnly(platform(libs.ortReporters))

    testImplementation(testFixtures(projects.config.configSpi))
    testImplementation(testFixtures(projects.dao))
    testImplementation(testFixtures(projects.storage.storageSpi))
    testImplementation(testFixtures(projects.transport.transportSpi))
    testImplementation(projects.utils.test)
    testImplementation(testFixtures(projects.workers.common))

    testImplementation(libs.koinTest)
    testImplementation(libs.kotestAssertionsCore)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.mockk)
}

jib {
    from.image = "${dockerBaseImagePrefix}ort-server-reporter-worker-base-image:$dockerBaseImageTag"
    to.image = "${dockerImagePrefix}ort-server-reporter-worker:$dockerImageTag"

    container {
        mainClass = "org.eclipse.apoapsis.ortserver.workers.reporter.EntrypointKt"
        creationTime.set("USE_CURRENT_TIMESTAMP")
    }
}
