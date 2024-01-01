/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.infra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class PluginManagerReferenceTest {
    @Test
    public void testGetPluginManager() throws Exception {
        PluginManagerReference reference = PluginManagerReference.reference();
        try {
            reference.getPluginManager();
            fail("should throw exception");
        } catch (IllegalStateException ignored) {
        }
        PluginManager mockManager = mock(PluginManager.class);
        reference.setPluginManager(mockManager);
        assertThat(reference.getPluginManager()).isEqualTo(mockManager);
    }
}
