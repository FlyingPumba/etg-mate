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

import org.mate.MATE;
import org.mate.espresso.util.Pair;
import org.mate.interaction.DeviceMgr;
import org.mate.ui.Action;
import org.mate.ui.ActionType;
import org.mate.ui.Widget;
import org.mate.utils.Randomness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mate.espresso.codegen.MatcherBuilder.Kind.ClassName;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.ContentDescription;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.Id;
import static org.mate.espresso.codegen.MatcherBuilder.Kind.Text;
import static org.mate.espresso.util.StringHelper.boxString;
import static org.mate.espresso.util.StringHelper.isNullOrEmpty;
import static org.mate.espresso.util.StringHelper.parseId;

public class TestCodeMapper {

  private static final int MAX_HIERARCHY_VIEW_LEVEL = 2;
  private static final String VIEW_VARIABLE_CLASS_NAME = "ViewInteraction";
  private static final String DATA_VARIABLE_CLASS_NAME = "DataInteraction";
  private static final String CLASS_VIEW_PAGER = "android.support.v4.view.ViewPager";

  private boolean mIsChildAtPositionAdded = false;
  private boolean mIsRecyclerViewActionAdded = false;
  private boolean mIsclassOrSuperClassesNameAdded = false;
  private boolean mIsKotlinTestClass = false;
  private boolean mUseTextForElementMatching = true;

  /**
   * Map of variable_name -> first_unused_index. This map is used to ensure that variable names are unique.
   */
  private final Map<String, Integer> mVariableNameIndexes = new HashMap<>();
  private final DeviceMgr deviceMgr;

  public TestCodeMapper(DeviceMgr deviceMgr) {
    this.deviceMgr = deviceMgr;
  }

  public List<String> getTestCodeLinesForAction(Action action) {
    List<String> testCodeLines = new ArrayList<>();

    if (action.getActionType() == ActionType.BACK) {
      testCodeLines.add("pressBackUnconditionally()" + getStatementTerminator());
      return testCodeLines;
    }
    else if (action.getActionType() == ActionType.MENU) {
      //testCodeLines.add("openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())" + getStatementTerminator());
      testCodeLines.add("onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_MENU));");
      return testCodeLines;
    }

//        if (event.isDelayedMessagePost()) {
//            testCodeLines.add(createSleepStatement(event.getDelayTime()));
//            return testCodeLines;
//        }

    String variableName = addPickingStatement(action, testCodeLines);
    int recyclerViewChildPosition = action.getWidget().getRecyclerViewChildPosition();

    if (action.getActionType() == ActionType.SWIPE_DOWN) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "swipeDown()", action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.SWIPE_UP) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "swipeUp()", action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.SWIPE_RIGHT) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "swipeRight()", action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.SWIPE_LEFT) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "swipeLeft()", action.getWidget().isScrollable()));
    }

//        else if (event.isPressEditorAction()) {
//            // TODO: If this is the same element that was just edited, consider reusing the same view interaction (i.e., variable name).
//            testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "pressImeActionButton()", false));
//        } else

    else if (action.getActionType() == ActionType.CLICK) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "click()", action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.LONG_CLICK) {
      testCodeLines.add(createActionStatement(variableName, recyclerViewChildPosition, "longClick()", action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.TYPE_TEXT) {
      String closeSoftKeyboardAction = doesNeedStandaloneCloseSoftKeyboardAction(action) ? "" : ", closeSoftKeyboard()";
      testCodeLines.add(createActionStatement(
              variableName, recyclerViewChildPosition, "replaceText(" + boxString(action.getExtraInfo()) + ")" + closeSoftKeyboardAction, action.getWidget().isScrollable()));
    }
    else if (action.getActionType() == ActionType.ENTER) {
      // do nothing, since this is handled solely by the Espresso "replaceText" command.
    }
    else {
      throw new RuntimeException("Unsupported event type: " + action.getActionType());
    }

    if (doesNeedStandaloneCloseSoftKeyboardAction(action)) {
      addStandaloneCloseSoftKeyboardAction(action, testCodeLines);
    }

    return testCodeLines;
  }

  private void addStandaloneCloseSoftKeyboardAction(Action action, List<String> testCodeLines) {
    // Simulate an artificial close soft keyboard event.
    Action closeSoftKeyboardAction = new Action(action.getWidget(), action.getActionType());

    testCodeLines.add("");
    String variableName = addPickingStatement(closeSoftKeyboardAction, testCodeLines);
    testCodeLines.add(createActionStatement(variableName, closeSoftKeyboardAction.getWidget().getRecyclerViewChildPosition(), "closeSoftKeyboard()", false));
  }

  private boolean doesNeedStandaloneCloseSoftKeyboardAction(Action action) {
    // Make text edit in a RecyclerView child always require a standalone close soft keyboard action since actionOnItemAtPosition
    // accepts only a single action.
    return mUseTextForElementMatching && action.getActionType() == ActionType.TYPE_TEXT
            && (!isNullOrEmpty(action.getExtraInfo()) || action.getWidget().getRecyclerViewChildPosition() != -1);
  }

  private String addPickingStatement(Action action, List<String> testCodeLines) {
    if (isAdapterViewAction(action)) {
      return addDataPickingStatement(action, testCodeLines);
    }
    String statement = addViewPickingStatement(action, testCodeLines);

    if (!statement.contains("R.id") ||
            !statement.contains("withContentDescription") ||
            !statement.contains("withText")) {
      // this view pick statement is too unspecific, try to start the creation of the statement from the lowest children

      int actionType = action.getActionType();
      Widget target = action.getWidget();

      while (target.getChildren().size() > 0) {
        target = Randomness.randomElement(target.getChildren());
      }

      // remove from testCodeLines the view picking statement
      testCodeLines.remove(testCodeLines.size() - 1);

      // regenerate statement
      statement = addViewPickingStatement(new Action(target, actionType), testCodeLines);
    }

    return statement;
  }

  private String addDataPickingStatement(Action action, List<String> testCodeLines) {
    String variableName = generateVariableNameFromElementClassName(action.getWidget().getClazz(), DATA_VARIABLE_CLASS_NAME);
    // TODO: Add '.onChildView(...)' when we support AdapterView beyond the immediate parent of the affected element.
    testCodeLines.add(getVariableTypeDeclaration(false) + " " + variableName + " = onData(anything())\n.inAdapterView(" +
            generateElementHierarchyConditions(action, 1) + ")\n.atPosition(" + action.getWidget().getAdapterViewChildPosition() +
            ")" + getStatementTerminator());
    return variableName;
  }

  private String getVariableTypeDeclaration(boolean isOnViewInteraction) {
    if (mIsKotlinTestClass) {
      return "val";
    }
    return isOnViewInteraction ? VIEW_VARIABLE_CLASS_NAME : DATA_VARIABLE_CLASS_NAME;
  }

  // TODO: This will not detect an adapter view action if the affected element's immediate parent is not an AdapterView
  // (e.g., clicking on a button, whose parent's parent is AdapterView will not be detected as an AdapterView action).
  private static boolean isAdapterViewAction(Action action) {
    return action.getWidget().getAdapterViewChildPosition() != -1 && action.getWidget().getParent() != null;
  }

  private String addViewPickingStatement(Action action, List<String> testCodeLines) {
    // Skip a level for RecyclerView children as they will be identified through their position.
    int startIndex = action.getWidget().getRecyclerViewChildPosition() != -1 && action.getWidget().getParent() != null ? 1 : 0;

    String variableClassName = startIndex == 0 ? action.getWidget().getClazz() : action.getWidget().getParent().getClazz();
    String variableName = generateVariableNameFromElementClassName(variableClassName, VIEW_VARIABLE_CLASS_NAME);

    String viewMatchers = generateElementHierarchyConditions(action, startIndex);

    if ("isDisplayed()".equals(viewMatchers)) {
      // this means that the action has an empty widget as a target
      viewMatchers = "isRoot()";
    }

    testCodeLines.add(getVariableTypeDeclaration(true) + " " + variableName + " = onView(\n" +
            viewMatchers + ")" + getStatementTerminator());

    return variableName;
  }

  private String generateVariableNameFromElementClassName(String elementClassName, String defaultClassName) {
    if (isNullOrEmpty(elementClassName)) {
      return generateVariableNameFromTemplate(defaultClassName);
    }
    return generateVariableNameFromTemplate(elementClassName);
  }

  private String generateVariableNameFromTemplate(String template) {
    template = template.replace(".", "_");
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

  private String generateElementHierarchyConditions(Action action, int startIndex) {
    // remove widgets in the hierarchy until we reach the desired index
    Widget widget = action.getWidget();
    while (startIndex > 0) {
      widget = widget.getParent();
      startIndex--;

      // the widget hierarchy is not as deep as the desired startIndex
      if (widget == null) {
        return "UNKNOWN";
      }
    }
    return generateElementHierarchyConditionsRecursively(widget, !widget.isScrollable(), startIndex);
  }

  private String generateElementHierarchyConditionsRecursively(Widget widget, boolean checkIsDisplayed, int index) {
    // Add isDisplayed() only to the innermost element.
    boolean addIsDisplayed = checkIsDisplayed && index == 0;
    MatcherBuilder matcherBuilder = new MatcherBuilder();

    if (isEmpty(widget)
            // Cannot use child position for the last element, since no parent descriptor available.
            || widget.getParent() == null && isEmptyIgnoringChildPosition(widget)
            || index == 0 && isLoginRadioButton(widget)) {
      matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
      mIsclassOrSuperClassesNameAdded = true;
    } else {
      // Do not use android framework ids that are not visible to the compiler.
      String resourceId = widget.getResourceID();
      if (isAndroidFrameworkPrivateId(resourceId)) {
        matcherBuilder.addMatcher(ClassName, widget.getClazz(), true, false);
        mIsclassOrSuperClassesNameAdded = true;
      } else {
        matcherBuilder.addMatcher(Id, convertIdToTestCodeFormat(resourceId), false, false);
      }

      if (mUseTextForElementMatching) {
        matcherBuilder.addMatcher(Text, widget.getText(), true, false);
      }

      matcherBuilder.addMatcher(ContentDescription, widget.getContentDesc(), true, false);
    }

    // TODO: Consider minimizing the generated statement to improve test's readability and maintainability (e.g., by capping parent hierarchy).

    // The last element has no parent.
    if (widget.getParent() == null || index > MAX_HIERARCHY_VIEW_LEVEL) {
      if (matcherBuilder.getMatcherCount() > 1 || addIsDisplayed) {
        String matchers = matcherBuilder.getMatchers();
        if (!matchers.isEmpty()) {
          return "allOf(" + matchers + (addIsDisplayed ? ", isDisplayed()" : "") + ")";
        } else {
          return addIsDisplayed ? "isDisplayed()" : "";
        }
      }
      return matcherBuilder.getMatchers();
    }

    boolean addAllOf = matcherBuilder.getMatcherCount() > 0 || addIsDisplayed;
    int groupViewChildPosition = widget.getGroupViewChildPosition();

    // Do not use child position for ViewPager children as it changes dynamically and non-deterministically.
    if (CLASS_VIEW_PAGER.equals(widget.getParent().getClazz())) {
      groupViewChildPosition = -1;
    }

    mIsChildAtPositionAdded = mIsChildAtPositionAdded || groupViewChildPosition != -1;

    return (addAllOf ? "allOf(" : "") + matcherBuilder.getMatchers() + (matcherBuilder.getMatcherCount() > 0 ? ",\n" : "")
            + (groupViewChildPosition != -1 ? "childAtPosition(\n" : "withParent(")
            + generateElementHierarchyConditionsRecursively(widget.getParent(), checkIsDisplayed, index + 1)
            + (groupViewChildPosition != -1 ? ",\n" + groupViewChildPosition : "") + ")"
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

  private String createActionStatement(String variableName, int recyclerViewChildPosition, String action, boolean addScrollTo) {
    mIsRecyclerViewActionAdded = mIsRecyclerViewActionAdded || recyclerViewChildPosition != -1;

    // No need to explicitly scroll to perform an action on a RecyclerView child.
    String completeAction = (addScrollTo && recyclerViewChildPosition == -1 ? "scrollTo(), " : "") + action;
    completeAction = recyclerViewChildPosition == -1
            ? completeAction
            : getActionOnItemAtPositionMethodCallPrefix() + recyclerViewChildPosition + ", " + completeAction + ")";

    return variableName + ".perform(" + completeAction + ")" + getStatementTerminator();
  }

  private String getActionOnItemAtPositionMethodCallPrefix() {
    return mIsKotlinTestClass ? "actionOnItemAtPosition<ViewHolder>(" : "actionOnItemAtPosition(";
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

  public boolean isEmptyIgnoringChildPosition(Widget widget) {
    return isNullOrEmpty(widget.getResourceID()) && isNullOrEmpty(widget.getText())
            && isNullOrEmpty(widget.getContentDesc());
  }

  public boolean isEmpty(Widget widget) {
    return widget.getRecyclerViewChildPosition() == -1 && widget.getAdapterViewChildPosition() == -1 && widget.getGroupViewChildPosition() == -1
            && isEmptyIgnoringChildPosition(widget);
  }

  public boolean isChildAtPositionAdded() {
    return mIsChildAtPositionAdded;
  }

  public boolean isRecyclerViewActionAdded() {
    return mIsRecyclerViewActionAdded;
  }

  private String getStatementTerminator() {
    return mIsKotlinTestClass ? "" : ";";
  }

  public boolean isClassOrSuperClassesNameAdded() {
    return mIsclassOrSuperClassesNameAdded;
  }
}
