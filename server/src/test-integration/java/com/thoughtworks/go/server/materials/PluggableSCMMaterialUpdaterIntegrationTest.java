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
package com.thoughtworks.go.server.materials;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.materials.MaterialPoller;
import com.thoughtworks.go.server.service.materials.PluggableSCMMaterialPoller;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(ClearSingleton.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PluggableSCMMaterialUpdaterIntegrationTest {
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private LegacyMaterialChecker materialChecker;
    @Autowired
    private MaterialService materialService;

    private SCMExtension scmExtension;
    private SubprocessExecutionContext subprocessExecutionContext;
    private ScmMaterialUpdater scmMaterialUpdater;
    private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        scmExtension = mock(SCMExtension.class);
        subprocessExecutionContext = mock(SubprocessExecutionContext.class);
        scmMaterialUpdater = mock(ScmMaterialUpdater.class);
        pluggableSCMMaterialUpdater = new PluggableSCMMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldUpdateMaterialInstanceWhenPluginIsUpgraded() {
        final PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        final MaterialInstance materialInstance = material.createMaterialInstance();
        materialRepository.saveOrUpdate(materialInstance);

        addMetadata(material, "fieldX", false);
        material.getScmConfig().getConfiguration().addNewConfiguration("fieldX", true);
        final List<Modification> modifications = ModificationsMother.multipleModificationList();
        doNothing().when(scmMaterialUpdater).insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications(modifications));
        transactionTemplate.execute(transactionStatus -> {
            pluggableSCMMaterialUpdater.insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications(modifications));
            return null;
        });

        MaterialInstance actualInstance = materialRepository.findMaterialInstance(material);
        assertThat(actualInstance.getConfiguration()).isEqualTo(material.createMaterialInstance().getConfiguration());
    }

    @Test
    public void shouldUpdateMaterialInstanceWhenAdditionalDataIsUpdatedDuringLatestModification() {
        final PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        final MaterialInstance materialInstance = material.createMaterialInstance();
        materialRepository.saveOrUpdate(materialInstance);

        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        when(scmExtension.getLatestRevision(any(String.class), any(SCMPropertyConfiguration.class), any(), any(String.class))).thenReturn(new MaterialPollResult(data, new SCMRevision()));
        mockSCMExtensionInPoller();
        scmMaterialUpdater = new ScmMaterialUpdater(materialRepository, materialChecker, subprocessExecutionContext, materialService);
        pluggableSCMMaterialUpdater = new PluggableSCMMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);

        transactionTemplate.execute(transactionStatus -> {
            pluggableSCMMaterialUpdater.insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications());
            return null;
        });

        MaterialInstance actualInstance = materialRepository.findMaterialInstance(material);
        assertThat(actualInstance.getAdditionalDataMap()).isEqualTo(data);
    }

    @Test
    public void shouldUpdateMaterialInstanceWhenAdditionalDataIsUpdatedDuringLatestModificationsSince() {
        final PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        final MaterialInstance materialInstance = material.createMaterialInstance();
        Map<String, String> oldData = new HashMap<>();
        oldData.put("k1", "v1");
        materialInstance.setAdditionalData(new GsonBuilder().create().toJson(oldData));
        materialRepository.saveOrUpdate(materialInstance);

        Map<String, String> newData = new HashMap<>(oldData);
        newData.put("k2", "v2");
        when(scmExtension.latestModificationSince(any(String.class), any(SCMPropertyConfiguration.class), any(), any(String.class), any(SCMRevision.class))).thenReturn(new MaterialPollResult(newData, new SCMRevision()));
        mockSCMExtensionInPoller();
        scmMaterialUpdater = new ScmMaterialUpdater(materialRepository, materialChecker, subprocessExecutionContext, materialService);
        pluggableSCMMaterialUpdater = new PluggableSCMMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);

        transactionTemplate.execute(transactionStatus -> {
            pluggableSCMMaterialUpdater.insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications(new Modification()));
            return null;
        });

        MaterialInstance actualInstance = materialRepository.findMaterialInstance(material);
        assertThat(actualInstance.getAdditionalDataMap()).isEqualTo(newData);
    }

    @SuppressWarnings("SameParameterValue")
    private void addMetadata(PluggableSCMMaterial material, String field, boolean partOfIdentity) {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration(field).with(SCMConfiguration.PART_OF_IDENTITY, partOfIdentity));
        SCMMetadataStore.getInstance().addMetadataFor(material.getPluginId(), scmConfigurations, null);
    }

    private void mockSCMExtensionInPoller() {
        Map<Class<?>, MaterialPoller<?>> materialPollerMap = ReflectionUtil.getField(materialService, "materialPollerMap");
        materialPollerMap.put(PluggableSCMMaterial.class, new PluggableSCMMaterialPoller(materialRepository, scmExtension, transactionTemplate));
        ReflectionUtil.setField(materialService, "materialPollerMap", materialPollerMap);
    }
}
