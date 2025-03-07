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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GoDashboardConfigChangeHandlerTest {
    @Mock
    private GoDashboardService cacheUpdateService;

    private GoDashboardConfigChangeHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GoDashboardConfigChangeHandler(cacheUpdateService);
    }

    @Test
    public void shouldReplaceAllEntriesInCacheWithNewEntriesWhenTheWholeConfigHasChanged() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2");

        handler.call(config);

        verify(cacheUpdateService).updateCacheForAllPipelinesIn(config);
    }

    @Test
    public void shouldReplaceOnlyTheExistingPipelineEntryInCacheWithANewEntryWhenOnlyPipelineConfigHasChanged() {
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        PipelineConfig pipelineConfig = new GoConfigMother().addPipeline(config, "pipeline1", "stage1", "job1");

        handler.call(pipelineConfig);

        verify(cacheUpdateService).updateCacheForPipeline(pipelineConfig);
    }
}
