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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.sonar.api.internal.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public final class WildcardPatternFileScanner {

    private static final Logger LOG = Loggers.get(WildcardPatternFileScanner.class);

    private static final int SEARCH_MAX_DEPTH = 64;

    private static final String PATH_MATCHER_SPECIAL_CHAR = "*?";

    private WildcardPatternFileScanner() {
        // utility class
    }

    public static List<Path> scan(Path baseDirectory, String patternPath) {
        String unixLikePatternPath = toUnixLikePath(patternPath);
        int specialCharIndex = indexOfMatcherSpecialChar(unixLikePatternPath);
        if (specialCharIndex == -1) {
            return scanNonWildcardPattern(baseDirectory, unixLikePatternPath);
        } else {
            // For performance reason, we don't want to scan recursively all files in baseDirectory
            // when patternPath start with "none wildcard" subfolder names. For example,
            // scanWildcardPattern("/base", "sub1/sub2/**/file*.xml") is converted into
            // scanWildcardPattern("/base/sub1/sub2", "**/file*.xml")
            int additionalBaseDirectoryPart = unixLikePatternPath.lastIndexOf('/', specialCharIndex);
            if (additionalBaseDirectoryPart != -1) {
                Path additionalBaseDirectory = toFileSystemPath(unixLikePatternPath.substring(0, additionalBaseDirectoryPart + 1));
                String remainingWildcardPart = unixLikePatternPath.substring(additionalBaseDirectoryPart + 1);
                Path moreSpecificBaseDirectory = baseDirectory.resolve(additionalBaseDirectory);
                return scanWildcardPattern(moreSpecificBaseDirectory, remainingWildcardPart);
            } else {
                return scanWildcardPattern(baseDirectory, unixLikePatternPath);
            }
        }
    }

    private static List<Path> scanNonWildcardPattern(Path baseDirectory, String unixLikePath) {
        Path path = baseDirectory.resolve(toFileSystemPath(unixLikePath));
        if (Files.isRegularFile(path)) {
            return Collections.singletonList(path);
        }
        return Collections.emptyList();
    }

    private static List<Path> scanWildcardPattern(Path baseDirectory, String unixLikePatternPath) {
        if (!Files.exists(baseDirectory)) {
            return Collections.emptyList();
        }
        try {
            Path absoluteBaseDirectory = baseDirectory.toRealPath();
            if (absoluteBaseDirectory.equals(absoluteBaseDirectory.getRoot())) {
                throw new IOException("For performance reason, wildcard pattern search is not possible from filesystem root");
            }
            List<Path> paths = new ArrayList<>();
            WildcardPattern matcher = WildcardPattern.create(toUnixLikePath(absoluteBaseDirectory.toString()) + "/" + unixLikePatternPath);
            try (Stream<Path> stream = Files.walk(absoluteBaseDirectory, SEARCH_MAX_DEPTH)) {
                stream
                        .filter(Files::isRegularFile)
                        .filter(path -> matcher.match(toUnixLikePath(path.toString())))
                        .forEach(paths::add);
            }
            return paths;
        } catch (IOException | RuntimeException e) {
            LOG.warn("Failed to get Jacoco report paths: Scanning '" + baseDirectory + "' with pattern '" + unixLikePatternPath + "'" +
                    " threw a " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @VisibleForTesting
    static String toUnixLikePath(String path) {
        return path.indexOf('\\') != -1 ? path.replace('\\', '/') : path;
    }

    @VisibleForTesting
    static Path toFileSystemPath(String unixLikePath) {
        return Paths.get(unixLikePath.replace('/', File.separatorChar));
    }

    @VisibleForTesting
    static int indexOfMatcherSpecialChar(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (PATH_MATCHER_SPECIAL_CHAR.indexOf(path.charAt(i)) != -1) {
                return i;
            }
        }
        return -1;
    }

}
