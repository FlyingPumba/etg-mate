package org.mate.ui;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * Created by marceloe on 12/12/16.
 */

public class Action {

    private Widget rootWidget;
    private Vector<Integer> targetWidgetPath = new Vector<>();
    private ActionType actionType;
    private String extraInfo;
    private boolean executed;

    private float fitness;
    private long timeToWait;
    private float pheromone;
    private float proportionalPheromone;
    private @Nullable Swipe swipe;


    private Vector<Action> adjActions;
    private List<String> networkingInfo;

    public Vector<Action> getAdjActions() {
        return adjActions;
    }

    public Action(ActionType actionType){
        this.actionType = actionType;
        fitness=0;
        rootWidget = new Widget("","","");
    }

    public Action(Widget rootWidget, Vector<Integer> widgetPath, ActionType actionType) {
        setActionType(actionType);

        setRootWidget(rootWidget);
        targetWidgetPath.addAll(widgetPath);

        setExtraInfo("");
        adjActions = new Vector<Action>();
        setExecuted(false);
    }

    public void setSwipe(Swipe swipe) {
        this.swipe = swipe;
    }

    @Nullable
    public Swipe getSwipe() {
        return swipe;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    @JsonIgnore
    public Widget getWidget() {
        Widget targetWidget = rootWidget;
        for (Integer index : targetWidgetPath) {
            targetWidget = targetWidget.getChildren().get(index);
        }
        return targetWidget;
    }

    public Widget getRootWidget() {
        return rootWidget;
    }

    public void setRootWidget(Widget rootWidget) {
        this.rootWidget = rootWidget;
    }

    public Vector<Integer> getTargetWidgetPath() {
        return targetWidgetPath;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public void addAdjAction(Action eventAction){
        adjActions.add(eventAction);
    }

    public void setFitness(float fitness) {
        this.fitness = fitness;
    }

    public long getTimeToWait() {
        return timeToWait;
    }

    public void setTimeToWait(long timeToWait) {
        this.timeToWait = timeToWait;
    }
    public float getPheromone() {
        return pheromone;
    }

    public void setPheromone(float pheromone) {
        this.pheromone = pheromone;
    }
    public float getProportionalPheromone() {
        return proportionalPheromone;
    }

    public void setProportionalPheromone(float proportionalPheromone) {
        this.proportionalPheromone = proportionalPheromone;
    }

    public boolean isSwipe(){
        return getActionType().equals(ActionType.SWIPE_DOWN)
                || getActionType().equals(ActionType.SWIPE_UP)
                || getActionType().equals(ActionType.SWIPE_LEFT)
                || getActionType().equals(ActionType.SWIPE_RIGHT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return actionType == action.actionType &&
                Objects.equals(getWidget().getIdByActivity(), action.getWidget().getIdByActivity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWidget().getIdByActivity(), actionType);
    }

    public void setNetworkingInfo(List<String> networkingInfo) {
        this.networkingInfo = networkingInfo;
    }

    public List<String> getNetworkingInfo() {
        return networkingInfo;
    }
}
