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

import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {PackagesCRUD} from "models/package_repositories/packages_crud";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {PackageRepositoriesPage} from "views/pages/package_repositories";
import {MaterialsWidget} from "./materials_widget";

export class MaterialsTabContent extends TabContent<PipelineConfig> {
  private readonly pluginInfos: Stream<PluginInfos>                 = Stream(new PluginInfos());
  private readonly packageRepositories: Stream<PackageRepositories> = Stream(new PackageRepositories());
  private readonly packages: Stream<Packages>                       = Stream(new Packages());
  private readonly scmMaterials: Stream<Scms>                       = Stream(new Scms());

  constructor() {
    super();
    this.fetchRelatedPluginInfos();
    this.fetchAllPackageReposAndPackages();
    this.fetchScmMaterials();
  }

  static tabName(): string {
    return "Materials";
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  name(): string {
    return "Materials";
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig, flashMessage: FlashMessageModelWithTimeout, pipelineConfigSave: () => Promise<any>, pipelineConfigReset: () => any) {
    const mergedPkgRepos = PackageRepositoriesPage.getMergedList(this.packageRepositories, this.packages);
    return <MaterialsWidget materials={entity.materials}
                            pipelineGroupName={entity.group()}
                            parentPipelineName={entity.name()}
                            readonly={this.isEntityDefinedInConfigRepository()}
                            pluginInfos={this.pluginInfos}
                            flashMessage={flashMessage}
                            packageRepositories={Stream(mergedPkgRepos)}
                            packages={this.packages}
                            scmMaterials={this.scmMaterials}
                            pipelineConfigSave={pipelineConfigSave}/>;
  }

  private fetchAllPackageReposAndPackages() {
    Promise.all([PackageRepositoriesCRUD.all(), PackagesCRUD.all()])
           .then((result) => {
             result[0].do((successResponse) => {
               this.packageRepositories(successResponse.body);
             }, super.pageLoadFailure);
             result[1].do((successResponse) => {
               this.packages(successResponse.body);
             }, super.pageLoadFailure);
           })
           .finally(() => super.pageLoaded());
  }

  private fetchScmMaterials() {
    PluggableScmCRUD.all()
                    .then((result) => {
                      result.do((successResponse) => {
                        this.scmMaterials(successResponse.body);
                        super.pageLoaded();
                      }, super.pageLoadFailure);
                    });
  }

  private fetchRelatedPluginInfos() {
    Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.PACKAGE_REPO}), PluginInfoCRUD.all({type: ExtensionTypeString.SCM})])
           .then((result) => {
             result.forEach((apiResult) => apiResult.do((successResponse) => {
               this.pluginInfos().push(...successResponse.body);
               this.pageLoaded();
             }, super.pageLoadFailure));
           });
  }
}
