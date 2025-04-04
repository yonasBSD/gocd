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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CruiseConfigClassTest {

    private ConfigCache configCache = new ConfigCache();

    @Test
    public void shouldFindAllFields() {
        GoConfigClassWriter fooBarClass = new GoConfigClassWriter(FooBar.class, configCache, null);
        assertThat(fooBarClass.getAllFields(new FooBar()).size()).isEqualTo(3);
    }

    @Test
    public void shouldFindAllFieldsInBaseClass() {
        GoConfigClassWriter fooBarClass = new GoConfigClassWriter(DerivedFooBar.class, configCache, null);
        assertThat(fooBarClass.getAllFields(new DerivedFooBar()).size()).isEqualTo(4);
    }

}

class FooBar {
    @SuppressWarnings("unused") private String value;
    @SuppressWarnings("unused") private String data;
    @SuppressWarnings("unused") protected String moreData;
}

class DerivedFooBar extends FooBar {
    @SuppressWarnings("unused") protected String derivedClassData;
}
