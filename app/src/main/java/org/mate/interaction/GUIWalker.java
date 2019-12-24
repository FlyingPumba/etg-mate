package org.mate.interaction;

import android.app.Instrumentation;
import androidx.test.uiautomator.UiDevice;

import org.mate.MATE;
import org.mate.exceptions.AUTCrashException;
import org.mate.exceptions.InvalidScreenStateException;
import org.mate.model.IGUIModel;
import org.mate.ui.Action;
import org.mate.state.IScreenState;
import org.mate.state.ScreenStateFactory;

import java.util.Vector;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * Created by marceloeler on 21/06/17.
 */

public class GUIWalker {

    private IGUIModel guiModelMgr;
    private String packageName;
    DeviceMgr deviceMgr;

    public GUIWalker(IGUIModel guiModel, String packageName, DeviceMgr deviceMgr){
        this.guiModelMgr = guiModel;
        this.packageName = packageName;
        Instrumentation instrumentation =  getInstrumentation();
        this.deviceMgr = deviceMgr;
    }

    private Action lastActionExecuted=null;



    private boolean checkStateReached(String targetScreenStateId) {
        IScreenState state = ScreenStateFactory.getScreenState("ActionsScreenState");
        try {
            guiModelMgr.moveToState(state);
        } catch (InvalidScreenStateException e) {
            e.printStackTrace();
            return false;
        }
        if (guiModelMgr.getCurrentStateId().equals(targetScreenStateId))
            return true;
        else
            return false;
    }

    public boolean goToState(String targetScreenStateId) {
        boolean stateFound=false;

        if (guiModelMgr.getCurrentStateId().equals(targetScreenStateId)) {
            return true;
        }

        MATE.log("Going from " + guiModelMgr.getCurrentStateId() +" to " + targetScreenStateId);
//        MATE.log("         >>> 1st attempt");
        Vector<Vector<Action>> paths = guiModelMgr.pathFromTo(guiModelMgr.getCurrentStateId(),targetScreenStateId);
        if (paths.size()>0){
            executePaths(paths, targetScreenStateId);
            stateFound = this.checkStateReached(targetScreenStateId);
            if (stateFound) {
                return true;
            }
        }

//        MATE.log("         >>> 2nd attempt - restart");
        deviceMgr.restartApp();
        //MATE.log("State detected after restart: " + guiModelMgr.detectCurrentState(packageName,ScreenStateFactory.getScreenState("ActionsScreenState")));
        stateFound = this.checkStateReached(targetScreenStateId);
        if (stateFound) {
            return true;
        }
        paths = guiModelMgr.pathFromTo(guiModelMgr.getCurrentStateId(),targetScreenStateId);
        if (paths.size()>0){
            executePaths(paths,targetScreenStateId);
            stateFound = this.checkStateReached(targetScreenStateId);
            if (stateFound) {
                return true;
            }
        }

//        MATE.log("         >>> 3rd attempt - reinstall");
        deviceMgr.reinstallApp();
        deviceMgr.restartApp();
        //MATE.log("State detected after restart: " + guiModelMgr.detectCurrentState(packageName,ScreenStateFactory.getScreenState("ActionsScreenState")));
        stateFound = this.checkStateReached(targetScreenStateId);
        if (stateFound) {
            return true;
        }
        paths = guiModelMgr.pathFromTo(guiModelMgr.getCurrentStateId(),targetScreenStateId);
        if (paths.size()>0){
            executePaths(paths,targetScreenStateId);
            stateFound = this.checkStateReached(targetScreenStateId);
            if (stateFound) {
                return true;
            }
            deviceMgr.restartApp();
            stateFound = this.checkStateReached(targetScreenStateId);
            if (stateFound) {
                return true;
            }
        }
        return stateFound;
    }


    private void executePaths(Vector<Vector<Action>> paths, String targetScreenStateId) {
        boolean desiredStateReached = false;
        for (int ev = 0; ev < paths.size() && !desiredStateReached; ev++) {
            Vector<Action> path = paths.get(ev);
            goToState(guiModelMgr.getCurrentStateId());
            executePath(path, targetScreenStateId);
            desiredStateReached = checkStateReached(targetScreenStateId);
        }
    }

    private void executePath(Vector<Action> path, String targetScreenStateId){

        boolean desiredStateReached=false;
        Vector<Action> actions = new Vector<Action>();
        if (!path.isEmpty()){
            boolean targetReached = false;
            for (int i=0; i< path.size() && !targetReached; i++){

                Action action = path.get(i);

                actions.add(action);

                try {
                    deviceMgr.executeAction(action);
                    targetReached = this.checkStateReached(targetScreenStateId);
                    lastActionExecuted=action;

                    if (action.getWidget().isEditable()){
                        for (Action act: action.getAdjActions())
                            deviceMgr.executeAction(act);
                    }

                } catch (AUTCrashException e) {
                    deviceMgr.handleCrashDialog();
                    i = path.size()+1; //exit loop
                }
            }
        }
    }
}
