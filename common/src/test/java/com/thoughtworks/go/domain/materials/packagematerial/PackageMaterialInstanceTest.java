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
package com.thoughtworks.go.domain.materials.packagematerial;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageMaterialInstanceTest {

    @Test
    public void shouldConvertMaterialInstanceToMaterial() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        PackageDefinition packageDefinition = material.getPackageDefinition();
        PackageMaterialInstance materialInstance = new PackageMaterialInstance(JsonHelper.toJsonString(material), "flyweight");
        materialInstance.setId(1L);

        PackageMaterial constructedMaterial = (PackageMaterial) materialInstance.toOldMaterial(null, null, null);

        assertThat(constructedMaterial.getPackageDefinition().getConfiguration()).isEqualTo(packageDefinition.getConfiguration());
        assertThat(constructedMaterial.getPackageDefinition().getRepository().getPluginConfiguration().getId()).isEqualTo(packageDefinition.getRepository().getPluginConfiguration().getId());
        assertThat(constructedMaterial.getPackageDefinition().getRepository().getConfiguration()).isEqualTo(packageDefinition.getRepository().getConfiguration());
        assertThat(constructedMaterial.getId()).isEqualTo(1L);
    }

    @Test
    public void shouldTestEqualsBasedOnConfiguration() {
        PackageMaterial material = MaterialsMother.packageMaterial("repo-id", "repo-name", "pkg-id", "pkg-name", ConfigurationPropertyMother.create("key1", false, "value1"));
        MaterialInstance materialInstance = material.createMaterialInstance();
        MaterialInstance materialInstanceCopy = material.createMaterialInstance();

        material.getPackageDefinition().getConfiguration().add(ConfigurationPropertyMother.create("key2", false, "value2"));
        MaterialInstance newMaterialInstance = material.createMaterialInstance();

        assertThat(materialInstance).isEqualTo(materialInstanceCopy);
        assertThat(materialInstance).isNotEqualTo(newMaterialInstance);
    }

    @Test
    public void shouldCorrectlyCheckIfUpgradeIsNecessary() {
        PackageMaterial material = MaterialsMother.packageMaterial("repo-id", "repo-name", "pkg-id", "pkg-name", ConfigurationPropertyMother.create("key1", false, "value1"));
        PackageMaterialInstance materialInstance = (PackageMaterialInstance) material.createMaterialInstance();
        materialInstance.setId(10L);
        PackageMaterialInstance materialInstanceCopy = (PackageMaterialInstance) material.createMaterialInstance();

        material.getPackageDefinition().getConfiguration().add(ConfigurationPropertyMother.create("key2", false, "value2"));
        PackageMaterialInstance newMaterialInstance = (PackageMaterialInstance) material.createMaterialInstance();

        assertThat(materialInstance.shouldUpgradeTo(materialInstanceCopy)).isFalse();
        assertThat(materialInstance.shouldUpgradeTo(newMaterialInstance)).isTrue();
    }

    @Test
    public void shouldCorrectlyCopyConfigurationValue() {
        PackageMaterialInstance packageMaterialInstance = (PackageMaterialInstance) MaterialsMother.packageMaterial().createMaterialInstance();
        packageMaterialInstance.setId(10L);
        PackageMaterial latestMaterial = MaterialsMother.packageMaterial("repo-id", "name", "pkId", "name", ConfigurationPropertyMother.create("key1", false, "value1"));
        PackageMaterialInstance newPackageMaterialInstance = (PackageMaterialInstance) latestMaterial.createMaterialInstance();
        packageMaterialInstance.upgradeTo(newPackageMaterialInstance);
        assertThat(packageMaterialInstance.getId()).isEqualTo(10L);
        assertThat(packageMaterialInstance.getConfiguration()).isEqualTo(newPackageMaterialInstance.getConfiguration());
    }

    @Test
    public void shouldSetFingerprintWhenConvertingMaterialInstanceToMaterial() {
        String fingerprint = "fingerprint";
        PackageMaterial material = MaterialsMother.packageMaterial();
        PackageMaterialInstance materialInstance = new PackageMaterialInstance(JsonHelper.toJsonString(material), "flyweight");
        ReflectionUtil.setField(materialInstance, "fingerprint", fingerprint);
        materialInstance.setId(1L);

        PackageMaterial constructedMaterial = (PackageMaterial) materialInstance.toOldMaterial(null, null, null);

        assertThat(constructedMaterial.getFingerprint()).isEqualTo(fingerprint);
    }


}
