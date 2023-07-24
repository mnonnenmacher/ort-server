/*
 * Copyright (C) 2023 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.server.model

import kotlinx.serialization.Serializable

import org.ossreviewtoolkit.model.config.Options
import org.ossreviewtoolkit.server.model.runs.PackageManagerConfiguration

/**
 * The configurations for the jobs in an [OrtRun].
 */
@Serializable
data class JobConfigurations(
    val analyzer: AnalyzerJobConfiguration = AnalyzerJobConfiguration(),
    val advisor: AdvisorJobConfiguration? = null,
    val scanner: ScannerJobConfiguration? = null,
    val evaluator: EvaluatorJobConfiguration? = null,
    val reporter: ReporterJobConfiguration? = null
)

/**
 * The configuration for an analyzer job.
 */
@Serializable
data class AnalyzerJobConfiguration(
    /**
     * Enable the analysis of projects that use version ranges to declare their dependencies. If set to true,
     * dependencies of exactly the same project might change with another scan done at a later time if any of the
     * (transitive) dependencies are declared using version ranges and a new version of such a dependency was
     * published in the meantime. If set to false, analysis of projects that use version ranges will fail. Defaults to
     * false.
     */
    val allowDynamicVersions: Boolean = false,

    /**
     * A list of the case-insensitive names of package managers that are enabled. Disabling a package manager in
     * [disabledPackageManagers] overrides enabling it here.
     */
    val disabledPackageManagers: List<String>? = null,

    /**
     * A list of the case-insensitive names of package managers that are disabled. Disabling a package manager in this
     * list overrides [enabledPackageManagers].
     */
    val enabledPackageManagers: List<String>? = null,

    /** The explicit environment configuration to be used for this run. */
    val environmentConfig: EnvironmentConfig? = null,

    /**
     * Package manager specific configurations. The key needs to match the name of the package manager class, e.g.
     * "NuGet" for the NuGet package manager.
     */
    val packageManagerOptions: Map<String, PackageManagerConfiguration>? = null,

    /**
     * A flag to control whether excluded scopes and paths should be skipped during the analysis.
     */
    val skipExcluded: Boolean? = null,

    /**
     * Additional parameters of the job.
     */
    val parameters: Parameters? = null
)

/**
 * The configuration for an advisor job.
 */
@Serializable
data class AdvisorJobConfiguration(
    /**
     * The Advisors to use (e.g. NexusIQ, VulnerableCode, DefectDB).
     */
    val advisors: List<String> = emptyList(),

    /**
     * High-level parameters of the advisor job.
     */
    val parameters: Parameters? = null,

    /**
     * A map of configuration options that are specific to a concrete advisor.
     */
    val options: Map<String, Options>? = null
)

/**
 * The configuration for a scanner job.
 */
@Serializable
data class ScannerJobConfiguration(
    /**
     * Do not scan excluded projects or packages.
     */
    val skipExcluded: Boolean = false
)

/**
 * The configuration for an evaluator job.
 */
@Serializable
data class EvaluatorJobConfiguration(
    /**
     * The id of the rule set to use for the evaluation.
     */
    val ruleSet: String? = null
)

@Serializable
data class ReporterJobConfiguration(
    /**
     * The report formats to generate.
     */
    val formats: List<String> = emptyList()
)

/**
 * A type for storing key-value pairs of job configuration parameters. These parameters are subject for validation
 * performed by a validation script, which can then map them to [Options].
 */
typealias Parameters = Map<String, String>
