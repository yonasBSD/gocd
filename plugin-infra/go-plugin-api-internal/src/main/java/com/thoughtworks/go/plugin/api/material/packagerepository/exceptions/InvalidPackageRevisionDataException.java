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
package com.thoughtworks.go.plugin.api.material.packagerepository.exceptions;

/**
 * Exception generated when the data related to package revision is invalid
 * - may be empty or may be invalid format.
 *
 * @author GoCD Team
 * @see com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision#addData(String, String)
 */
public class InvalidPackageRevisionDataException extends RuntimeException {
    public InvalidPackageRevisionDataException(String message) {
        super(message);
    }
}
