package org.mate.interaction;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.text.InputType;

import org.mate.MATE;
import org.mate.datagen.DataGenerator;
import org.mate.exceptions.AUTCrashException;
import org.mate.model.IGUIModel;
import org.mate.ui.Action;
import org.mate.ui.ActionType;
import org.mate.ui.EnvironmentManager;
import org.mate.ui.Widget;

import java.util.List;

/**
 * Created by marceloeler on 08/03/17.
 */

public class DeviceMgr implements IApp {

    private UiDevice device;
    private String packageName;

    public DeviceMgr(UiDevice device, String packageName){
        this.device = device;
        this.packageName = packageName;
    }

    public void executeAction(Action action) throws AUTCrashException{
        MATE.log(" ____ execute " + action.getActionType() + " on " + action.getWidget().getId() + "  : " + action.getWidget().getText() + "  hint: " + action.getWidget().getHint());
        Widget selectedWidget = action.getWidget();
        int typeOfAction = action.getActionType();

        switch (typeOfAction){

            case ActionType.CLICK:
                handleClick(selectedWidget);
                break;

            case ActionType.LONG_CLICK:
                handleLongPress(selectedWidget);
                break;

            case ActionType.TYPE_TEXT:
                handleEdit(action);
                break;

            case ActionType.CLEAR_WIDGET:
                handleClear(selectedWidget);
                break;

            case ActionType.SWIPE_DOWN:
                handleSwipe(selectedWidget, 0);
                break;

            case ActionType.SWIPE_UP:
                handleSwipe(selectedWidget, 1);
                break;

            case ActionType.SWIPE_LEFT:
                handleSwipe(selectedWidget, 2);
                break;

            case ActionType.SWIPE_RIGHT:
                handleSwipe(selectedWidget, 3);
                break;

            case ActionType.WAIT:
                break;

            case ActionType.BACK:
                device.pressBack();;
                break;

            case ActionType.MENU:
                device.pressMenu();
                break;

            case ActionType.ENTER:
                device.pressEnter();
                break;

        }

        //if there is a progress bar associated to that action
        sleep(action.getTimeToWait());

        //handle app crashes
        UiObject window = new UiObject(new UiSelector().packageName("android")
                .textContains("has stopped"));
        if (window.exists()) {
            MATE.log("CRASH");
            throw new AUTCrashException("App crashed");
        }
    }

    public void handleClick(Widget widget){
        device.click(widget.getX(),widget.getY());
    }

    public void handleClear(Widget widget){
        UiObject2 obj = findObject(widget);
        if (obj!=null)
            obj.setText("");
    }

    public void handleSwipe(Widget widget, int direction){

        int pixelsmove=300;
        int X = 0;
        int Y = 0;
        int steps = 15;

        if (!widget.getClazz().equals("")){
            UiObject2 obj = findObject(widget);
            if (obj!=null){
                X = obj.getVisibleBounds().centerX();
                Y = obj.getVisibleBounds().centerY();
            }
            else {
                X = widget.getX();
                Y = widget.getY();
            }
        }
        else{
            X = device.getDisplayWidth()/2;
            Y = device.getDisplayHeight()/2;
            if (direction==0 || direction==1)
                pixelsmove=Y;
            else
                pixelsmove=X;
        }

        //50 pixels has been arbitrarily selected - create a properties file in the future
        switch (direction){
            case 0: device.swipe(X, Y, X, Y-pixelsmove,steps);
                break;

            case 1: device.swipe(X, Y, X, Y+pixelsmove,steps);
                break;
            case 2: device.swipe(X, Y, X+pixelsmove, Y,steps);
                break;
            case 3: device.swipe(X, Y, X-pixelsmove, Y,steps);
                break;
        }
    }

    public void handleLongPress(Widget widget) {
        UiObject2 obj = findObject(widget);
        int X = widget.getX();
        int Y = widget.getY();
        if (obj!=null){
            X = obj.getVisibleBounds().centerX();
            Y = obj.getVisibleBounds().centerY();
        }
        device.swipe(X, Y, X, Y,120);
    }

    public void sleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private UiObject2 findObject(Widget widget){
        List<UiObject2> objs = device.findObjects(By.res(widget.getId()));
        if (objs!=null){
            if (objs.size()==1)
                return objs.get(0);
            else {
                for (UiObject2 uiObject2: objs){
                    if (uiObject2.getText()!=null && uiObject2.getText().equals(widget.getText()))
                        return uiObject2;
                }
            }
        }

        //if no obj has been found by id, and then text if there is more than one object with the same id
        objs = device.findObjects(By.text(widget.getText()));
        if (objs!=null){
            if (objs.size()==1)
                return objs.get(0);
            else {
                for (UiObject2 uiObject2: objs){
                    if (uiObject2.getContentDescription()!=null && uiObject2.getContentDescription().equals(widget.getContentDesc()) ||
                            (uiObject2.getVisibleBounds()!=null && uiObject2.getVisibleBounds().centerX()==widget.getX() && uiObject2.getVisibleBounds().centerY()==widget.getY()))
                        return uiObject2;
                }
            }
        }
        return null;
    }

    public void handleEdit(Action action){

        Widget widget = action.getWidget();
        String textData = generateTextData(action);

        if (widget.getResourceID().equals("")){
            if (!widget.getText().equals("")) {
                UiObject2 obj = device.findObject(By.text(widget.getText()));
                if (obj != null) {
                    obj.setText(textData);
                }
            }
            else{
                device.click(widget.getX(),widget.getY());
                UiObject2 obj = device.findObject(By.focused(true));
                if (obj!=null){
                    obj.setText(textData);
                }
            }
        }
        else{
            List<UiObject2> objs = device.findObjects(By.res(widget.getId()));
            if (objs!=null && objs.size()>0){
                int i=0;
                int size = objs.size();
                boolean objfound=false;
                while (i<size && !objfound){
                    UiObject2 obj = objs.get(i);
                    if (obj!=null) {
                        String objText = "";
                        if (obj.getText()!=null)
                            objText = obj.getText();
                        if (objText.equals(widget.getText())) {
                            obj.setText(textData);
                            objfound=true;

                        }
                    }
                    i++;
                }
                if (!objfound)
                    MATE.log("  ********* obj "+widget.getId()+ "  not found");
            }
            else{
                MATE.log("  ********* obj "+widget.getId()+ "  not found");
            }
        }

        action.setExtraInfo(textData);

    }

    public String generateTextData(Action action) {
        Widget widget = action.getWidget();

        String widgetText = widget.getText();
        if (widgetText.equals(""))
            widgetText = widget.getHint();

        String textData = "";
        String inputType ="";
        int maxLengthInt=widget.getMaxLength();
        if (action.getExtraInfo().equals("")){

            if (maxLengthInt<0)
                maxLengthInt=15;
            if (maxLengthInt>15)
                maxLengthInt=15;

            if (widget.getInputType() == (InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER))
                inputType="number";
            if (widget.getInputType()==InputType.TYPE_CLASS_PHONE)
                inputType="phone";
            if (widget.getInputType()==InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                inputType="email";
            if (widget.getInputType()==InputType.TYPE_TEXT_VARIATION_URI)
                inputType="uri";


            widgetText = widgetText.replace(".","");
            widgetText = widgetText.replace(",","");
            if (inputType.equals("") && !widgetText.equals("") && android.text.TextUtils.isDigitsOnly(widgetText)) {
                inputType = "number";
            }

            if (inputType.equals("")){
                String desc = widget.getContentDesc();
                if (desc!=null){
                    if (desc.contains("email")||desc.contains("e-mail")||desc.contains("E-mail")||desc.contains("Email"))
                        inputType="email";
                }
            }
            if (inputType.equals(""))
                inputType="text";


            textData = getRandomData(inputType,maxLengthInt);
        }
        else{
            textData = action.getExtraInfo();
        }
        return textData;
    }

    private String getRandomData(String inputType, int maxLengthInt) {
        //need to also generate random invalid string, number, email, uri, ...
        String textData = "";
        DataGenerator dataGen = new DataGenerator();
        if (inputType!=null){

            if (inputType.contains("phone") || inputType.contains("number") || inputType.contains("Phone") || inputType.contains("Number")) {
                textData = dataGen.getRandomValidNumber(maxLengthInt);
            }
            else
            if (inputType.contains("Email") || inputType.contains("email")) {
                textData = dataGen.getRandomValidEmail(maxLengthInt);
            }
            else
            if (inputType.contains("uri") || inputType.contains("URI")) {
                textData = dataGen.getRandomUri(maxLengthInt);
            }
            else {
                textData = dataGen.getRandomValidString(maxLengthInt);
            }
        }
        else
            textData = dataGen.getRandomValidString(maxLengthInt);
        return textData;
    }

    public void reinstallApp(){
        MATE.log("Reinstall app");
        EnvironmentManager.clearAppData();
        //sleep(1000);
    }

    public void restartApp() {
            MATE.log("Restarting app");
            // Launch the app
            Context context = InstrumentationRegistry.getContext();
            final Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(packageName);
            // Clear out any previous instances
            try{
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            catch(Exception e){
                MATE.log("EXCEPTION CLEARING ACTIVITY FLAG");
            }
            context.startActivity(intent);
           // sleep(1000);

    }

    public void handleCrashDialog() {
        UiObject button = null;
        String[] COMMON_BUTTONS = {
                "OK", "Cancel", "Yes", "No", "Dismiss"
        };
        for (String keyword : COMMON_BUTTONS) {
            button = device.findObject(new UiSelector().text(keyword).enabled(true));
            if (button != null && button.exists()) {
                break;
            }
        }
        try {
            // sometimes it takes a while for the OK button to become enabled
            if (button != null && button.exists()) {
                button.waitForExists(1000);
                button.click();
                sleep(1000);
                restartApp();
            }
        } catch (UiObjectNotFoundException e) {
        }
    }



    public boolean goToState(IGUIModel guiModel, String targetScreenStateId){
        return new GUIWalker(guiModel, packageName,this).goToState(targetScreenStateId);
    }

}
