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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class PluginSettingsJsonMessageHandlerTestBase {
    protected PluginSettingsJsonMessageHandlerBase messageHandler;

    @BeforeEach
    public void setUp() {
        messageHandler = messageHandler();
    }

    protected abstract PluginSettingsJsonMessageHandlerBase messageHandler();


    @Test
    public void shouldBuildPluginSettingsConfigurationFromResponseBody() {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";
        PluginSettingsConfiguration configuration = messageHandler.responseMessageForPluginSettingsConfiguration(responseBody);

        assertPropertyConfiguration((PluginSettingsProperty) configuration.get("key-one"), "key-one", "", true, false, "key-one", 0);
        assertPropertyConfiguration((PluginSettingsProperty) configuration.get("key-two"), "key-two", "two", true, true, "display-two", 1);
        assertPropertyConfiguration((PluginSettingsProperty) configuration.get("key-three"), "key-three", "three", false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildPluginSettingsViewFromResponse() {
        String jsonResponse = "{\"template\":\"<html>junk</html>\"}";

        String view = messageHandler.responseMessageForPluginSettingsView(jsonResponse);

        assertThat(view).isEqualTo("<html>junk</html>");
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForPluginSettingsConfiguration() {
        assertThat(errorMessageForPluginSettingsConfiguration("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForPluginSettingsConfiguration(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForPluginSettingsConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]")).isEqualTo("Unable to de-serialize json response. Plugin Settings Configuration should be returned as a map");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. Plugin Settings Configuration key cannot be empty");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":[{}]}")).isEqualTo("Unable to de-serialize json response. Plugin Settings Configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"secure\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"secure\":100}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"secure\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"required\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"required\":100}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"required\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"display-name\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"display-name\":100}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"display-order\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"display-order\":10.0}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForPluginSettingsConfiguration("{\"key\":{\"display-order\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForPluginSettingsView() {
        assertThat(errorMessageForPluginSettingsView("{\"template\":null}")).isEqualTo("Unable to de-serialize json response. Error: Plugin Settings View's 'template' is a required field.");
        assertThat(errorMessageForPluginSettingsView("{\"template\":true}")).isEqualTo("Unable to de-serialize json response. Error: Plugin Settings View's 'template' should be of type string.");
    }

    @Test
    public void shouldBuildRequestBodyForCheckSCMConfigurationValidRequest() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("key-one", "value-one"));
        configuration.add(new PluginSettingsProperty("key-two", "value-two"));

        String requestMessage = messageHandler.requestMessageForPluginSettingsValidation(configuration);

        assertThat(requestMessage).isEqualTo("{\"plugin-settings\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    public void shouldBuildValidationResultFromCheckSCMConfigurationValidResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForPluginSettingsValidation(responseBody);

        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    private void assertPropertyConfiguration(PluginSettingsProperty property, String key, String value, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey()).isEqualTo(key);
        assertThat(property.getValue()).isEqualTo(value);
        assertThat(property.getOption(Property.REQUIRED)).isEqualTo(required);
        assertThat(property.getOption(Property.SECURE)).isEqualTo(secure);
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo(displayName);
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(displayOrder);
    }

    private String errorMessageForPluginSettingsConfiguration(String message) {
        try {
            messageHandler.responseMessageForPluginSettingsConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForPluginSettingsView(String message) {
        try {
            messageHandler.responseMessageForPluginSettingsView(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private void assertValidationError(ValidationError validationError, String expectedKey, String expectedMessage) {
        assertThat(validationError.getKey()).isEqualTo(expectedKey);
        assertThat(validationError.getMessage()).isEqualTo(expectedMessage);
    }
}
