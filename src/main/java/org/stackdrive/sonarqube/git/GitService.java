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
package org.stackdrive.sonarqube.git;

import org.stackdrive.sonarqube.properties.StackDriveProperties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class GitService {

    private static final Logger LOGGER = Loggers.get(GitService.class);

    private static final String BUILD_VERSION = new StackDriveProperties().getBuildVersion();

    private final StackDriveProperties stackProperties;

    public GitService() {
        this.stackProperties = new StackDriveProperties();
        LOGGER.trace("StackDrive Log Service Url {} ", stackProperties.getLogHost() + "/audit");
    }

    public RepositoryBuilder getVerifiedRepositoryBuilder(Path basedir) {
        RepositoryBuilder builder = new RepositoryBuilder()
                .findGitDir(basedir.toFile())
                .setMustExist(true);

        if (builder.getGitDir() == null) {
            throw MessageException.of("Not inside a Git work tree: " + basedir);
        }
        return builder;
    }

    public Repository buildRepository(Path basedir) {
        try {
            Repository repo = getVerifiedRepositoryBuilder(basedir).build();
            try (ObjectReader objReader = repo.getObjectDatabase().newReader()) {
                // SONARSCGIT-2 Force initialization of shallow commits to avoid later concurrent modification issue
                objReader.getShallowCommits();
                return repo;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open Git repository", e);
        }
    }

    public boolean supports(File baseDir) {
        RepositoryBuilder builder = new RepositoryBuilder().findGitDir(baseDir);
        return builder.getGitDir() != null;
    }

    public LastCommitInfo getLastCommitInfo(File baseDir) {
        String lastAuthor = "empty";
        String repoName = "empty";
        String bitbucketRepo = "empty";
        String hash = "empty";
        try (Repository repo = buildRepository(baseDir.toPath()); Git git = Git.wrap(repo)) {
            try {
                Iterable<RevCommit> logs = git.log().setMaxCount(1).call();
                for (RevCommit rev : logs) {
                    lastAuthor = rev.getAuthorIdent().getEmailAddress();
                    hash = rev.getName();
                }
                List<RemoteConfig> remoteConfigs = git.remoteList().call();
                for (RemoteConfig ref : remoteConfigs) {
                    List<URIish> urIs = ref.getURIs();
                    for (URIish urIish : urIs) {
                        repoName = urIish.getHumanishName();
                        bitbucketRepo = createHttps(urIish.getPath(), urIish.getHost(), urIish.getHumanishName());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("getLastCommitInfo", e);
            }
        }
        return new LastCommitInfo(lastAuthor, repoName, bitbucketRepo, hash);
    }

    private String createHttps(String path, String host, String repo) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://");
        sb.append(host);

        if (!BUILD_VERSION.contains("DEV")) {
            sb.append("/stash");
        }

        sb.append("/projects/");
        final String[] split = path.split("/");
        final String project = split[split.length - 2];
        sb.append(project);

        sb.append("/repos/");
        sb.append(repo);
        return sb.toString();
    }
}