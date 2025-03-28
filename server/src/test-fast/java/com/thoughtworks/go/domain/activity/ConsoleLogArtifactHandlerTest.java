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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.service.ConsoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ConsoleLogArtifactHandlerTest {

    private ConsoleService consoleService;
    private ConsoleLogArtifactHandler handler;
    private JobInstance completedJobInstance;
    private JobInstance buildingJobInstance;

    @BeforeEach
    public void setUp() {
        consoleService = mock(ConsoleService.class);
        handler = new ConsoleLogArtifactHandler(consoleService);
        completedJobInstance = JobInstanceMother.completed("job");
        buildingJobInstance = JobInstanceMother.building("job");
    }

    @Test
    public void shouldMoveConsoleArtifactWhenJobThatIsRunOrRerunCompletes() {
        completedJobInstance.setOriginalJobId(null);
        handler.jobStatusChanged(completedJobInstance);
        verify(consoleService).moveConsoleArtifacts(completedJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobCompletedIsWasNotActuallyRunWhenAnotherJobInItsStageWasRerun() {
        completedJobInstance.setOriginalJobId(1L);
        handler.jobStatusChanged(completedJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobIsACopyIsNotYetCompleted() {
        completedJobInstance.setOriginalJobId(1L);
        handler.jobStatusChanged(buildingJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

    @Test
    public void shouldNotMoveConsoleArtifactWhenJobIsNotYetCompleted() {
        completedJobInstance.setOriginalJobId(null);
        handler.jobStatusChanged(buildingJobInstance);
        verify(consoleService, never()).moveConsoleArtifacts(buildingJobInstance.getIdentifier());
    }

}