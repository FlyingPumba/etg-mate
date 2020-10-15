package org.mate.state;

import org.mate.state.executables.ActionsScreenState;
import org.mate.state.executables.AppScreen;

/**
 * Created by marceloeler on 21/06/17.
 */

public class ScreenStateFactory {
    public static IScreenState getScreenState(String stateType){
        if (stateType==null)
            return null;
        if (stateType.equals("ActionsScreenState")) {

            ActionsScreenState state = new ActionsScreenState(new AppScreen());
            for (int i = 0; i < 2; i++) {
                if (state.getWidgets().size() == 0) {
                    // There is a strong possibility that the Accessibility Service failed to parse
                    // the screen. Let's try again after some time.

                    try {
                        Thread.sleep(5000);
                    } catch(Exception e){ }

                    state =  new ActionsScreenState(new AppScreen());
                } else {
                    return state;
                }
            }

            return state;
        }
        return null;
    }
}
