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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Common rule definition
 */
public class StackDriveRuleDefinition implements RulesDefinition {
    static final String QUALITY_PROFILE_NAME = "StackDrive";
    static final String REPOSITORY = "StackDrive";
    static final String JAVA_LANGUAGE = "java";
    static final String TAG = "stackdrive";
    static final RuleKey BUG_ERROR = RuleKey.of(REPOSITORY, "Bug error level scope");
    static final RuleKey BUG_WARNING = RuleKey.of(REPOSITORY, "Bug warning level scope");
    static final RuleKey BUG_INFO = RuleKey.of(REPOSITORY, "Bug info level scope");
    static final RuleKey THREAT_ERROR = RuleKey.of(REPOSITORY, "Threat error level scope");
    static final RuleKey THREAT_WARNING = RuleKey.of(REPOSITORY, "Threat warning level scope");
    static final RuleKey THREAT_INFO = RuleKey.of(REPOSITORY, "Threat info level scope");

    @Override
    public void define(RulesDefinition.Context context) {
        RulesDefinition.NewRepository repository = context.createRepository(REPOSITORY, JAVA_LANGUAGE)
                .setName("StackDrive bugs & threats");
        repository.createRule(BUG_ERROR.rule())
                .setName("StackDrive - Bug error level scope")
                .setHtmlDescription("StackDrive - Bug error level scope").setTags(TAG)
                .setType(RuleType.BUG).setSeverity(Severity.MAJOR);
        repository.createRule(BUG_WARNING.rule())
                .setName("StackDrive - Bug warning level scope")
                .setHtmlDescription("StackDrive - Bug warning level scope").setTags(TAG)
                .setType(RuleType.BUG).setSeverity(Severity.MINOR);
        repository.createRule(BUG_INFO.rule())
                .setName("StackDrive - Bug info level scope")
                .setHtmlDescription("StackDrive - Bug info level scope").setTags(TAG)
                .setType(RuleType.BUG).setSeverity(Severity.INFO);
        repository.createRule(THREAT_ERROR.rule())
                .setName("StackDrive - Threat error level scope")
                .setHtmlDescription("StackDrive - Threat error level scope").setTags(TAG)
                .setType(RuleType.VULNERABILITY).setSeverity(Severity.MAJOR);
        repository.createRule(THREAT_WARNING.rule())
                .setName("StackDrive - Threat warning level scope")
                .setHtmlDescription("StackDrive - Threat warning level scope").setTags(TAG)
                .setType(RuleType.VULNERABILITY).setSeverity(Severity.MINOR);
        repository.createRule(THREAT_INFO.rule())
                .setName("StackDrive - Threat info level scope")
                .setHtmlDescription("StackDrive - Threat info level scope").setTags(TAG)
                .setType(RuleType.VULNERABILITY).setSeverity(Severity.INFO);

        repository.done();
    }
}