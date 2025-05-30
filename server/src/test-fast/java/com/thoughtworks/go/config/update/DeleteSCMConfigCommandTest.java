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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.materials.PluggableScmService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteSCMConfigCommandTest {

    @Mock
    private PluggableScmService pluggableScmService;

    @Mock
    private GoConfigService goConfigService;

    private HttpLocalizedOperationResult result;
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private SCM scmConfig;

    @BeforeEach
    public void setup() {
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
        scmConfig = new SCM("id", "name");
        result = new HttpLocalizedOperationResult();
    }


    @Test
    public void shouldDeleteTemplateFromTheGivenConfig() {
        cruiseConfig.getSCMs().add(scmConfig);
        DeleteSCMConfigCommand command = new DeleteSCMConfigCommand(scmConfig, pluggableScmService, result, currentUser, goConfigService);
        assertThat(cruiseConfig.getSCMs().contains(scmConfig)).isTrue();
        command.update(cruiseConfig);
        assertThat(cruiseConfig.getSCMs().contains(scmConfig)).isFalse();
    }

    @Test
    public void shouldValidateWhetherSCMIsAssociatedWithPipelines() {
        PipelineConfig pipelineConfig = new GoConfigMother().addPipeline(cruiseConfig, "p1", "s1", "j1");
        pipelineConfig.addMaterialConfig(new PluggableSCMMaterialConfig(scmConfig.getSCMId()));
        DeleteSCMConfigCommand command = new DeleteSCMConfigCommand(scmConfig, pluggableScmService, result, currentUser, goConfigService);

        assertThatThrownBy(() -> command.isValid(cruiseConfig))
                .hasMessageContaining("The scm 'name' is being referenced by pipeline(s): [p1]");
    }

    @Test
    public void shouldThrowAnExceptionIfSCMIsNotFound() {
        SCM scm = new SCM("non-existent-id", new PluginConfiguration("non-existent-plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"))));
        DeleteSCMConfigCommand command = new DeleteSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);
        assertThatThrownBy(() -> command.update(cruiseConfig))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("The pluggable scm material with id 'non-existent-id' is not found.");
    }

    @Test
    public void shouldNotContinueWithConfigSaveIfUserIsUnauthorized() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(false);
        when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM scm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"))));
        DeleteSCMConfigCommand command = new DeleteSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);

        assertThat(command.canContinue(cruiseConfig)).isFalse();
        assertThat(result.message()).isEqualTo(EntityType.SCM.forbiddenToEdit(scm.getId(), currentUser.getUsername()));
    }

    @Test
    public void shouldContinueWithConfigSaveIfUserIsAdmin() {
        when(goConfigService.isUserAdmin(currentUser)).thenReturn(true);
        lenient().when(goConfigService.isGroupAdministrator(currentUser.getUsername())).thenReturn(false);

        SCM scm = new SCM("id", new PluginConfiguration("plugin-id", "1"), new Configuration(new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"))));
        DeleteSCMConfigCommand command = new DeleteSCMConfigCommand(scm, pluggableScmService, result, currentUser, goConfigService);

        assertThat(command.canContinue(cruiseConfig)).isTrue();
    }
}
