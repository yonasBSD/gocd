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
package com.thoughtworks.go.server.web;


import com.thoughtworks.go.util.SystemEnvironment;

import java.util.Optional;

public class HstsHeader {

    private final long maxAge;
    private final boolean includeSubdomains;
    private final boolean preload;

    private HstsHeader(long maxAge, boolean includeSubdomains, boolean preload) {
        this.maxAge = maxAge;
        this.includeSubdomains = includeSubdomains;
        this.preload = preload;
    }

    public String headerName() {
        return "Strict-Transport-Security";
    }

    public String headerValue() {
        StringBuilder value = new StringBuilder("max-age=").append(maxAge);
        if (includeSubdomains) {
            value.append("; includeSubDomains");
        }
        if (preload) {
            value.append("; preload");
        }
        return value.toString();
    }

    public static Optional<HstsHeader> fromSystemEnvironment(SystemEnvironment systemEnvironment) {
        if (!systemEnvironment.enableHstsHeader()) {
            return Optional.empty();
        }
        HstsHeader hstsHeader = new HstsHeader(systemEnvironment.hstsHeaderMaxAgeInSeconds(),
                systemEnvironment.hstsHeaderIncludeSubdomains(), systemEnvironment.hstsHeaderPreload());
        return Optional.of(hstsHeader);
    }
}
