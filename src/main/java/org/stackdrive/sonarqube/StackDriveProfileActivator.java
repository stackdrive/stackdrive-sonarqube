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
package org.stackdrive.sonarqube;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

/**
 * Rule repository activator
 */
public class StackDriveProfileActivator implements BuiltInQualityProfilesDefinition {

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(StackDriveRuleDefinition.QUALITY_PROFILE_NAME,
                StackDriveRuleDefinition.JAVA_LANGUAGE);
        profile.setDefault(true);
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.BUG_ERROR.rule());
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.BUG_WARNING.rule());
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.BUG_INFO.rule());
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.THREAT_ERROR.rule());
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.THREAT_WARNING.rule());
        profile.activateRule(StackDriveRuleDefinition.REPOSITORY, StackDriveRuleDefinition.THREAT_INFO.rule());
        profile.done();
    }

}