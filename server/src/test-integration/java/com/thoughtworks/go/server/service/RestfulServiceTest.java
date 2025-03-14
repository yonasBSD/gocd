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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class RestfulServiceTest {
    @Autowired private RestfulService restfulService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao cruiseConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private StageDao stageDao;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private InstanceFactory instanceFactory;

    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper ;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(cruiseConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        fixture.usingThreeJobs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        fixture.onTearDown();
    }

    @Test
    public void shouldShouldTranslateLatestPipelineLabel() {
        fixture.createdPipelineWithAllStagesPassed();
        Pipeline latestPipeline = fixture.createdPipelineWithAllStagesPassed();
        final JobIdentifier jobIdentifier1 = new JobIdentifier(latestPipeline.getName(), 1, JobIdentifier.LATEST, fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        JobIdentifier jobIdentifier = restfulService.findJob(jobIdentifier1.getPipelineName(), jobIdentifier1.getPipelineLabel(), jobIdentifier1.getStageName(), jobIdentifier1.getStageCounter(), jobIdentifier1.getBuildName());
        assertThat(jobIdentifier.getPipelineLabel()).isEqualTo(latestPipeline.getLabel());
    }

    @Test
    public void shouldTranslateLatestToRealPipelineLabel() {
        fixture.createdPipelineWithAllStagesPassed();
        Pipeline latestPipeline = fixture.createdPipelineWithAllStagesPassed();
        JobIdentifier jobIdentifier = restfulService.findJob(latestPipeline.getName(), JobIdentifier.LATEST, fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(jobIdentifier.getPipelineLabel()).isEqualTo(latestPipeline.getLabel());
    }


    @Test
    public void shouldTranslateLatestToRealStageCounter() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getCounter().toString(), fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(Integer.valueOf(jobIdentifier.getStageCounter())).isEqualTo(pipeline.getStages().byName(fixture.devStage).getCounter());
    }

    @Test
    public void shouldTranslateEmtpyToLatestStageCounter() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getCounter().toString(), fixture.devStage, "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(Integer.valueOf(jobIdentifier.getStageCounter())).isEqualTo(pipeline.getStages().byName(fixture.devStage).getCounter());
    }

    @Test
    public void canSupportQueryingUsingPipelineNameWithDifferentCase() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName().toUpperCase(), JobIdentifier.LATEST, fixture.devStage, "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);

        assertThat(jobIdentifier.getPipelineName()).isEqualTo(pipeline.getName());
    }

    @Test
    public void canSupportQueryingUsingStageNameWithDifferentCase() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getCounter().toString(), fixture.devStage.toUpperCase(), "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);

        assertThat(jobIdentifier.getStageName()).isEqualTo(fixture.devStage);
    }

    @Test
    public void shouldFindJobByPipelineCounter() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result).isEqualTo(expect);
    }

    @Test
    public void shouldFindOriginalWhenJobCopiedForRerun() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.findJob(PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        Stage rerunStage = instanceFactory.createStageForRerunOfJobs(stage, List.of(PipelineWithTwoStages.DEV_STAGE_SECOND_JOB),
                new DefaultSchedulingContext("loser", new Agents()),
                fixture.pipelineConfig().getStage(
                        new CaseInsensitiveString(fixture.devStage)), new TimeProvider(), "md5");
        stageDao.saveWithJobs(pipeline, rerunStage);
        dbHelper.passStage(rerunStage);

        JobIdentifier result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(rerunStage.getCounter()), job.getName());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result).isEqualTo(expect);

        long copiedJobId = rerunStage.getJobInstances().getByName(job.getName()).getId();
        assertThat(copiedJobId).isNotEqualTo(job.getId());//sanity check(its a copy, not the same)

        result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(),
                String.valueOf(rerunStage.getCounter()), job.getName());
        assertThat(result).isEqualTo(expect);//still, the job identifier returned should be the same(because other one was a copy)

        result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(),
                String.valueOf(rerunStage.getCounter()), job.getName(), copiedJobId);
        assertThat(result).isNotEqualTo(expect);//since caller knows the buildId, honor it(caller knows what she is doing)
        assertThat(result).isEqualTo(new JobIdentifier(rerunStage.getIdentifier(), job.getName(), copiedJobId));
    }

    @Test
    public void shouldReturnJobWithJobIdWhenSpecifyPipelineCounter() {
        configHelper.setPipelineLabelTemplate(fixture.pipelineName, "label-${COUNT}");
        Pipeline oldPipeline = fixture.createdPipelineWithAllStagesPassed();
        fixture.createdPipelineWithAllStagesPassed();

        Stage stage = oldPipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(oldPipeline.getName(), String.valueOf(oldPipeline.getCounter()), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), null);
        JobIdentifier expect = new JobIdentifier(oldPipeline, stage, job);
        assertThat(result).isEqualTo(expect);
    }

    @Test
    public void shouldReturnJobWithLabelWhenSpecifyPipelineLabel() {
        configHelper.setPipelineLabelTemplate(fixture.pipelineName, "label-${COUNT}");
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(pipeline.getName(), pipeline.getCounter().toString(),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result).isEqualTo(expect);
    }

    @Test
    public void shouldTranslateLatestStageCounter() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        StageIdentifier stageIdentifier = restfulService.translateStageCounter(pipeline.getIdentifier(),
                fixture.devStage, "latest");
        assertThat(stageIdentifier).isEqualTo(new StageIdentifier(pipeline, pipeline.getStages().byName(fixture.devStage)));
    }

}
