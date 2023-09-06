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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.stackdrive.report.model.SectionDTO;
import org.stackdrive.report.model.SolutionDTO;
import org.stackdrive.report.model.tabbed.TabPane;
import org.stackdrive.report.model.tabbed.Tabbed;
import org.stackdrive.report.model.views.table.DataTable;
import org.stackdrive.report.model.views.table.TableRow;
import org.stackdrive.sonarqube.audit.EventCode;
import org.stackdrive.sonarqube.audit.StackDriveLogService;
import org.stackdrive.sonarqube.git.GitService;
import org.stackdrive.sonarqube.git.LastCommitInfo;
import org.stackdrive.sonarqube.jacoco.CoverageReport;
import org.stackdrive.sonarqube.jacoco.JacocoReportImporter;
import org.stackdrive.sonarqube.model.*;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Sensor checks <b>stackdrive_solution.json</b> files. Takes all bugs and put's them to sonar qube report
 */
public class StackDriveSensor implements Sensor {

    /**
     * The logger object for the sensor.
     */
    private static final Logger LOGGER = Loggers.get(StackDriveSensor.class);

    private static final String STACKDRIVE_DIR = ".stackdrive";

    private static final String STACKDRIVE_REPORT = "maintenance.json";

    private static final String STACKDRIVE_CONFIG = "stackdrive.properties";

    private static final String BITBUCKET_REPO = "bitbucketRepo";

    private static final String COMMIT_HASH = "commitHash";

    private final StackDriveLogService logService;

    private final GitService gitService;

    public StackDriveSensor() {
        this.logService = new StackDriveLogService();
        this.gitService = new GitService();
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.createIssuesForRuleRepository(StackDriveRuleDefinition.REPOSITORY);
        descriptor.name("StackDrive sonar problems");
        descriptor.onlyOnLanguage("java");
    }

    @Override
    public void execute(SensorContext sensorContext) {
        try {
            FileSystem fs = sensorContext.fileSystem();
            if (gitService.supports(fs.baseDir())) {
                LastCommitInfo lastCommitInfo = gitService.getLastCommitInfo(fs.baseDir());
                sendValidationPresented(lastCommitInfo);
                LOGGER.info("StackDrive - Last commit info {} {} {} {}", lastCommitInfo.getLastAuthor(), lastCommitInfo.getRepoName(), lastCommitInfo.getBitbucketRepo(), lastCommitInfo.getHash());

                sendCodeReview(sensorContext, lastCommitInfo);
                sendValidationVerified(sensorContext, lastCommitInfo);
            }

            Path reportFile = getFilePath(sensorContext, STACKDRIVE_REPORT);
            LOGGER.info("Report file exists? {}", Files.exists(reportFile));
            if (Files.exists(reportFile)) {
                ParsingResult parsingResult = new ParsingResult();
                try {
                    this.loadJson(reportFile.toFile(), parsingResult);
                } catch (Exception e) {
                    LOGGER.warn("Can't parse JSON", e);
                }
                LOGGER.info("Загружен файл '{}' с отчётом о валидации проекта", STACKDRIVE_REPORT);
                setIssuesOnFile(sensorContext, parsingResult.getProblemList());

                if (Objects.nonNull(parsingResult.getTrashyList()) && !parsingResult.getTrashyList().isEmpty()) {
                    if (gitService.supports(fs.baseDir())) {
                        LastCommitInfo lastCommitInfo = gitService.getLastCommitInfo(fs.baseDir());
                        sendCheckList(parsingResult.getTrashyList(), lastCommitInfo);
                    }
                }
            } else {
                LOGGER.info("Не обнаружен файл '{}' с отчётом о валидации проекта", STACKDRIVE_REPORT);
            }
        } catch (Exception e) {
            LOGGER.warn("StackDriveSensor execute fail", e);
        }
    }

    /**
     * Loads json file
     *
     * @param jsonFile
     * @param parsingResult
     */
    private void loadJson(File jsonFile, ParsingResult parsingResult) {
        SolutionDTO root = loadReport(jsonFile);
        if (root == null) {
            return;
        }
        Map<String, SectionDTO> sectionList = root.getSectionList();
        SectionDTO section = sectionList.get("Risks");
        if (section == null) {
            return;
        }
        List<Tabbed> tabbedList = section.getTabbedList();
        TabPane bugTable = getBugTable(tabbedList);
        TabPane threatTable = getThreatTable(tabbedList);

        parsingResult.getProblemList().addAll(this.getProblems(Status.BUG, bugTable));
        parsingResult.getProblemList().addAll(this.getProblems(Status.THREAT, threatTable));

        TabPane okTable = getOkTable(tabbedList);
        parsingResult.getTrashyList().addAll(this.getTrashyObjects(okTable));
    }

    /**
     * Parses problems from table
     *
     * @param status
     * @param table
     * @return
     */
    private List<Problem> getProblems(Status status, TabPane table) {
        List<Problem> problemList = new ArrayList<>();
        DataTable tableBody = (DataTable) table.getElement();
        for (TableRow dataNode : tableBody.getTable()) {
            String element = dataNode.getCell2();
            String code = dataNode.getCell1();
            Problem problem = new Problem(element, status, this.getSeverety(dataNode), code);
            problemList.add(problem);
        }
        return problemList;
    }

    /**
     * Parses trashy object from table
     *
     * @param table
     * @return
     */
    private List<TrashyObject> getTrashyObjects(TabPane table) {
        List<TrashyObject> trashyObjects = new ArrayList<>();
        DataTable tableBody = (DataTable) table.getElement();
        for (TableRow dataNode : tableBody.getTable()) {
            TrashyObject trashyObject = new TrashyObject();
            final String cell1 = dataNode.getCell1();
            if (Objects.nonNull(cell1)) {
                if (cell1.contains("_off")) {
                    trashyObject.setEnabled(false);
                    trashyObject.setExceptionCount(0);
                    trashyObject.setValidationCount(0);
                } else {
                    trashyObject.setEnabled(true);
                }
            }

            final String cell2 = dataNode.getCell2();
            if (Objects.nonNull(cell2)) {
                final String[] split = cell2.split(":");
                trashyObject.setValidatorCode(split[0]);
            }

            final String cell4 = dataNode.getCell4();
            if (Objects.nonNull(cell4)) {
                trashyObject.setExceptionCount(Integer.parseInt(cell4));
            }

            final String cell3 = dataNode.getCell3();
            if (Objects.nonNull(cell3)) {
                if (cell3.contains("problem_ok")) {
                    trashyObject.setValidationCount(trashyObject.getExceptionCount());
                } else {
                    trashyObject.setValidationCount(Integer.parseInt(cell3));
                }
            }

            trashyObjects.add(trashyObject);
        }
        return trashyObjects;
    }

    /**
     * Returns severety
     *
     * @param node
     * @return
     */
    private Severety getSeverety(TableRow node) {
        String img = node.getImageCode();
        if ("balloon_info".equals(img)) {
            return Severety.INFO;
        }
        if ("balloon_warning".equals(img)) {
            return Severety.WARNING;
        }
        if ("balloon_error".equals(img)) {
            return Severety.ERROR;
        }
        return Severety.INFO;
    }

    /**
     * Find file
     *
     * @param context
     * @param fileName
     * @return file in base dir or parent dir
     */
    private Path getFilePath(SensorContext context, String fileName) {
        FileSystem fs = context.fileSystem();
        final Path modulePath = Paths.get(fs.baseDir().getAbsolutePath());
        final Path defaultPath = Paths.get(modulePath.toString(), STACKDRIVE_DIR, fileName);
        final Path parentPath = Paths.get(modulePath.getParent().toString(), STACKDRIVE_DIR, fileName);
        final Path parentOfParentPath = Paths.get(modulePath.getParent().getParent().toString(), STACKDRIVE_DIR, fileName);
        final Path parentOfParentOfParentPath = Paths.get(modulePath.getParent().getParent().getParent().toString(), STACKDRIVE_DIR, fileName);
        if (Files.exists(defaultPath)) {
            return defaultPath;
        } else if (Files.exists(parentPath)) {
            return parentPath;
        } else if (Files.exists(parentOfParentPath)) {
            return parentOfParentPath;
        } else if (Files.exists(parentOfParentOfParentPath)) {
            return parentOfParentOfParentPath;
        } else {
            return defaultPath;
        }
    }

    /**
     * Loads report
     *
     * @param jsonFile
     * @return
     */
    private SolutionDTO loadReport(File jsonFile) {
        try (final FileReader json = new FileReader(jsonFile)) {
            final TypeToken<SolutionDTO> requestListTypeToken = new TypeToken<SolutionDTO>() {
            };
            final Gson gson = new GsonBuilder().registerTypeAdapter(TabPane.class, new TabPaneDeserializer()).create();
            return gson.fromJson(json, requestListTypeToken.getType());
        } catch (Exception e) {
            LOGGER.warn("loadReport", e);
            return null;
        }
    }

    /**
     * Adds issues on sonar context
     *
     * @param context
     * @param problems
     */
    private void setIssuesOnFile(SensorContext context, List<Problem> problems) {
        try {
            FileSystem fs = context.fileSystem();
            FilePredicate predicateAll = fs.predicates().hasType(InputFile.Type.MAIN);
            Map<String, List<Problem>> problemMap = new HashMap();
            for (Problem problem : problems) {
                List<Problem> pl = problemMap.get(problem.clazz());
                if (pl == null) {
                    pl = new ArrayList<>();
                    problemMap.put(problem.clazz(), pl);
                }
                pl.add(problem);
            }
            // Adds file problems
            for (InputFile f : fs.inputFiles(predicateAll)) {
                String name = f.filename().substring(0, f.filename().length() - 5);
                LOGGER.info("File " + name);
                List<Problem> pl = problemMap.get(name);
                if (pl != null) {
                    for (Problem problem : pl) {
                        this.createIssue(context, f, problem);
                    }
                }
            }
            // Adds project problems
            List<Problem> pl = problemMap.get("project");
            if (pl != null) {
                for (Problem problem : pl) {
                    this.createIssue(context, null, problem);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("failed to add issues on sonar context", e);
        }
    }

    /**
     * Creates issue by specified problem
     *
     * @param problem
     */
    private void createIssue(SensorContext context, InputFile f, Problem problem) {
        if (problem.isProjectProblem()) {
            NewIssue issue = context.newIssue().forRule(this.getRuleForProblem(problem));
            NewIssueLocation primaryLocation = issue.newLocation()
                    .on(context.module())
                    .message(problem.getMessage());
            issue.at(primaryLocation);
            issue.save();
        } else if (f.lines() <= problem.line()) {
            NewIssue issue = context.newIssue().forRule(this.getRuleForProblem(problem));
            NewIssueLocation primaryLocation = issue.newLocation()
                    .on(f)
                    .at(f.selectLine(problem.line()))
                    .message(problem.getMessage());
            issue.at(primaryLocation);
            issue.save();
        }
    }

    /**
     * Returns rule for problem
     *
     * @param problem
     * @return
     */
    private RuleKey getRuleForProblem(Problem problem) {
        if (problem.getStatus() == Status.BUG && problem.getSeverety() == Severety.ERROR) {
            return StackDriveRuleDefinition.BUG_ERROR;
        }
        if (problem.getStatus() == Status.BUG && problem.getSeverety() == Severety.WARNING) {
            return StackDriveRuleDefinition.BUG_WARNING;
        }
        if (problem.getStatus() == Status.BUG && problem.getSeverety() == Severety.INFO) {
            return StackDriveRuleDefinition.BUG_INFO;
        }
        if (problem.getStatus() == Status.THREAT && problem.getSeverety() == Severety.ERROR) {
            return StackDriveRuleDefinition.THREAT_ERROR;
        }
        if (problem.getStatus() == Status.THREAT && problem.getSeverety() == Severety.WARNING) {
            return StackDriveRuleDefinition.THREAT_WARNING;
        }
        if (problem.getStatus() == Status.THREAT && problem.getSeverety() == Severety.INFO) {
            return StackDriveRuleDefinition.THREAT_INFO;
        }
        return StackDriveRuleDefinition.THREAT_INFO;
    }

    private TabPane getBugTable(List<Tabbed> tabbedList) {
        for (Tabbed tabbed : tabbedList) {
            final List<TabPane> tabPaneList = tabbed.getTabPaneList();
            TabPane bugTable = tabPaneList.stream()
                    .filter(tabPane -> "list_bug".equals(tabPane.getTitle()))
                    .findAny()
                    .orElse(null);
            if (Objects.nonNull(bugTable)) {
                return bugTable;
            }
        }
        return null;
    }

    private TabPane getThreatTable(List<Tabbed> tabbedList) {
        for (Tabbed tabbed : tabbedList) {
            final List<TabPane> tabPaneList = tabbed.getTabPaneList();
            TabPane threatTable = tabPaneList.stream()
                    .filter(tabPane -> "list_threat".equals(tabPane.getTitle()))
                    .findAny()
                    .orElse(null);
            if (Objects.nonNull(threatTable)) {
                return threatTable;
            }
        }
        return null;
    }

    private TabPane getOkTable(List<Tabbed> tabbedList) {
        for (Tabbed tabbed : tabbedList) {
            final List<TabPane> tabPaneList = tabbed.getTabPaneList();
            TabPane threatTable = tabPaneList.stream()
                    .filter(tabPane -> "list_ok".equals(tabPane.getTitle()))
                    .findAny()
                    .orElse(null);
            if (Objects.nonNull(threatTable)) {
                return threatTable;
            }
        }
        return null;
    }

    private void sendCodeReview(SensorContext sensorContext, LastCommitInfo lastCommitInfo) {
        try {
            JacocoReportImporter jacocoReportImporter = new JacocoReportImporter();
            final CoverageReport report = jacocoReportImporter.analyse(sensorContext);

            Map<String, Object> extension = new HashMap<>();
            extension.put("all_code", report.getAll());
            extension.put("module", (extractModule(sensorContext.module().key())).toLowerCase());
            extension.put("module_key", sensorContext.module().key());
            extension.put("module_full", (lastCommitInfo.getRepoName() + ":" + extractModule(sensorContext.module().key())).toLowerCase());

            logService.sendLog(EventCode.CODE_REVIEW_EVENT, lastCommitInfo.getRepoName(), lastCommitInfo.getLastAuthor(), extension);
            LOGGER.info("StackDrive - Code review report send, all {}", report.getAll());
        } catch (Exception e) {
            LOGGER.warn("sendCodeReview", e);
        }
    }

    private void sendValidationPresented(LastCommitInfo lastCommitInfo) {
        Map<String, Object> cle = new HashMap<>();
        cle.put(BITBUCKET_REPO, lastCommitInfo.getBitbucketRepo());
        cle.put(COMMIT_HASH, lastCommitInfo.getHash());
        logService.sendLog(EventCode.VALIDATION_PRESENTED, lastCommitInfo.getRepoName(), lastCommitInfo.getLastAuthor(), cle);
    }

    private void sendValidationVerified(SensorContext sensorContext, LastCommitInfo lastCommitInfo) {
        Path configFile = getFilePath(sensorContext, STACKDRIVE_CONFIG);
        if (Files.exists(configFile)) {
            Map<String, Object> cle = new HashMap<>();
            cle.put(BITBUCKET_REPO, lastCommitInfo.getBitbucketRepo());
            cle.put(COMMIT_HASH, lastCommitInfo.getHash());
            logService.sendLog(EventCode.VALIDATION_VERIFIED, lastCommitInfo.getRepoName(), lastCommitInfo.getLastAuthor(), cle);
            LOGGER.info("StackDrive - Config file exists {}", configFile);
        }
    }

    private void sendCheckList(List<TrashyObject> problemList, LastCommitInfo lastCommitInfo) {
        final long runUID = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        for (TrashyObject trashyObject : problemList) {
            Map<String, Object> cle = new HashMap<>();
            cle.put(BITBUCKET_REPO, lastCommitInfo.getBitbucketRepo());
            cle.put(COMMIT_HASH, lastCommitInfo.getHash());
            cle.put("serviceCode", lastCommitInfo.getRepoName());
            cle.put("isEnabled", trashyObject.getValidatorCode());
            cle.put("validatorCode", trashyObject.getValidatorCode());
            cle.put("validationCount", trashyObject.getValidationCount());
            cle.put("exceptionCount", trashyObject.getExceptionCount());
            cle.put("runUID", runUID);
            logService.sendLog(EventCode.CHECK_LIST_EVENT, lastCommitInfo.getRepoName(), lastCommitInfo.getLastAuthor(), cle);
        }
    }

    private String extractModule(String moduleKey) {
        if (Objects.nonNull(moduleKey)) {
            final String[] split = moduleKey.split(":");
            if (split.length > 0) {
                return split[split.length - 1];
            }
        }
        return "";
    }
}