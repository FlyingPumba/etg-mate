package org.mate.ui;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * Created by marceloe on 12/12/16.
 */

public class Action {

    private Widget widget;
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
        widget = new Widget("","","");
    }

    public Action(Widget widget, ActionType actionType) {
        setWidget(widget);
        setActionType(actionType);
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

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
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
                Objects.equals(widget.getIdByActivity(), action.widget.getIdByActivity());
    }

    @Override
    public int hashCode() {

        return Objects.hash(widget.getIdByActivity(), actionType);
    }

    public void setNetworkingInfo(List<String> networkingInfo) {
        this.networkingInfo = networkingInfo;
    }

    public List<String> getNetworkingInfo() {
        return networkingInfo;
    }
}
