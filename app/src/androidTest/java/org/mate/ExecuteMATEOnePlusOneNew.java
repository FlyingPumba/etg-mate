package org.mate;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExecuteMATEOnePlusOneNew {


    @Test
    public void useAppContext() throws Exception {

        MATE.log_acc("Starting Evolutionary Search...");
        MATE.log_acc("One-plus-one (new) algorithm");

        MATE mate = new MATE();
        mate.testApp("OnePlusOneNew");

        //Report
        //Vector<TestCase> ts = new Vector<>(OnePlusOne.testsuite.values());
        //MATE.log_acc("Final Report: test cases number = "+ts.size());

        //MATE.log_acc(OnePlusOne.coverageArchive.keySet().toString());
        //MATE.log_acc("Visited GUI States number = "+ OnePlusOne.coverageArchive.keySet().size());
        //MATE.log_acc("Covered GUI States = "+ OnePlusOne.testsuite.get("0").getVisitedStates().size());


    }
}
