package org.mate;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mate.accessibility.AccessibilitySummaryResults;
import org.mate.model.IGUIModel;

/**
 * Created by marceloeler on 11/07/17.
 */

@RunWith(AndroidJUnit4.class)
public class ExecuteMATEAccTestingRandom {


    @Test
    public void useAppContext() throws Exception {

        MATE.log("start testing acc");
        MATE mate = new MATE();

        //run mate for timeout minutes
        mate.testApp("AccRandom");

        String packageName = mate.getPackageName();
        IGUIModel guiModel = mate.getGuiModel();

        //produce summary - print to the log file
        if (guiModel!=null) {
            //MATE.logsum("STATES_VISITED_BY_MATE:"+guiModel.getStates().size());
            AccessibilitySummaryResults.printSummary(guiModel);
            //EnvironmentManager.deleteAllScreenShots(packageName);

        }
    }
}
