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
package com.thoughtworks.go.domain.packagerepository;


import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.plugin.api.config.Property.PART_OF_IDENTITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PackageRepositoriesTest {

    @Test
    void shouldCheckEqualityOfPackageRepositories() {
        PackageRepository packageRepository = new PackageRepository();
        PackageRepositories packageRepositories = new PackageRepositories(packageRepository);
        assertThat(packageRepositories).isEqualTo(new PackageRepositories(packageRepository));
    }

    @Test
    void shouldFindRepositoryGivenTheRepoId() {
        PackageRepository packageRepository1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageRepository packageRepository2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);

        PackageRepositories packageRepositories = new PackageRepositories(packageRepository1, packageRepository2);
        assertThat(packageRepositories.find("repo-id2")).isEqualTo(packageRepository2);
    }

    @Test
    void shouldReturnNullIfNoMatchingRepoFound() {
        PackageRepositories packageRepositories = new PackageRepositories();
        assertThat(packageRepositories.find("not-found")).isNull();
    }

    @Test
    void shouldGetPackageRepositoryForGivenPackageId() {
        PackageRepository repo1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionOne = PackageDefinitionMother.create("pid1", repo1);
        PackageDefinition packageDefinitionTwo = PackageDefinitionMother.create("pid2", repo1);
        repo1.getPackages().addAll(List.of(packageDefinitionOne, packageDefinitionTwo));

        PackageRepository repo2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionThree = PackageDefinitionMother.create("pid3", repo2);
        PackageDefinition packageDefinitionFour = PackageDefinitionMother.create("pid4", repo2);
        repo2.getPackages().addAll(List.of(packageDefinitionThree, packageDefinitionFour));


        PackageRepositories packageRepositories = new PackageRepositories(repo1, repo2);

        assertThat(packageRepositories.findPackageRepositoryHaving("pid3")).isEqualTo(repo2);
        assertThat(packageRepositories.findPackageRepositoryWithPackageIdOrBomb("pid3")).isEqualTo(repo2);
    }

    @Test
    void shouldReturnNullWhenRepositoryForGivenPackageNotFound() {
        PackageRepositories packageRepositories = new PackageRepositories();
        assertThat(packageRepositories.findPackageRepositoryHaving("invalid")).isNull();
    }

    @Test
    void shouldThrowExceptionWhenRepositoryForGivenPackageNotFound() {
        PackageRepositories packageRepositories = new PackageRepositories();

        try {
            packageRepositories.findPackageRepositoryWithPackageIdOrBomb("invalid");
            fail("should have thrown exception for not finding package repository");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Could not find repository for given package id:[invalid]");
        }
    }

    @Test
    void shouldFindPackageRepositoryById() {
        PackageRepositories packageRepositories = new PackageRepositories();
        packageRepositories.add(PackageRepositoryMother.create("repo1"));
        PackageRepository repo2 = PackageRepositoryMother.create("repo2");
        packageRepositories.add(repo2);
        packageRepositories.removePackageRepository("repo1");

        assertThat(packageRepositories).containsExactly(repo2);
    }

    @Test
    void shouldReturnNullExceptionWhenRepoIdIsNotFound() {
        PackageRepositories packageRepositories = new PackageRepositories();
        try {
            packageRepositories.removePackageRepository("repo1");
            fail("This should have thrown an exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Could not find repository with id '%s'", "repo1"));
        }
    }

    @Test
    void shouldValidateForCaseInsensitiveNameAndIdUniqueness() {
        PackageRepository repo1 = PackageRepositoryMother.create("repo1");
        PackageRepository duplicate = PackageRepositoryMother.create("REPO1");
        PackageRepository unique = PackageRepositoryMother.create("unique");
        PackageRepositories packageRepositories = new PackageRepositories();
        packageRepositories.add(repo1);
        packageRepositories.add(duplicate);
        packageRepositories.add(unique);

        packageRepositories.validate(null);
        assertThat(repo1.errors().isEmpty()).isFalse();
        String nameError = String.format("You have defined multiple repositories called '%s'. Repository names are case-insensitive and must be unique.", duplicate.getName());
        assertThat(repo1.errors().getAllOn(PackageRepository.NAME).contains(nameError)).isTrue();
        assertThat(duplicate.errors().isEmpty()).isFalse();
        assertThat(duplicate.errors().getAllOn(PackageRepository.NAME).contains(nameError)).isTrue();
        assertThat(unique.errors().isEmpty()).isTrue();
    }

    @Test
    void shouldFailValidationIfMaterialWithDuplicateFingerprintIsFound() {

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        packageConfiguration.add(new PackageMaterialProperty("k1"));
        packageConfiguration.add(new PackageMaterialProperty("k2").with(PART_OF_IDENTITY, false));

        PackageMetadataStore.getInstance().addMetadataFor("plugin", new PackageConfigurations(packageConfiguration));

        String expectedErrorMessage = "Cannot save package or repo, found duplicate packages. [Repo Name: 'repo-repo1', Package Name: 'pkg1'], [Repo Name: 'repo-repo1', Package Name: 'pkg3'], [Repo Name: 'repo-repo1', Package Name: 'pkg5']";
        PackageRepository repository = PackageRepositoryMother.create("repo1");
        PackageDefinition definition1 = PackageDefinitionMother.create("1", "pkg1", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);
        PackageDefinition definition2 = PackageDefinitionMother.create("2", "pkg2", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v2"))), repository);
        PackageDefinition definition3 = PackageDefinitionMother.create("3", "pkg3", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);
        PackageDefinition definition4 = PackageDefinitionMother.create("4", "pkg4", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("V1"))), repository);
        PackageDefinition definition5 = PackageDefinitionMother.create("5", "pkg5", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1")), new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("v2"))), repository);

        repository.setPackages(new Packages(definition1, definition2, definition3, definition4, definition5));

        PackageRepositories packageRepositories = new PackageRepositories(repository);

        packageRepositories.validate(null);

        assertThat(definition1.errors().getAllOn(PackageDefinition.ID)).isEqualTo(List.of(expectedErrorMessage));
        assertThat(definition3.errors().getAllOn(PackageDefinition.ID)).isEqualTo(List.of(expectedErrorMessage));
        assertThat(definition3.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER))).isTrue();
        assertThat(definition5.errors().getAllOn(PackageDefinition.ID)).isEqualTo(List.of(expectedErrorMessage));
        assertThat(definition5.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER))).isTrue();

        assertThat(definition2.errors().getAllOn(PackageDefinition.ID)).isEmpty();
        assertThat(definition2.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER))).isFalse();
        assertThat(definition4.errors().getAllOn(PackageDefinition.ID)).isEmpty();
        assertThat(definition4.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER))).isFalse();
    }

    @Test
    void shouldGetPackageDefinitionForGivenPackageId() {
        PackageRepository repo1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionOne = PackageDefinitionMother.create("pid1", repo1);
        PackageDefinition packageDefinitionTwo = PackageDefinitionMother.create("pid2", repo1);
        repo1.getPackages().addAll(List.of(packageDefinitionOne, packageDefinitionTwo));

        PackageRepository repo2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionThree = PackageDefinitionMother.create("pid3", repo2);
        PackageDefinition packageDefinitionFour = PackageDefinitionMother.create("pid4", repo2);
        repo2.getPackages().addAll(List.of(packageDefinitionThree, packageDefinitionFour));


        PackageRepositories packageRepositories = new PackageRepositories(repo1, repo2);
        assertThat(packageRepositories.findPackageDefinitionWith("pid3")).isEqualTo(packageDefinitionThree);
        assertThat(packageRepositories.findPackageDefinitionWith("pid5")).isNull();
    }

    @BeforeEach
    void setup() {
        RepositoryMetadataStoreHelper.clear();
    }

    @AfterEach
    void tearDown() {
        RepositoryMetadataStoreHelper.clear();
    }
}
