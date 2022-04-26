/*
 * Copyright (c) 2022 KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.bundle.junit_runner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.knopflerfish.bundle.junit_runner.Grunt.log;

public class MissingTestFinder {
  static final String EXPECTED_SUITES_FILE = "init-tests-expected-suites";

  static void includeMissingSuites(PrintWriter masterIndexPW, List<String> testRuns, File outDir) {
    final List<String> resultTestSuites = getResultTestSuites(testRuns, outDir);
    log("resultTestSuites: " + resultTestSuites);
    final List<String> expectedTestSuites = getExpectedTestSuites();
    log("expectedTestSuites: " + expectedTestSuites);

    for (String expectedTestSuite : expectedTestSuites) {
      if (!resultTestSuites.contains(expectedTestSuite)) {
        log("Missing suite: " + expectedTestSuite);
        masterIndexPW.println("<junit_run name=\"missing suites\" description=\"suite result not found\">");
        masterIndexPW.println("<junit date=\"" + new Date() + "\">");
        masterIndexPW.println("<testcase id=\"" + expectedTestSuite + "\">");
        masterIndexPW.println("<description><![CDATA[" + expectedTestSuite + " has no result file.]]></description>");
        masterIndexPW.println("<testresult wasSuccessful = \"false\" runCount = \"0\" failureCount = \"0\" errorCount = \"0\"></testresult>");
        masterIndexPW.println("</testcase>");
        masterIndexPW.println("</junit>");
        masterIndexPW.println("</junit_run>");
      }
    }
  }

  private static List<String> getExpectedTestSuites() {
    Path path = Paths.get(EXPECTED_SUITES_FILE);
    try {
      return Files.lines(path)
          .filter(className -> className.length() > 0)
          .collect(Collectors.toList());
    } catch (IOException e) {
      log("Failed to read expected test suites file " + path);
      return Collections.emptyList();
    }
  }

  private static List<String> getResultTestSuites(List<String> testRuns, File outDir) {
    final Path path = outDir.toPath();
    return testRuns.stream()
        .map(testRun -> findSuiteClassesForTestRun(path, testRun))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private static List<String> findSuiteClassesForTestRun(Path path, String testRun) {
    try {
      return Files.list(path)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(fileName -> isTestSuiteResultFile(fileName, testRun))
          .map(MissingTestFinder::extractSuiteClassName)
          .collect(Collectors.toList());
    } catch (IOException e) {
      log("Failed to extract suite classes for run " + testRun);
      return Collections.emptyList();
    }
  }

  private static boolean isTestSuiteResultFile(String fileName, String testRun) {
    return fileName.startsWith(testRun + "-") && fileName.endsWith("TestSuite.xml");
  }

  private static String extractSuiteClassName(String fileName) {
    int classNameStart = fileName.lastIndexOf("-") + 1;
    int classNameEnd = fileName.indexOf(".xml", classNameStart);
    return fileName.substring(classNameStart, classNameEnd);
  }

}
