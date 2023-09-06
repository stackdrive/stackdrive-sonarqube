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

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class XmlReportParser {

    private final Path xmlReportPath;

    private static final String COLUMN = " column ";

    public XmlReportParser(Path xmlReportPath) {
        this.xmlReportPath = xmlReportPath;
    }

    public List<Counter> parse() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLStreamReader xmlStreamReaderParser = null;
        try (Reader reader = Files.newBufferedReader(xmlReportPath, StandardCharsets.UTF_8)) {
            xmlStreamReaderParser = factory.createXMLStreamReader(reader);
            // Need to be effectively final to be used in Supplier lambdas
            final XMLStreamReader parser = xmlStreamReaderParser;

            List<Counter> counters = new ArrayList<>();

            String packageName = null;

            while (true) {
                int event = parser.next();

                if (event == XMLStreamConstants.END_DOCUMENT) {
                    parser.close();
                    break;
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String element = parser.getLocalName();
                    if (element.equals("package")) {
                        packageName = null;
                    }
                } else if (event == XMLStreamConstants.START_ELEMENT) {
                    String element = parser.getLocalName();

                    if (element.equals("package")) {
                        packageName = getStringAttr(parser, "name", () -> "for a 'package' at line " + parser.getLocation().getLineNumber() + COLUMN + parser.getLocation().getColumnNumber());
                    } else if (element.equals("counter") && packageName == null) {

                        Supplier<String> errorCtx = () -> "for the sourcefile '" + xmlReportPath.getFileName() + "' at line "
                                + parser.getLocation().getLineNumber() + COLUMN + parser.getLocation().getColumnNumber();

                        Counter counter = new Counter(
                                getStringAttr(parser, "type", errorCtx),
                                getIntAttr(parser, "missed", errorCtx),
                                getIntAttr(parser, "covered", errorCtx));
                        counters.add(counter);
                    }
                }
            }

            return counters;
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException("Failed to parse JaCoCo XML report: " + xmlReportPath.toAbsolutePath(), e);
        } finally {
            if (xmlStreamReaderParser != null) {
                try {
                    xmlStreamReaderParser.close();
                } catch (XMLStreamException e) {
                    // do nothing - the stream used to read from will be closed by the try-with-resource
                }
            }
        }
    }

    private static String getStringAttr(XMLStreamReader parser, String name, Supplier<String> errorContext) {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            throw new IllegalStateException("Invalid report: couldn't find the attribute '" + name + "' " + errorContext.get());
        }
        return value;
    }

    private static int getOptionalIntAttr(XMLStreamReader parser, String name, Supplier<String> errorContext) {
        String value = parser.getAttributeValue(null, name);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid report: failed to parse integer from the attribute '" + name + "' " + errorContext.get());
        }
    }

    private static int getIntAttr(XMLStreamReader parser, String name, Supplier<String> errorContext) {
        String value = getStringAttr(parser, name, errorContext);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid report: failed to parse integer from the attribute '" + name + "' " + errorContext.get());
        }
    }

    static class Counter {
        private String type;
        private int missedLines;
        private int coveredLines;

        public Counter(String type, int missedLines, int coveredLines) {
            this.type = type;
            this.missedLines = missedLines;
            this.coveredLines = coveredLines;
        }

        public String getType() {
            return type;
        }

        public int getMissedLines() {
            return missedLines;
        }

        public int getCoveredLines() {
            return coveredLines;
        }
    }
}