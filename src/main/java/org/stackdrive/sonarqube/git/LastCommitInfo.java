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

public class LastCommitInfo {

    private final String lastAuthor;

    private final String repoName;

    private final String bitbucketRepo;

    private final String hash;

    public LastCommitInfo(String lastAuthor, String repoName, String bitbucketRepo, String hash) {
        this.lastAuthor = lastAuthor;
        this.repoName = repoName;
        this.bitbucketRepo = bitbucketRepo;
        this.hash = hash;
    }

    public String getLastAuthor() {
        return lastAuthor;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getBitbucketRepo() {
        return bitbucketRepo;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "LastCommitInfo{" +
                "lastAuthor='" + lastAuthor + '\'' +
                ", repoName='" + repoName + '\'' +
                ", bitbucketRepo='" + bitbucketRepo + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
