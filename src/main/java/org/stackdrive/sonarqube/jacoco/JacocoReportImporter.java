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

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JacocoReportImporter {

    private static final Logger LOG = Loggers.get(JacocoReportImporter.class);

    public CoverageReport analyse(SensorContext context) {
        ReportPathsProvider reportPathsProvider = new ReportPathsProvider(context);
        return importReports(reportPathsProvider);
    }

    CoverageReport importReports(ReportPathsProvider reportPathsProvider) {
        Collection<Path> reportPaths = reportPathsProvider.getPaths();

        CoverageReport coverageReport = new CoverageReport();
        if (reportPaths.isEmpty()) {
            LOG.info("StackDrive - No report imported, no coverage information will be imported by JaCoCo XML Report Importer");
            return coverageReport;
        }

        LOG.info("StackDrive - Importing {} report(s). Turn your logs in debug mode in order to see the exhaustive list.", reportPaths.size());
        for (Path reportPath : reportPaths) {
            LOG.info("StackDrive - Reading report '{}'", reportPath);
            try {
                final CoverageReport report = importReport(new XmlReportParser(reportPath));
                coverageReport.add(report.getCovered(), report.getMissed());
            } catch (Exception e) {
                LOG.warn("StackDrive - Coverage report '{}' could not be read/imported. Error: {}", reportPath, e);
            }
        }
        return coverageReport;
    }

    CoverageReport importReport(XmlReportParser reportParser) {
        List<XmlReportParser.Counter> counters = reportParser.parse();

        CoverageReport coverageReport = new CoverageReport();
        for (XmlReportParser.Counter counter : counters)
            try {
                if (Objects.equals("LINE", counter.getType()) && (counter.getCoveredLines() > 0 || counter.getMissedLines() > 0)) {
                    final int covered = Math.max(counter.getCoveredLines(), 0);
                    final int missed = Math.max(counter.getMissedLines(), 0);
                    coverageReport.add(covered, missed);
                    LOG.info("StackDrive - Import report: covered {} missed {}", coverageReport.getCovered(), coverageReport.getMissed());
                }
            } catch (IllegalStateException e) {
                LOG.warn("StackDrive - Cannot import coverage information, coverage data is invalid. Error: {}", e);
            }
        return coverageReport;
    }
}