/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.stackdrive.sonarqube.properties;

import org.stackdrive.sonarqube.audit.StackDriveLogService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class StackDriveProperties {

    private static final Logger LOGGER = Loggers.get(StackDriveProperties.class);

    private static final String STACKDRIVE_LOG_HOST = "stackdrive.loghost";

    private static final String STACKDRIVE_BUILD_VERSION = "stackdrive.build.version";

    public String getBuildVersion() {
        return getProperty(STACKDRIVE_BUILD_VERSION);
    }

    public String getLogHost() {
        return getProperty(STACKDRIVE_LOG_HOST);
    }

    private String getProperty(String key) {
        try {
            try (InputStream inputStream = StackDriveLogService.class.getResourceAsStream("/stack-drive.properties")) {
                Properties properties = new Properties();
                if (inputStream != null) {
                    properties.load(inputStream);
                    return properties.getProperty(key);
                }
            }
        } catch (IOException e) {
            LOGGER.error("getProperty", e);
        }
        return key;
    }
}