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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.CRBuiltInArtifact;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluggableArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ArtifactTypeAdapterTest {

    private ArtifactTypeAdapter artifactTypeAdapter;

    @Mock
    private JsonDeserializationContext jsonDeserializationContext;

    @Mock
    private Type type;

    @BeforeEach
    public void setUp() {
        artifactTypeAdapter = new ArtifactTypeAdapter();
    }

    @Test
    public void shouldInstantiateATaskOfTypeExec() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "build");
        artifactTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        verify(jsonDeserializationContext).deserialize(jsonObject, CRBuiltInArtifact.class);
    }

    @Test
    public void shouldInstantiateATaskOfTypeAnt() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "test");
        artifactTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        verify(jsonDeserializationContext).deserialize(jsonObject, CRBuiltInArtifact.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeNant() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "external");
        artifactTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        verify(jsonDeserializationContext).deserialize(jsonObject, CRPluggableArtifact.class);
    }
}
