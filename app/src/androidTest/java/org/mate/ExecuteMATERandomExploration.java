package org.mate;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExecuteMATERandomExploration {


    @Test
    public void useAppContext() throws Exception {

        MATE.log_acc("Starting Random Exploration...");

        MATE mate = new MATE();
        mate.testApp("RandomExploration");
    }
}
