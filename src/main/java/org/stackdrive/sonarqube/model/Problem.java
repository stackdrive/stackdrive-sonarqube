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
package org.stackdrive.sonarqube.model;

/**
 * Model of problem
 */
public class Problem {

    /**
     * Canonical element reference from root of project to semantical significant element
     */
    private String element;

    /**
     * Status of problem
     */
    private Status status;

    /**
     * Severety of problem
     */
    private Severety severety;

    /**
     * Problem definition: code and description
     */
    private String message;

    public Problem (String element, Status status, Severety severety, String message) {
        this.element = element;
        this.status = status;
        this.severety = severety;
        this.message = message;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Severety getSeverety() {
        return severety;
    }

    public void setSeverety(Severety severety) {
        this.severety = severety;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isProjectProblem () {
        if (this.element.contains("project")) {
            return true;
        }
        return false;
    }

    /**
     * Returns element class
     * @return
     */
    public String clazz () {
        if (this.element.contains("#")) {
            return this.element.substring(0, this.element.indexOf('#'));
        }
        return "project";
    }

    /**
     * Returns line
     * @return
     */
    public int line () {
        if (this.element.contains("#")) {
            return Integer.parseInt(this.element.substring(this.element.indexOf('#') + 1));
        }
        return 1;
    }
}