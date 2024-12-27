/*
 * Copyright (C) 2022 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

import org.eclipse.apoapsis.ortserver.model.JobConfigurations
import org.eclipse.apoapsis.ortserver.model.JobStatus
import org.eclipse.apoapsis.ortserver.model.WorkerJob
import org.eclipse.apoapsis.ortserver.model.orchestrator.AdvisorRequest
import org.eclipse.apoapsis.ortserver.model.orchestrator.AnalyzerRequest
import org.eclipse.apoapsis.ortserver.model.orchestrator.EvaluatorRequest
import org.eclipse.apoapsis.ortserver.model.orchestrator.NotifierRequest
import org.eclipse.apoapsis.ortserver.model.orchestrator.ReporterRequest
import org.eclipse.apoapsis.ortserver.model.orchestrator.ScannerRequest
import org.eclipse.apoapsis.ortserver.model.util.asPresent
import org.eclipse.apoapsis.ortserver.transport.AdvisorEndpoint
import org.eclipse.apoapsis.ortserver.transport.AnalyzerEndpoint
import org.eclipse.apoapsis.ortserver.transport.EvaluatorEndpoint
import org.eclipse.apoapsis.ortserver.transport.NotifierEndpoint
import org.eclipse.apoapsis.ortserver.transport.ReporterEndpoint
import org.eclipse.apoapsis.ortserver.transport.ScannerEndpoint

internal class JobScheduler(private val repositories: WorkerJobRepositories) {
    fun createAndScheduleJob(context: WorkerScheduleContext, info: WorkerScheduleInfo) {
        val job = createJob(context.jobConfigs(), context.ortRun.id, info) ?: return
        sendMessage(context, info, job.id)
        updateJobStatus(info, job.id)
    }

    private fun createJob(jobConfigs: JobConfigurations, ortRunId: Long, info: WorkerScheduleInfo): WorkerJob? {
        return when (info) {
            WorkerScheduleInfo.ADVISOR -> {
                jobConfigs.advisor?.let { repositories.advisorJobRepository.create(ortRunId, it) }
            }

            WorkerScheduleInfo.ANALYZER -> {
                repositories.analyzerJobRepository.create(ortRunId, jobConfigs.analyzer)
            }

            WorkerScheduleInfo.SCANNER -> {
                jobConfigs.scanner?.let { repositories.scannerJobRepository.create(ortRunId, it) }
            }

            WorkerScheduleInfo.EVALUATOR -> {
                jobConfigs.evaluator?.let { repositories.evaluatorJobRepository.create(ortRunId, it) }
            }

            WorkerScheduleInfo.REPORTER -> {
                jobConfigs.reporter?.let { repositories.reporterJobRepository.create(ortRunId, it) }
            }

            WorkerScheduleInfo.NOTIFIER -> {
                jobConfigs.notifier?.let { repositories.notifierJobRepository.create(ortRunId, it) }
            }
        }
    }

    private fun sendMessage(context: WorkerScheduleContext, info: WorkerScheduleInfo, jobId: Long) {
        when (info) {
            WorkerScheduleInfo.ADVISOR -> context.publish(AdvisorEndpoint, AdvisorRequest(jobId))
            WorkerScheduleInfo.ANALYZER -> context.publish(AnalyzerEndpoint, AnalyzerRequest(jobId))
            WorkerScheduleInfo.SCANNER -> context.publish(ScannerEndpoint, ScannerRequest(jobId))
            WorkerScheduleInfo.EVALUATOR -> context.publish(EvaluatorEndpoint, EvaluatorRequest(jobId))
            WorkerScheduleInfo.REPORTER -> context.publish(ReporterEndpoint, ReporterRequest(jobId))
            WorkerScheduleInfo.NOTIFIER -> context.publish(NotifierEndpoint, NotifierRequest(jobId))
        }
    }

    private fun updateJobStatus(info: WorkerScheduleInfo, jobId: Long) {
        val status = JobStatus.SCHEDULED.asPresent()
        when (info) {
            WorkerScheduleInfo.ADVISOR -> repositories.advisorJobRepository.update(jobId, status = status)
            WorkerScheduleInfo.ANALYZER -> repositories.analyzerJobRepository.update(jobId, status = status)
            WorkerScheduleInfo.SCANNER -> repositories.scannerJobRepository.update(jobId, status = status)
            WorkerScheduleInfo.EVALUATOR -> repositories.evaluatorJobRepository.update(jobId, status = status)
            WorkerScheduleInfo.REPORTER -> repositories.reporterJobRepository.update(jobId, status = status)
            WorkerScheduleInfo.NOTIFIER -> repositories.notifierJobRepository.update(jobId, status = status)
        }
    }
}