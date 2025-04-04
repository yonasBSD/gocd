/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.access.authorization.v2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationResponseDTOTest {

    @Test
    public void shouldAbleToDeserializeJSON() {

        String json = """
                {
                  "user": {
                      "username":"gocd",
                      "display_name": "GoCD Admin",
                      "email": "gocd@go.cd"
                  },
                  "roles": ["admin","blackbird"]
                }""";

        com.thoughtworks.go.plugin.access.authorization.v2.AuthenticationResponseDTO authenticationResponse = com.thoughtworks.go.plugin.access.authorization.v2.AuthenticationResponseDTO.fromJSON(json);

        assertThat(authenticationResponse.getUser()).isEqualTo(new UserDTO("gocd", "GoCD Admin", "gocd@go.cd"));
        assertThat(authenticationResponse.getRoles()).hasSize(2);
        assertThat(authenticationResponse.getRoles()).contains("admin", "blackbird");
    }
}
