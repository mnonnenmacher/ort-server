/*
 * Copyright (C) 2024 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

package org.eclipse.apoapsis.ortserver.orchestrator

import io.kotest.core.spec.style.WordSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.contain
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should

import org.eclipse.apoapsis.ortserver.model.JobStatus

class OrtRunInfoTest : WordSpec({
    "getNextJobs" should {
        "return ANALYZER if no job was created yet" {
            val ortRunInfo = OrtRunInfo(
                id = 1,
                failed = false,
                configuredJobs = WorkerScheduleInfo.entries.toSet(),
                jobInfos = emptyMap()
            )

            ortRunInfo.getNextJobs() should containExactly(WorkerScheduleInfo.ANALYZER)
        }

        "return nothing if ANALYZER is still running" {
            val ortRunInfo = OrtRunInfo(
                id = 1,
                failed = false,
                configuredJobs = WorkerScheduleInfo.entries.toSet(),
                jobInfos = mapOf(
                    WorkerScheduleInfo.ANALYZER to WorkerJobInfo(
                        id = 1,
                        status = JobStatus.RUNNING
                    )
                )
            )

            ortRunInfo.getNextJobs() should beEmpty()
        }

        "return REPORTER if ANALYZER failed" {
            val ortRunInfo = OrtRunInfo(
                id = 1,
                failed = false,
                configuredJobs = WorkerScheduleInfo.entries.toSet(),
                jobInfos = mapOf(
                    WorkerScheduleInfo.ANALYZER to WorkerJobInfo(
                        id = 1,
                        status = JobStatus.FAILED
                    )
                )
            )

            ortRunInfo.getNextJobs() should containExactly(WorkerScheduleInfo.REPORTER)
        }

        "return REPORTER if config worker failed" {
            val ortRunInfo = OrtRunInfo(
                id = 1,
                failed = true,
                configuredJobs = WorkerScheduleInfo.entries.toSet(),
                jobInfos = emptyMap()
            )

            ortRunInfo.getNextJobs() should containExactly(WorkerScheduleInfo.REPORTER)
        }
    }
})
