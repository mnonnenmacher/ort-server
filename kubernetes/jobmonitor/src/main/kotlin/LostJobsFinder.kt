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

package org.eclipse.apoapsis.ortserver.kubernetes.jobmonitor

import io.kubernetes.client.openapi.models.V1Job

import org.eclipse.apoapsis.ortserver.kubernetes.jobmonitor.JobHandler.Companion.ortRunId
import org.eclipse.apoapsis.ortserver.model.WorkerJob
import org.eclipse.apoapsis.ortserver.model.repositories.AdvisorJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.AnalyzerJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.EvaluatorJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.NotifierJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.OrtRunRepository
import org.eclipse.apoapsis.ortserver.model.repositories.ReporterJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.ScannerJobRepository
import org.eclipse.apoapsis.ortserver.model.repositories.WorkerJobRepository
import org.eclipse.apoapsis.ortserver.transport.AdvisorEndpoint
import org.eclipse.apoapsis.ortserver.transport.AnalyzerEndpoint
import org.eclipse.apoapsis.ortserver.transport.ConfigEndpoint
import org.eclipse.apoapsis.ortserver.transport.Endpoint
import org.eclipse.apoapsis.ortserver.transport.EvaluatorEndpoint
import org.eclipse.apoapsis.ortserver.transport.NotifierEndpoint
import org.eclipse.apoapsis.ortserver.transport.ReporterEndpoint
import org.eclipse.apoapsis.ortserver.transport.ScannerEndpoint

import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * A class that periodically checks for jobs that have disappeared in Kubernetes.
 *
 * It cannot always be guaranteed that all failed jobs are detected by the [JobMonitor]. Obviously, there are certain
 * infrastructure conditions that can cause a job to disappear without an event being received by the [JobMonitor].
 * A `kubectl delete job xxx` also has such an effect. Orchestrator would then get no notification about affected jobs,
 * and the associated ORT runs would stay forever in the "running" state.
 *
 * To detect and remedy such scenarios, this class does a periodic sync between the ORT Server database and the job
 * status in Kubernetes. It looks for jobs that should be running according to the database, but do not exist
 * (anymore) in Kubernetes. For such jobs, it sends a notification to the Orchestrator, to give it the chance to act
 * accordingly.
 *
 * Another problem detected by this class is missing schedules, i.e. active ORT runs for which all jobs have been
 * completed. For such runs, a different type of notification is sent to the Orchestrator, so that it can try to resume
 * the run.
 */
@Suppress("LongParameterList")
internal class LostJobsFinder(
    /** The object to query and manipulate jobs. */
    private val jobHandler: JobHandler,

    /** The object to send notifications to the orchestrator. */
    private val notifier: FailedJobNotifier,

    /** The configuration. */
    private val monitorConfig: MonitorConfig,

    /** The repository for Analyzer jobs. */
    analyzerJobRepository: AnalyzerJobRepository,

    /** The repository for Advisor jobs. */
    advisorJobRepository: AdvisorJobRepository,

    /** The repository for Scanner jobs. */
    scannerJobRepository: ScannerJobRepository,

    /** The repository for Evaluator jobs. */
    evaluatorJobRepository: EvaluatorJobRepository,

    /** The repository for Reporter jobs. */
    reporterJobRepository: ReporterJobRepository,

    /** The repository for Notifier jobs. */
    notifierJobRepository: NotifierJobRepository,

    /** The repository for ORT runs. */
    val ortRunRepository: OrtRunRepository,

    /** The object to determine the current time and the age of jobs. */
    private val timeHelper: TimeHelper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(LostJobsFinder::class.java)
    }

    /** A map associating all worker endpoints with their job repository. */
    private val jobRepositories = mapOf(
        AnalyzerEndpoint to analyzerJobRepository,
        AdvisorEndpoint to advisorJobRepository,
        ScannerEndpoint to scannerJobRepository,
        EvaluatorEndpoint to evaluatorJobRepository,
        ReporterEndpoint to reporterJobRepository,
        NotifierEndpoint to notifierJobRepository
    )

    /**
     * Schedule an action to periodically check for lost jobs on the given [scheduler] according to the configuration.
     */
    fun run(scheduler: Scheduler) {
        scheduler.schedule(monitorConfig.lostJobsInterval, this::checkForLostJobs)
    }

    /**
     * For all worker types, check if there are currently active jobs in the database for which no job in Kubernetes
     * exists. For those jobs, send a corresponding notification to the Orchestrator.
     */
    private fun checkForLostJobs() {
        logger.info("Checking for lost jobs and missing schedules.")

        // Keep track of runs for which active jobs exist. These runs are not subject to lost schedules.
        val ortRunsWithJobs = mutableSetOf<Long>()

        jobRepositories.forEach { (endpoint, jobRepository) ->
            val (jobsMap, lostJobs) = gatherLostJobData(endpoint, jobRepository)
            handleLostWorkerJobs(endpoint, lostJobs)

            ortRunsWithJobs += jobsMap.keys.filterNotNull()
            ortRunsWithJobs += lostJobs.map { it.ortRunId }
        }

        checkForLostSchedules(ortRunsWithJobs)
    }

    /**
     * Find information about lost jobs and affected ORT runs for the given [endpoint] using the given [jobRepository].
     *  Return a map with the active ORT runs and their jobs of the endpoint and a list with the lost jobs that were
     *  found.
     */
    private fun gatherLostJobData(
        endpoint: Endpoint<*>,
        jobRepository: WorkerJobRepository<*>
    ): Pair<Map<Long?, V1Job>, List<WorkerJob>> {
        val currentTime = timeHelper.now()

        val kubeJobs = jobHandler.findJobsForWorker(endpoint).associateBy { it.ortRunId }

        logger.debug(
            "Found {} active Kubernetes jobs for {}: {}",
            kubeJobs.size,
            endpoint.configPrefix,
            kubeJobs.values.map { it.metadata?.name }
        )

        val lostJobs = jobRepository.listActive(currentTime - monitorConfig.lostJobsMinAge)
            .filterNot { it.ortRunId in kubeJobs }

        return Pair(kubeJobs, lostJobs)
    }

    /**
     * Perform actions to handle the given list of [lostJobs] for the given [endpoint].
     */
    private fun handleLostWorkerJobs(
        endpoint: Endpoint<*>,
        lostJobs: List<WorkerJob>
    ) {
        if (lostJobs.isNotEmpty()) {
            logger.warn("Found ${lostJobs.size} lost jobs for ${endpoint.configPrefix}.")
            logger.debug("Lost jobs: {}", lostJobs)

            lostJobs.forEach {
                val ortRun = ortRunRepository.get(it.ortRunId)

                MDC.put("traceId", ortRun?.traceId ?: "unknown")
                MDC.put("ortRunId", it.ortRunId.toString())

                notifier.sendLostJobNotification(it.ortRunId, endpoint)
            }
        }
    }

    /**
     * Check for active ORT runs for which no jobs exist in Kubernetes. Determine affected runs based on the given
     * set with [ortRunsWithJobs]. Notify the Orchestrator about those runs.
     */
    private fun checkForLostSchedules(ortRunsWithJobs: Set<Long>) {
        val referenceTime = timeHelper.now() - monitorConfig.lostJobsMinAge

        // The ORT runs with job take only workers into account for which a database representation exists.
        // This is not the case for the Config worker, so those need to be added manually.
        val ortRunsWithConfigJobs = jobHandler.findJobsForWorker(ConfigEndpoint)
            .mapNotNullTo(mutableSetOf()) { it.ortRunId }
        ortRunsWithConfigJobs += ortRunsWithJobs

        val runsWithMissingSchedules = ortRunRepository.listActiveRuns()
            .filter { it.createdAt <= referenceTime && it.runId !in ortRunsWithConfigJobs }

        runsWithMissingSchedules.forEach { ortRun ->
            logger.warn("Found ORT run {} with missing schedules.", ortRun)
            notifier.sendLostScheduleNotification(ortRun)
        }
    }
}
