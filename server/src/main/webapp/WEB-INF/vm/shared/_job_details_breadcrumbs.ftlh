<#--
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
 -->
<#if _page_title??>
<div id="job_details_header" class="job_details entity_title page_header">
  <div class="row">
    <span class="page_name">Job Details</span>
    <ul class="entity_title">
      <li class="name">
        <span class="label">Pipeline</span>
        <a href="${req.getContextPath()}/pipeline/activity/${presenter.pipelineName}"
           title="View this pipeline's activity">${presenter.pipelineName}</a>
      </li>
      <li class="pipeline_label">
        <span class="label">Instance</span>
        <span class="run_no">${presenter.pipelineLabel}</span>
        <a href="${req.getContextPath()}/pipelines/value_stream_map/${presenter.pipelineName}/${presenter.pipelineCounter?c}"
           title="View this stage's jobs summary">VSM</a>
      </li>
      <li class="stage_name">
        <span class="label">Stage</span>
        <a href="${req.getContextPath()}/pipelines/${presenter.stageLocator}"
           title="View this stage's details">${presenter.stageName} / ${presenter.stageCounter}</a>
      </li>
      <li class='last'>
        <span class="label">Job</span>
        <h1>${presenter.buildName}</h1></li>
    </ul>
      <#if userHasAdministratorRights || userHasGroupAdministratorRights >
        <div class="job_detail_setting">
            <a href="${req.getContextPath()}/admin/pipelines/${presenter.pipelineName}/general"
               class="icon16 setting"></a>
        </div>
      </#if>
    <div id="build_status" class="build-status"></div>
  </div>
</div>
</#if>
