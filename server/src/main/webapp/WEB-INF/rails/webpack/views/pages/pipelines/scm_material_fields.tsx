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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Filter} from "models/maintenance_mode/material";
import {
  GitMaterialAttributes,
  HgMaterialAttributes,
  Material,
  MaterialAttributes,
  P4MaterialAttributes,
  ScmMaterialAttributes,
  SvnMaterialAttributes,
  TfsMaterialAttributes
} from "models/materials/types";
import {
  CheckboxField,
  FormField,
  PasswordField,
  Size,
  TextAreaField,
  TextField
} from "views/components/forms/input_fields";
import {TestConnection} from "views/components/materials/test_connection";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {MaterialAutoUpdateToggle} from "views/pages/pipelines/material_auto_update_toggle";
import {DENYLIST_HELP_MESSAGE, DESTINATION_DIR_HELP_MESSAGE, IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";

interface Attrs {
  material: Material;
  hideTestConnection?: boolean;
  showLocalWorkingCopyOptions: boolean;
  disabled?: boolean;
  readonly?: boolean;
  parentPipelineName?: string;
  showGitMaterialShallowClone?: boolean;
  pipelineGroupName?: string;
}

function markAllDisabled(vnodes: m.ChildArray) {
  if (vnodes instanceof Array) {
    for (const vnode of (vnodes as m.Vnode[])) {
      if (vnode instanceof Array) {
        markAllDisabled(vnode);
      } else {
        //@ts-ignore
        if (FormField.isPrototypeOf(vnode.tag)) {
          (vnode.attrs as any).readonly = true;
          (vnode.attrs as any).disabled = true;
        } else {
          if (vnode.children instanceof Array) {
            markAllDisabled(vnode.children);
          }
        }
      }
    }
  }
  return vnodes;
}

abstract class ScmFields extends MithrilViewComponent<Attrs> {
  protected filterValue: Stream<string> = Stream("");

  oninit(vnode: m.Vnode<Attrs, this>): any {
    const attrs = vnode.attrs.material.attributes() as ScmMaterialAttributes;
    if (attrs.filter() === undefined) {
      attrs.filter(new Filter([]));
    }
    this.filterValue(attrs.filter()!.ignore().join(','));
  }

  errs(attrs: MaterialAttributes, key: string): string {
    return attrs.errors().errorsForDisplay(key);
  }

  view(vnode: m.Vnode<Attrs>): m.Children {
    const mattrs = vnode.attrs.material.attributes() as ScmMaterialAttributes;

    if (vnode.attrs.disabled) {
      return markAllDisabled(this.requiredFields(mattrs, vnode));
    }

    const fields: m.Children = [this.requiredFields(mattrs, vnode)];

    if (!vnode.attrs.hideTestConnection) {
      fields.push(<TestConnection material={vnode.attrs.material} pipeline={vnode.attrs.parentPipelineName} group={vnode.attrs.pipelineGroupName}/>);
    }

    fields.push(this.advancedOptions(mattrs, vnode));

    if (vnode.attrs.readonly) {
      return markAllDisabled(fields);
    }

    return fields;
  }

  advancedOptions(mattrs: ScmMaterialAttributes, vnode: m.Vnode<Attrs>): m.Children {
    const showLocalWorkingCopyOptions: boolean = vnode.attrs.showLocalWorkingCopyOptions;
    let settings = this.extraFields(mattrs, vnode);

    if (showLocalWorkingCopyOptions) {
      const labelForDestination = [
        "Alternate Checkout Path",
        " ",
        <Tooltip.Help size={TooltipSize.medium} content={DESTINATION_DIR_HELP_MESSAGE}/>
      ];
      const commonSettings      = [
        <TextField label={labelForDestination} property={mattrs.destination}
                   errorText={this.errs(mattrs, "destination")}/>,

        <TextField label="Material Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE}
                   placeholder="A human-friendly label for this material" property={mattrs.name}
                   errorText={this.errs(mattrs, "name")}/>,

        <MaterialAutoUpdateToggle toggle={mattrs.autoUpdate} errors={mattrs.errors()} disabled={vnode.attrs.disabled || vnode.attrs.readonly}/>,

        <TextField label="Denylist" helpText={DENYLIST_HELP_MESSAGE}
                   property={this.filterValue}
                   onchange={this.filterProxy.bind(this,mattrs)}
                   errorText={this.errs(mattrs, "filter")}/>,

        <CheckboxField property={mattrs.invertFilter} dataTestId={"invert-filter"}
                       label="Invert the file filter, e.g. a Denylist becomes an Allowlist instead."
                       errorText={this.errs(mattrs, "invertFilter")}/>
      ];
      settings                  = settings.concat(commonSettings);
    }

    const shouldForceOpen = mattrs.errors().hasErrors("name") ||
                            mattrs.errors().hasErrors("destination") ||
                            mattrs.errors().hasErrors("autoUpdate") ||
                            mattrs.errors().hasErrors("filter") ||
                            mattrs.errors().hasErrors("invertFilter");
    return <AdvancedSettings forceOpen={shouldForceOpen}>
      {settings}
    </AdvancedSettings>;
  }

  abstract requiredFields(attrs: MaterialAttributes, vnode: m.Vnode<Attrs>): m.ChildArray;

  abstract extraFields(attrs: MaterialAttributes, vnode: m.Vnode<Attrs>): m.ChildArray;

  protected filterProxy(attrs: ScmMaterialAttributes) {
    const filters = this.filterValue().split(',')
                        .map((val) => val.trim())
                        .filter((val) => val.length > 0);
    attrs.filter()!.ignore(filters);
  }
}

export class GitFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as GitMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes, vnode: m.Vnode<Attrs>): m.ChildArray {
    const mat = attrs as GitMaterialAttributes;

    const fields = [
      <TextField label="Repository Branch" property={mat.branch} errorText={this.errs(attrs, "branch")} placeholder="master"/>,
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>
    ];

    if (vnode.attrs.showGitMaterialShallowClone === undefined || vnode.attrs.showGitMaterialShallowClone === true) {
      fields.push(<CheckboxField label="Shallow clone (recommended for large repositories)" property={mat.shallowClone}/>);
    }

    return fields;
  }
}

export class HgFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as HgMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as HgMaterialAttributes;
    return [
      <TextField label="Repository Branch" property={mat.branch} placeholder="default"/>,
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
    ];
  }
}

export class SvnFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as SvnMaterialAttributes;
    return [<TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>];
  }

  extraFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as SvnMaterialAttributes;
    return [
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
      <CheckboxField label="Check Externals" property={mat.checkExternals}/>,
    ];
  }
}

export class P4Fields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as P4MaterialAttributes;
    return [
      <TextField label="P4 [Protocol:][Host:]Port" property={mat.port} errorText={this.errs(attrs, "port")} required={true}/>,
      <TextAreaField label="P4 View" property={mat.view} errorText={this.errs(attrs, "view")} required={true} rows={5} size={Size.MATCH_PARENT} resizable={true}/>,
    ];
  }

  extraFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as P4MaterialAttributes;
    return [
      <TextField label="Username" property={mat.username}/>,
      <PasswordField label="Password" property={mat.password}/>,
      <CheckboxField label="Use Ticket Authentication" property={mat.useTickets}/>,
    ];
  }
}

export class TfsFields extends ScmFields {
  requiredFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as TfsMaterialAttributes;
    return [
      <TextField label="Repository URL" property={mat.url} errorText={this.errs(attrs, "url")} required={true}/>,
      <TextField label="Project Path" property={mat.projectPath} errorText={this.errs(attrs, "projectPath")} required={true}/>,
      <TextField label="Username" property={mat.username} errorText={this.errs(attrs, "username")} required={true}/>,
      <PasswordField label="Password" property={mat.password} errorText={this.errs(attrs, "password")} required={false}/>,
    ];
  }

  extraFields(attrs: MaterialAttributes): m.ChildArray {
    const mat = attrs as TfsMaterialAttributes;
    return [<TextField label="Domain" property={mat.domain}/>];
  }
}
