package org.mate.espresso;

public class TestCodeTemplate {

    public static String getTemplate() {
        return "#if (${PackageName} && ${PackageName} != \"\")\n" +
                "package ${PackageName};\n" +
                "\n" +
                "#end\n" +
                "\n" +
                "import ${EspressoPackageName}.espresso.ViewInteraction;\n" +
                "import android.support.test.rule.ActivityTestRule;\n" +
                "import android.support.test.runner.AndroidJUnit4;\n" +
                "import android.support.test.filters.LargeTest;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import android.view.ViewParent;\n" +
                "\n" +
                "import static ${EspressoPackageName}.InstrumentationRegistry.getInstrumentation;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.onView;\n" +
                "import static ${EspressoPackageName}.espresso.Espresso.openActionBarOverflowOrOptionsMenu;\n" +
                "#if (${AddContribImport})\n" +
                "import static ${EspressoPackageName}.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;\n" +
                "#end\n" +
                "import static ${EspressoPackageName}.espresso.action.ViewActions.*;\n" +
                "import static ${EspressoPackageName}.espresso.assertion.ViewAssertions.*;\n" +
                "import static ${EspressoPackageName}.espresso.matcher.ViewMatchers.*;\n" +
                "\n" +
                "import ${ResourcePackageName}.R;\n" +
                "\n" +
                "import org.hamcrest.Description;\n" +
                "import org.hamcrest.Matcher;\n" +
                "import org.hamcrest.TypeSafeMatcher;\n" +
                "import org.hamcrest.core.IsInstanceOf;\n" +
                "import org.junit.Rule;\n" +
                "import org.junit.Test;\n" +
                "import org.junit.runner.RunWith;\n" +
                "\n" +
                "import static org.hamcrest.Matchers.allOf;\n" +
                "import static org.hamcrest.Matchers.is;\n" +
                "\n" +
                "@LargeTest\n" +
                "@RunWith(AndroidJUnit4.class)\n" +
                "public class ${ClassName} {\n" +
                "\n" +
                "    @Rule\n" +
                "    public ActivityTestRule<${TestActivityName}> mActivityTestRule = new ActivityTestRule<>(${TestActivityName}.class);\n" +
                "\n" +
                "    @Test\n" +
                "    public void ${TestMethodName}() {\n" +
                "    #foreach (${line} in ${TestCode})\n" +
                "    ${line}\n" +
                "    #end\n" +
                "    }\n" +
                "\n" +
                "    #if (${AddChildAtPositionMethod})\n" +
                "    private static Matcher<View> childAtPosition(\n" +
                "            final Matcher<View> parentMatcher, final int position) {\n" +
                "\n" +
                "        return new TypeSafeMatcher<View>() {\n" +
                "            @Override\n" +
                "            public void describeTo(Description description) {\n" +
                "                description.appendText(\"Child at position \" + position + \" in parent \");\n" +
                "                parentMatcher.describeTo(description);\n" +
                "            }\n" +
                "\n" +
                "            @Override\n" +
                "            public boolean matchesSafely(View view) {\n" +
                "                ViewParent parent = view.getParent();\n" +
                "                return parent instanceof ViewGroup && parentMatcher.matches(parent)\n" +
                "                        && view.equals(((ViewGroup)parent).getChildAt(position));\n" +
                "            }\n" +
                "        };\n" +
                "    }\n" +
                "    #end\n" +
                "}";
    }
}
