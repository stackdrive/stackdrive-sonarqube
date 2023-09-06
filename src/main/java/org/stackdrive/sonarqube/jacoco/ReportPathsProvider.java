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
package org.stackdrive.sonarqube.jacoco;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

class ReportPathsProvider {
    private static final Logger LOG = Loggers.get(ReportPathsProvider.class);

    private static final String[] DEFAULT_PATHS = {"target/site/jacoco/jacoco.xml", "target/site/jacoco-it/jacoco.xml", "build/reports/jacoco/test/jacocoTestReport.xml"};
    static final String REPORT_PATHS_PROPERTY_KEY = "sonar.coverage.jacoco.xmlReportPaths";

    private final SensorContext context;

    ReportPathsProvider(SensorContext context) {
        this.context = context;
    }

    Collection<Path> getPaths() {
        Path baseDir = context.fileSystem().baseDir().toPath().toAbsolutePath();

        List<String> patternPathList = Stream.of(context.config().getStringArray(REPORT_PATHS_PROPERTY_KEY))
                .filter(pattern -> !pattern.isEmpty())
                .collect(Collectors.toList());

        Set<Path> reportPaths = new HashSet<>();
        if (!patternPathList.isEmpty()) {
            for (String patternPath : patternPathList) {
                List<Path> paths = WildcardPatternFileScanner.scan(baseDir, patternPath);
                if (paths.isEmpty() && patternPathList.size() > 1) {
                    LOG.info("Coverage report doesn't exist for pattern: '{}'", patternPath);
                }
                reportPaths.addAll(paths);
            }
        }

        if (!reportPaths.isEmpty()) {
            return reportPaths;
        } else {
            if (!patternPathList.isEmpty()) {
                LOG.warn("No coverage report can be found with sonar.coverage.jacoco.xmlReportPaths='{}'. Using default locations: {}",
                        String.join(",", patternPathList), String.join(",", DEFAULT_PATHS));
            } else {
                LOG.info("'sonar.coverage.jacoco.xmlReportPaths' is not defined. Using default locations: {}", String.join(",", DEFAULT_PATHS));
            }
            return Arrays.stream(DEFAULT_PATHS)
                    .map(baseDir::resolve)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toSet());
        }
    }
}