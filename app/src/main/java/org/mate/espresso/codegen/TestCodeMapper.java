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

import org.mate.espresso.util.Pair;
import org.mate.interaction.DeviceMgr;
import org.mate.ui.Action;
import org.mate.ui.ActionType;
import org.mate.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mate.espresso.codegen.MatcherBuilder.Kind.ClassName;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.ContentDescription;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.Id;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.Text;
import static org.mate.espresso.util.StringHelper.isNullOrEmpty;
import static org.mate.espresso.util.StringHelper.parseId;

public class TestCodeMapper {

  private static final int MAX_HIERARCHY_VIEW_LEVEL = 2;
  private static final String VIEW_VARIABLE_CLASS_NAME = "ViewInteraction";
  private static final String CLASS_VIEW_PAGER = "android.support.v4.view.ViewPager";

  private final String mApplicationId = "";

  //  private final boolean mIsUsingCustomEspresso;
  private boolean mIsChildAtPositionAdded = false;
  private boolean mIsRecyclerViewActionAdded;

  /**
   * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
   */
  private final Map<String, Integer> mVariableNameIndexes = new HashMap<>();
  private final DeviceMgr deviceMgr;
  private boolean USE_TEXT_FOR_ELEMENT_MATCHING = false;

  public TestCodeMapper(DeviceMgr deviceMgr) {
    this.deviceMgr = deviceMgr;
  }

  public List<String> getTestCodeLinesForAction(Action action) {
    List<String> testCodeLines = new ArrayList<>();

    if (action.getActionType() == ActionType.BACK) {
      testCodeLines.add("pressBack();");
      return testCodeLines;
    }
    else if (action.getActionType() == ActionType.MENU) {
      testCodeLines.add("onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_MENU));");
        //testCodeLines.add("openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());");
      return testCodeLines;
    }

//        if (event.isDelayedMessagePost()) {
//            testCodeLines.add(createSleepStatement(event.getDelayTime()));
//            return testCodeLines;
//        }

    String variableName = addViewPickingStatement(action, testCodeLines);
    if (action.getActionType() == ActionType.SWIPE_DOWN) {
      testCodeLines.add(createActionStatement(variableName, "swipeDown()", false));
    }
    else if (action.getActionType() == ActionType.SWIPE_UP) {
      testCodeLines.add(createActionStatement(variableName, "swipeUp()", false));
    }
    else if (action.getActionType() == ActionType.SWIPE_RIGHT) {
      testCodeLines.add(createActionStatement(variableName, "swipeRight()", false));
    }
    else if (action.getActionType() == ActionType.SWIPE_LEFT) {
      testCodeLines.add(createActionStatement(variableName, "swipeLeft()", false));
    }

//        else if (event.isPressEditorAction()) {
//            // TODO: If this is the same element that was just edited, consider reusing the same view interaction (i.e., variable name).
//            testCodeLines.add(createActionStatement(variableName, "pressImeActionButton()", false));
//        } else

    else if (action.getActionType() == ActionType.CLICK) {
      testCodeLines.add(createActionStatement(variableName, "click()", false));
    }
    else if (action.getActionType() == ActionType.LONG_CLICK) {
      testCodeLines.add(createActionStatement(variableName, "longClick()", false));
    }
    else if (action.getActionType() == ActionType.TYPE_TEXT) {
//            if (mIsUsingCustomEspresso) {
//                testCodeLines.add(createActionStatement(variableName, "clearText()", false));
//                testCodeLines.add(createActionStatement(
//                        variableName, "typeText(" + boxString(event.getReplacementText()) + "), closeSoftKeyboard()", false));
//            } else {
      testCodeLines.add(createActionStatement(
              variableName, "replaceText(" + deviceMgr.generateTextData(action) + "), closeSoftKeyboard()", false));
//            }
    }
    else if (action.getActionType() == ActionType.ENTER) {
      // do nothing, since this is handled solely by the Espresso "replaceText" command.
    }
    else {
      throw new RuntimeException("Unsupported event type: " + action.getActionType());
    }

    return testCodeLines;
  }

  private String addViewPickingStatement(Action action, List<String> testCodeLines) {
    String variableName = generateVariableNameFromElementClassName(action.getWidget().getClazz());
    testCodeLines.add(VIEW_VARIABLE_CLASS_NAME + " " + variableName + " = onView(\n" + generateElementHierarchyConditions(action) + ");");
    return variableName;
  }

  private String generateVariableNameFromElementClassName(String elementClassName) {
    if (elementClassName == null || elementClassName.isEmpty()) {
      return generateVariableNameFromTemplate(VIEW_VARIABLE_CLASS_NAME);
    }
    return generateVariableNameFromTemplate(elementClassName);
  }


  private String generateVariableNameFromTemplate(String template) {
    String variableName = Character.toLowerCase(template.charAt(0)) + template.substring(1);
//        if (JavaLexer.isKeyword(variableName, LanguageLevel.HIGHEST)) {
//            variableName += "_";
//        }

    Integer unusedIndex = mVariableNameIndexes.get(variableName);
    if (unusedIndex == null) {
      mVariableNameIndexes.put(variableName, 2);
      return variableName;
    }

    mVariableNameIndexes.put(variableName, unusedIndex + 1);
    return variableName + unusedIndex;
  }

  private String generateElementHierarchyConditions(Action action) {
    return generateElementHierarchyConditionsRecursively(action.getWidget(), 0);
  }

  private String generateElementHierarchyConditionsRecursively(Widget widget, int index) {
    // Add isDisplayed() only to the innermost element.
    boolean checkIsDisplayed = !widget.isScrollable();
    boolean addIsDisplayed = checkIsDisplayed && index == 0;
    MatcherBuilder matcherBuilder = new MatcherBuilder();

    if (isEmpty(widget)
            // Cannot use child position for the last element, since no parent descriptor available.
            || widget.getParent() == null && isEmptyIgnoringChildren(widget)
            || index == 0 && isLoginRadioButton(widget)) {
      matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
    } else {
      // Do not use android framework ids that are not visible to the compiler.
      String resourceId = widget.getResourceID();
      if (isAndroidFrameworkPrivateId(resourceId)) {
        matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
      } else {
        matcherBuilder.addMatcher(Id, convertIdToTestCodeFormat(resourceId), false, false);
      }

      if (USE_TEXT_FOR_ELEMENT_MATCHING) {
        matcherBuilder.addMatcher(Text, widget.getText(), true, false);
      }

      matcherBuilder.addMatcher(ContentDescription, widget.getContentDesc(), true, false);
    }

    // TODO: Consider minimizing the generated statement to improve test's readability and maintainability (e.g., by capping parent hierarchy).

    // The last element has no parent.
    if (widget.getParent() == null || index > MAX_HIERARCHY_VIEW_LEVEL) {
      if (matcherBuilder.getMatcherCount() > 1 || addIsDisplayed) {
        return "allOf(" + matcherBuilder.getMatchers() + (addIsDisplayed ? ", isDisplayed()" : "") + ")";
      }
      return matcherBuilder.getMatchers();
    }

    boolean addAllOf = matcherBuilder.getMatcherCount() > 0 || addIsDisplayed;
    int childPosition = widget.getChildPositionInParent();

    // Do not use child position for ViewPager children as it changes dynamically and non-deterministically.
    if (CLASS_VIEW_PAGER.equals(widget.getParent().getClazz())) {
      childPosition = -1;
    }

    mIsChildAtPositionAdded = mIsChildAtPositionAdded || childPosition != -1;

    return (addAllOf ? "allOf(" : "") + matcherBuilder.getMatchers() + (matcherBuilder.getMatcherCount() > 0 ? ",\n" : "")
            + (childPosition != -1 ? "childAtPosition(\n" : "withParent(")
            + generateElementHierarchyConditionsRecursively(widget.getParent(), index + 1)
            + (childPosition != -1 ? ",\n" + childPosition : "") + ")"
            + (addIsDisplayed ? ",\nisDisplayed()" : "") + (addAllOf ? ")" : "");
  }

  private boolean isAndroidFrameworkPrivateId(String resourceId) {
    Pair<String, String> parsedId = parseId(resourceId);
    return parsedId != null && "android".equals(parsedId.getFirst());
  }

  private String convertIdToTestCodeFormat(String resourceId) {
    Pair<String, String> parsedId = parseId(resourceId);

    if (parsedId == null) {
      // Parsing failed, return the raw id.
      return resourceId;
    }

    String testCodeId = "R.id." + parsedId.getSecond();
//    if (!parsedId.getFirst().equals(mApplicationId)) {
//      // Only the app's resource package will be explicitly imported, so use a fully qualified id for other packages.
//      testCodeId = parsedId.getFirst() + "." + testCodeId;
//    }

    return testCodeId;
  }

  private String createActionStatement(String variableName, String action, boolean addScrollTo) {
    return variableName + ".perform(" + (addScrollTo ? "scrollTo(), " : "") + action + ");";
  }

  /**
   * TODO: This is a temporary workaround for picking a login option in a username-agnostic way
   * such that the generated test is generic enough to run on other devices.
   * TODO: Also, it assumes a single radio button choice (such that it could be identified by the class name).
   */
  private boolean isLoginRadioButton(Widget widget) {
    return widget.getClazz().endsWith(".widget.AppCompatRadioButton")
            && "R.id.welcome_account_list".equals(convertIdToTestCodeFormat(widget.getParent().getResourceID()));
  }

  public boolean isEmptyIgnoringChildren(Widget widget) {
    return isNullOrEmpty(widget.getResourceID()) && isNullOrEmpty(widget.getText())
            && isNullOrEmpty(widget.getContentDesc());
  }

  public boolean isEmpty(Widget widget) {
    return widget.getChildPositionInParent() == -1 && isEmptyIgnoringChildren(widget);
  }

  public boolean isChildAtPositionAdded() {
    return mIsChildAtPositionAdded;
  }
}
