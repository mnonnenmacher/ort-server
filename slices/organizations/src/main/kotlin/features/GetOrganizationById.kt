/*
 * Copyright (C) 2025 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
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

package org.eclipse.apoapsis.ortserver.organizations.features

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import kotlinx.serialization.Serializable

import io.github.smiley4.ktorswaggerui.dsl.routing.get

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.transactions.transaction

// TODO rename to GetOrganizationById
fun Route.getOrganizationById() = route("organizations/{organizationId}") {
    get({
        operationId = "getOrganizationById"
        summary = "Get the details of an organization by ID."
        tags = listOf("Organizations")

        request {
            pathParameter<Long>("organizationId") {
                description = "The organization's ID."
            }
        }

        response {
            HttpStatusCode.OK to {
                description = "Success"
                body<Organization> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    example("Example") {
                        value = Organization(
                            id = 1,
                            name = "My Organization",
                            description = "This is my organization."
                        )
                    }
                }
            }
        }
    }) {
        // TODO: requirePermission(OrganizationPermission.READ)

        val organizationId = requireNotNull(call.parameters["organizationId"]?.toLongOrNull())

        val organization = transaction {
            OrganizationsTable
                .select(OrganizationsTable.name, OrganizationsTable.description)
                .where { OrganizationsTable.id eq organizationId }
                .firstNotNullOfOrNull {
                    Organization(
                        organizationId,
                        it[OrganizationsTable.name],
                        it[OrganizationsTable.description]
                    )
                }
        }

        if (organization == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, organization)
        }
    }
}

@Serializable
internal data class Organization(val id: Long, val name: String, val description: String?)

internal object OrganizationsTable : LongIdTable("organizations") {
    val name = text("name")
    val description = text("description").nullable()
}
