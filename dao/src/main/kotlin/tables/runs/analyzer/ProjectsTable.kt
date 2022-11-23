/*
 * Copyright (C) 2022 The ORT Project Authors (See <https://github.com/oss-review-toolkit/ort-server/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.server.dao.tables.runs.analyzer

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

import org.ossreviewtoolkit.server.dao.tables.runs.shared.IdentifierDao
import org.ossreviewtoolkit.server.dao.tables.runs.shared.IdentifiersTable
import org.ossreviewtoolkit.server.dao.tables.runs.shared.LicenseStringDao
import org.ossreviewtoolkit.server.dao.tables.runs.shared.VcsInfoDao
import org.ossreviewtoolkit.server.dao.tables.runs.shared.VcsInfoTable
import org.ossreviewtoolkit.server.model.runs.Project

/**
 * A table to represent a software package as a project.
 */
object ProjectsTable : LongIdTable("projects") {
    val identifierId = reference("identifier_id", IdentifiersTable.id, ReferenceOption.CASCADE)
    val vcsId = reference("vcs_id", VcsInfoTable.id, ReferenceOption.CASCADE)
    val vcsProcessedId = reference("vcs_processed_id", VcsInfoTable.id, ReferenceOption.CASCADE)
    val analyzerRunId = reference("analyzer_run_id", AnalyzerRunsTable.id, ReferenceOption.CASCADE)

    val cpe = text("cpe").nullable()
    val homepageUrl = text("homepage_url")
    val definitionFilePath = text("definition_file_path")
}

class ProjectDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ProjectDao>(ProjectsTable)

    var identifier by IdentifierDao referencedOn ProjectsTable.identifierId
    var vcs by VcsInfoDao referencedOn ProjectsTable.vcsId
    var vcsProcessed by VcsInfoDao referencedOn ProjectsTable.vcsProcessedId
    var analyzerRun by AnalyzerRunDao referencedOn ProjectsTable.analyzerRunId
    var authors by AuthorDao via ProjectsAuthorsTable
    var declaredLicenses by LicenseStringDao via ProjectsDeclaredLicensesTable
    val scopeNames by ProjectScopeDao referrersOn ProjectScopesTable.projectId

    var cpe by ProjectsTable.cpe
    var homepageUrl by ProjectsTable.homepageUrl
    var definitionFilePath by ProjectsTable.definitionFilePath

    fun mapToModel() = Project(
        id.value,
        identifier.mapToModel(),
        cpe,
        definitionFilePath,
        authors.map(AuthorDao::mapToModel).toSet(),
        declaredLicenses.map(LicenseStringDao::mapToModel).toSet(),
        vcs.mapToModel(),
        vcsProcessed.mapToModel(),
        homepageUrl,
        scopeNames.map(ProjectScopeDao::name).toSet()
    )
}
