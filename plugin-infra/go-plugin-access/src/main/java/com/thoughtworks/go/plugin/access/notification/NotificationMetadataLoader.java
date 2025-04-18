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
package com.thoughtworks.go.plugin.access.notification;

import com.thoughtworks.go.plugin.access.common.MetadataLoader;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetadataLoader extends MetadataLoader<NotificationPluginInfo> {
    @Autowired
    public NotificationMetadataLoader(PluginManager pluginManager, NotificationPluginInfoBuilder builder, NotificationExtension extension) {
        this(pluginManager, NotificationMetadataStore.instance(), builder, extension);
    }

    protected NotificationMetadataLoader(PluginManager pluginManager, NotificationMetadataStore metadataStore, NotificationPluginInfoBuilder builder, NotificationExtension extension) {
        super(pluginManager, builder, metadataStore, extension);
    }

}
