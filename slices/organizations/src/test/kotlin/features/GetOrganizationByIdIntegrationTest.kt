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

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.eclipse.apoapsis.ortserver.dao.test.DatabaseTestExtension
import org.eclipse.apoapsis.ortserver.utils.test.Integration
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

class GetOrganizationByIdIntegrationTest : WordSpec({
    tags(Integration)

    extension(DatabaseTestExtension())

    "GetOrganizationById" should {
        "return the organization with the specified ID" {
            testApplication {
                val organizationId = transaction {
                    OrganizationsTable.insertAndGetId {
                        it[name] = "Organization Name"
                        it[description] = "An organization description."
                    }.value
                }

                application {
                    install(ContentNegotiation) {
                        serialization(ContentType.Application.Json, Json)
                    }

                    routing {
                        get("/") {
                            call.respondText("Hello, world!")
                        }
                        getOrganizationById()
                    }
                }

                val client = createJsonClient()
                client.get("/") shouldHaveStatus HttpStatusCode.OK

                val response = client.get("organizations/$organizationId")

                response shouldHaveStatus HttpStatusCode.OK
                response.body<Organization>() shouldBe Organization(
                    id = organizationId,
                    name = "Organization Name",
                    description = "An organization description."
                )
            }
        }

        "return 404 if the organization does not exist" {
            
        }

        "return 400 if the ID is not a number" {

        }
    }
})

fun ApplicationTestBuilder.createJsonClient() = createClient {
    install(ClientContentNegotiation) {
        json(Json)
    }
}
