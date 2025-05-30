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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;

public class EmailMessageDrafter {
    private static final String LOW_ARTIFACTS_DISK_SPACE_EMAIL =
            "The email has been sent out automatically by the Go server at (%s) to Go administrators.\n"
                    + "\n"
                    + "This server has less than %sMb of disk space available at %s to store artifacts. "
                    + "When the available space goes below %sMb, Go will stop scheduling. "
                    + "Please ensure enough space is available. You can read more about Go's artifacts repository, "
                    + "including our recommendation to create a separate partition for it at "
                    + docsUrl("/installation/configuring_server_details.html") + "\n";

    private static final String NO_ARTIFACTS_DISK_SPACE_EMAIL =
            "The email has been sent out automatically by the Go server at (%s) to Go administrators.\n"
                    + "\n"
                    + "This server has stopped scheduling "
                    + "because it has less than %sMb of disk space available at %s to store artifacts. "
                    + "Please ensure enough space is available. You can read more about Go's artifacts repository, "
                    + "including our recommendation to create a separate partition for it at "
                    + docsUrl("/installation/configuring_server_details.html") + "\n";

    private static final String LOW_DATABASE_DISK_SPACE_EMAIL =
            """
                    The email has been sent out automatically by the Go server at (%s) to Go administrators.

                    This server has less than %sMb of disk space available at %s to store data. When the available space goes below %sMb, Go will stop scheduling. Please ensure enough space is available.
                    """;

    private static final String NO_DATABASE_DISK_SPACE_EMAIL =
            """
                    The email has been sent out automatically by the Go server at (%s) to Go administrators.

                    This server has stopped scheduling because it has less than %sMb of disk space available at %s to store data. Please ensure enough space is available.
                    """;

    public static SendEmailMessage lowArtifactsDiskSpaceMessage(SystemEnvironment systemEnvironment, String adminMail,
                                                                String targetFolder) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        return new SendEmailMessage(
                "Low artifacts disk space warning message from Go Server at " + ipAddress, String.format(
                        LOW_ARTIFACTS_DISK_SPACE_EMAIL, ipAddress, systemEnvironment.getArtifactRepositoryWarningLimit(),
                        targetFolder,
                        systemEnvironment.getArtifactRepositoryFullLimit()), adminMail);
    }

    public static SendEmailMessage noArtifactsDiskSpaceMessage(SystemEnvironment systemEnvironment, String adminMail,
                                                               String targetFolder) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        long size = systemEnvironment.getArtifactRepositoryFullLimit();
        return new SendEmailMessage(
                "No artifacts disk space error message from Go Server at " + ipAddress, String.format(
                        NO_ARTIFACTS_DISK_SPACE_EMAIL, ipAddress, size, targetFolder), adminMail);
    }

    public static SendEmailMessage lowDatabaseDiskSpaceMessage(SystemEnvironment systemEnvironment, String adminMail,
                                                               String targetFolder) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        return new SendEmailMessage(
                "Low database disk space warning message from Go Server at " + ipAddress, String.format(
                        LOW_DATABASE_DISK_SPACE_EMAIL, ipAddress, systemEnvironment.getDatabaseDiskSpaceWarningLimit(),
                        targetFolder,
                        systemEnvironment.getDatabaseDiskSpaceFullLimit()), adminMail);
    }

    public static SendEmailMessage noDatabaseDiskSpaceMessage(SystemEnvironment systemEnvironment, String adminMail,
                                                              String targetFolder) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        return new SendEmailMessage(
                "No database disk space error message from Go Server at " + ipAddress, String.format(
                        NO_DATABASE_DISK_SPACE_EMAIL, ipAddress, systemEnvironment.getDatabaseDiskSpaceFullLimit(),
                        targetFolder), adminMail);
    }

    public static SendEmailMessage backupSuccessfullyCompletedMessage(String backupDir, String adminEmail, String username) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' was successfully completed. The backup is stored at location: %s. This backup was triggered by '%s'.", ipAddress, backupDir, username);
        return new SendEmailMessage("Server Backup Completed Successfully", body, adminEmail);
    }

    public static SendEmailMessage backupFailedMessage(String exceptionMessage, String adminEmail) {
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        return new SendEmailMessage("Server Backup Failed",String.format("Backup of the Go server at '%s' has failed. The reason is: %s", ipAddress, exceptionMessage),adminEmail);
    }
}
