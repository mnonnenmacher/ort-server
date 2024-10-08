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

/*
 * Disable no-unused-vars for the import of IdTokenClaims,
 * as it has to be imported to be able to extend it.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import type { IdTokenClaims } from 'oidc-client-ts';

declare module 'oidc-client-ts' {
  /* Despite its name, the IdTokenClaims interface is also used as the
   * type for the profile property in the auth.user object returned by
   * the useAuth hook from react-oidc-context, which gets some of its
   * data from the userinfo endpoint, so this declaration is needed
   * even though the client roles are not included in the actual ID token.
   */
  export interface IdTokenClaims {
    // Declare additional claim for resource_access to be able to access the roles of the user.
    resource_access: {
      [client_id: string]: {
        roles: string[];
      };
    };
  }
}
