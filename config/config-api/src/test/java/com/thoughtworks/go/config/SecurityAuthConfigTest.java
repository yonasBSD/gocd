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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SecurityAuthConfigTest {
    private AuthorizationMetadataStore store = AuthorizationMetadataStore.instance();

    @AfterEach
    public void tearDown() {
        store.clear();
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithValue() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name"));

        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id");
        authConfig.addConfigurations(List.of(property));

        assertThat(authConfig.size()).isEqualTo(1);
        assertThat(authConfig).contains(new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue("some_name")));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithEncryptedValue() {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name"));

        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id");
        authConfig.addConfigurations(List.of(property));

        assertThat(authConfig.size()).isEqualTo(1);
        assertThat(authConfig).contains(new ConfigurationProperty(new ConfigurationKey("username"), new EncryptedConfigurationValue("some_name")));
    }

    @Test
    public void addConfiguration_shouldEncryptASecureVariable() {
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(List.of(new PluginConfiguration("password", new Metadata(true, true))));
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), profileSettings, null, null, null);

        store.setPluginInfo(pluginInfo);
        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id");
        authConfig.addConfigurations(List.of(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

        assertThat(authConfig.size()).isEqualTo(1);
        assertTrue(authConfig.first().isSecure());
    }

    @Test
    public void addConfiguration_shouldIgnoreEncryptionInAbsenceOfCorrespondingConfigurationInStore() {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), new PluggableInstanceSettings(new ArrayList<>()), null, null, null);

        store.setPluginInfo(pluginInfo);
        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id");
        authConfig.addConfigurations(List.of(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass"))));

        assertThat(authConfig.size()).isEqualTo(1);
        assertFalse(authConfig.first().isSecure());
        assertThat(authConfig).contains(new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));
    }

    @Test
    public void postConstruct_shouldEncryptSecureConfigurations() {
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(List.of(new PluginConfiguration("password", new Metadata(true, true))));
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(pluginDescriptor("plugin_id"), profileSettings, null, null, null);

        store.setPluginInfo(pluginInfo);
        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        authConfig.encryptSecureConfigurations();

        assertThat(authConfig.size()).isEqualTo(1);
        assertTrue(authConfig.first().isSecure());
    }

    @Test
    public void postConstruct_shouldIgnoreEncryptionIfPluginInfoIsNotDefined() {
        SecurityAuthConfig authConfig = new SecurityAuthConfig("id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        authConfig.encryptSecureConfigurations();

        assertThat(authConfig.size()).isEqualTo(1);
        assertFalse(authConfig.first().isSecure());
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }
}
