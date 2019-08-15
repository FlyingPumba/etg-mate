/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mate.espresso.codegen;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.mate.espresso.TestCodeTemplate;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.interaction.DeviceMgr;
import org.mate.model.TestCase;
import org.mate.ui.Action;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class TestCodeGenerator {
  private final String TEST_CODE_TEMPLATE_FILE_NAME = "TestCodeTemplate.vm";

  private final String ESPRESSO_CUSTOM_PACKAGE = "com.google.android.apps.common.testing.ui";
  private final String ESPRESSO_STANDARD_PACKAGE = "android.support.test";

  private final DeviceMgr deviceMgr;
  private String packageName;

  public TestCodeGenerator(DeviceMgr deviceMgr, String packageName) {
    this.deviceMgr = deviceMgr;
    this.packageName = packageName;
  }

  public List<String> getEspressoTestCases(List<IChromosome<TestCase>> widgetTestCases) {
    List<String> espressoTestCases = new ArrayList<>();
    for (int i = 0; i < widgetTestCases.size(); i++) {
      IChromosome<TestCase> widgetTestCase = widgetTestCases.get(i);
      espressoTestCases.add(generateEspressoTestCase(widgetTestCase, i));
    }

    return espressoTestCases;
  }

  private String generateEspressoTestCase(IChromosome<TestCase> widgetTestCase, int testCaseIndex) {
    Writer writer = null;
    try {
      writer = new StringWriter();

      VelocityEngine velocityEngine = new VelocityEngine();
      // Suppress creation of velocity.log file.
      velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogChute");
      velocityEngine.init();
      VelocityContext velocityContext = createVelocityContext(widgetTestCase, testCaseIndex);
      velocityEngine.evaluate(velocityContext, writer, "mystring", TestCodeTemplate.getTemplate());
      writer.flush();

      return writer.toString();

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate test class file: ", e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        }
        catch (Exception e) {
          // ignore
        }
      }
    }
  }

  private VelocityContext createVelocityContext(IChromosome<TestCase> widgetTestCase, int testCaseIndex) {
    VelocityContext velocityContext = new VelocityContext();

    Object[] visitedActivities = widgetTestCase.getValue().getVisitedActivities().toArray();
    String[] activityName = visitedActivities[0].toString().split("/");
    velocityContext.put("TestActivityName", packageName + activityName[1]);

    velocityContext.put("PackageName", packageName);
    velocityContext.put("ResourcePackageName", packageName);

    // TODO: improve test name based on TestCase's visitedActivities
    velocityContext.put("ClassName", String.format("TestCase%d", testCaseIndex));
    velocityContext.put("TestMethodName", "myTestCase");

    velocityContext.put("EspressoPackageName", false ? ESPRESSO_CUSTOM_PACKAGE : ESPRESSO_STANDARD_PACKAGE);

    TestCodeMapper codeMapper = new TestCodeMapper(deviceMgr);
    List<String> testCodeLines = new ArrayList<>();
    Vector<Action> actions = widgetTestCase.getValue().getEventSequence();
    for (Action action : actions) {
      testCodeLines.addAll(codeMapper.getTestCodeLinesForAction(action));
    }

    velocityContext.put("AddContribImport", codeMapper.isRecyclerViewActionAdded());
    velocityContext.put("AddChildAtPositionMethod", codeMapper.isChildAtPositionAdded());
    velocityContext.put("TestCode", testCodeLines);

    return velocityContext;
  }
}
