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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CRSvnMaterialTest extends AbstractCRTest<CRSvnMaterial> {

    private final CRSvnMaterial simpleSvn;
    private final CRSvnMaterial simpleSvnAuth;
    private final CRSvnMaterial customSvn;
    private final CRSvnMaterial invalidNoUrl;
    private final CRSvnMaterial invalidPasswordAndEncryptedPasswordSet;

    public CRSvnMaterialTest() {
        simpleSvn = new CRSvnMaterial();
        simpleSvn.setUrl("http://mypublicrepo");

        simpleSvnAuth = new CRSvnMaterial();
        simpleSvnAuth.setUrl("http://myprivaterepo");
        simpleSvnAuth.setUsername("john");
        simpleSvnAuth.setPassword("pa$sw0rd");

        customSvn = new CRSvnMaterial("svnMaterial1", "destDir1", false,
                false, "user1", List.of("tools", "lib"), "http://svn", true);
        customSvn.setPassword("pass1");

        invalidNoUrl = new CRSvnMaterial();
        invalidPasswordAndEncryptedPasswordSet = new CRSvnMaterial();
        invalidPasswordAndEncryptedPasswordSet.setUrl("http://myprivaterepo");
        invalidPasswordAndEncryptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncryptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRSvnMaterial> examples) {
        examples.put("simpleSvn", simpleSvn);
        examples.put("simpleSvnAuth", simpleSvnAuth);
        examples.put("customSvn", customSvn);
    }

    @Override
    public void addBadExamples(Map<String, CRSvnMaterial> examples) {
        examples.put("invalidNoUrl", invalidNoUrl);
        examples.put("invalidPasswordAndEncryptedPasswordSet", invalidPasswordAndEncryptedPasswordSet);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(customSvn);
        assertThat(jsonObject.get("type").getAsString()).isEqualTo(CRSvnMaterial.TYPE_NAME);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = customSvn;
        String json = gson.toJson(value);

        CRSvnMaterial deserializedValue = (CRSvnMaterial) gson.fromJson(json, CRMaterial.class);
        assertThat(deserializedValue).isEqualTo(value);
    }
}
