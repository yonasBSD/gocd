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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig environmentConfig;
    private CaseInsensitiveString environmentName;
    private HttpLocalizedOperationResult result;
    private String actionFailed;

    @Mock
    private GoConfigService goConfigService;

    @BeforeEach
    public void setup() {
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        environmentName = new CaseInsensitiveString("Dev");
        environmentConfig = new BasicEnvironmentConfig(environmentName);
        result = new HttpLocalizedOperationResult();
        actionFailed = "Could not add environment " + environmentConfig.name();
    }

    @Test
    public void shouldAddTheSpecifiedEnvironment() {
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentName));
    }

    @Test
    public void shouldValidateInvalidPipelineName() {
        environmentConfig.addPipeline(new CaseInsensitiveString("Invalid-pipeline-name"));
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity("Could not add environment Dev Environment 'Dev' refers to an unknown pipeline 'Invalid-pipeline-name'.");

        assertThat(command.isValid(cruiseConfig)).isFalse();
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void shouldValidateDuplicateEnvironmentVariables() {
        environmentConfig.addEnvironmentVariable("foo", "bar");
        environmentConfig.addEnvironmentVariable("foo", "baz");
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        command.update(cruiseConfig);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();

        assertThat(command.isValid(cruiseConfig)).isFalse();
        expectedResult.unprocessableEntity("Could not add environment Dev Environment Variable name 'foo' is not unique for environment 'Dev'., Environment Variable name 'foo' is not unique for environment 'Dev'.");
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void shouldNotContinueIfEnvironmentWithSameNameAlreadyExists() {
        AddEnvironmentCommand command = new AddEnvironmentCommand(goConfigService, environmentConfig, currentUser, actionFailed, result);
        when(goConfigService.hasEnvironmentNamed(environmentName)).thenReturn(true);
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.conflict(EntityType.Environment.alreadyExists(environmentName));

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result).isEqualTo(expectedResult);
    }
}
