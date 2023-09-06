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
package org.stackdrive.sonarqube.audit;

import org.stackdrive.sonarqube.properties.StackDriveProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.stackdrive.audit.dto.AuditDTO;
import org.stackdrive.audit.dto.Environment;
import org.stackdrive.audit.logger.StackDriveLogLogger;

public class StackDriveLogService {

    private static final Logger LOGGER = Loggers.get(StackDriveLogService.class);

    private final String buildVersion;

    private final StackDriveLogLogger stackDriveLog;

    public StackDriveLogService() {
        StackDriveProperties stackProperties = new StackDriveProperties();
        this.buildVersion = stackProperties.getBuildVersion();
        this.stackDriveLog = new StackDriveLogLogger(stackProperties.getLogHost() + "/audit");
        LOGGER.trace("StackDrive Log Service Url {} ", stackProperties.getLogHost() + "/audit");
    }

    public void sendLog(String code, String repo, String user) {
        LOGGER.trace("StackDrive sendLog >>> {} {} {}", code, repo, user);
        AuditDTO auditDTO = new AuditDTO();
        auditDTO.setCode(code);
        auditDTO.setLogin(user);
        auditDTO.setProject(repo);

        auditDTO.setEnv(Environment.SONARQUBE);
        auditDTO.setVersion(buildVersion);

        stackDriveLog.asyncSend(auditDTO);
        LOGGER.trace("StackDrive sendLog <<< {} {} {}", code, repo, user);
        sendAREventIfNeeded(code, repo, user);
    }

    public void sendLog(String code, String repo, String user, Object extension) {
        LOGGER.trace("StackDrive sendLog >>> {} {} {}", code, repo, user);
        AuditDTO auditDTO = new AuditDTO();
        auditDTO.setCode(code);
        auditDTO.setLogin(user);
        auditDTO.setProject(repo);

        auditDTO.setEnv(Environment.SONARQUBE);
        auditDTO.setVersion(buildVersion);

        auditDTO.setExtension(extension);

        stackDriveLog.asyncSend(auditDTO);
        LOGGER.trace("StackDrive sendLog <<< {} {} {}", code, repo, user);
        sendAREventIfNeeded(code, repo, user);
    }

    /**
     * Отправляет синтетически AREvent
     * <p>
     * Данный event необходим для подсчета Adoption Rate продукта,
     * данный тип event-ов используется только командой метрик
     */
    private void sendAREventIfNeeded(String code, String repo, String user) {
        if (EventCode.AR_EVENTS.contains(code)) {
            sendLog(EventCode.AR_EVENT, repo, user);
        }
    }
}