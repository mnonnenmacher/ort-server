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

import org.eclipse.apoapsis.ortserver.model.JobStatus

internal class OrtRunInfo(
    val id: Long,
    val failed: Boolean,
    val configuredJobs: Set<WorkerScheduleInfo>,
    val jobInfos: Map<WorkerScheduleInfo, WorkerJobInfo>
) {
    fun getNextJobs(): Set<WorkerScheduleInfo> = WorkerScheduleInfo.entries.filterTo(mutableSetOf()) { canRun(it) }

    private fun canRun(info: WorkerScheduleInfo): Boolean =
        isConfigured(info) &&
                !wasScheduled(info) &&
                canRunWithFailureState(info) &&
                info.dependsOn.all { isCompleted(it) } &&
                info.runsAfterTransitively.none { isPending(it) }

    private fun isConfigured(info: WorkerScheduleInfo): Boolean = info in configuredJobs

    private fun wasScheduled(info: WorkerScheduleInfo): Boolean = jobInfos.containsKey(info)

    private fun canRunWithFailureState(info: WorkerScheduleInfo): Boolean =
        info.runAfterFailure || !isFailed()

    private fun isFailed(): Boolean = failed || jobInfos.any { it.value.status == JobStatus.FAILED }

    private fun isCompleted(info: WorkerScheduleInfo): Boolean = jobInfos[info]?.status?.final == true

    private fun isPending(info: WorkerScheduleInfo): Boolean =
        isConfigured(info) &&
                !isCompleted(info) &&
                canRunWithFailureState(info) &&
                info.dependsOn.all { wasScheduled(it) || isPending(it) }
}

internal class WorkerJobInfo(
    val id: Long,
    val status: JobStatus
)
