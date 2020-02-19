package org.mate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.mate.MATE;
import org.mate.interaction.UIAbstractionLayer;
import org.mate.state.IScreenState;
import org.mate.ui.Action;
import org.mate.utils.Optional;
import org.mate.utils.Randomness;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class TestCase {
    private String id;
    private Set<String> visitedActivities;
    private double coverage;

    @JsonIgnore
    private Set<String> visitedStates;

    private Vector<Action> eventSequence;
    private float novelty;
    private boolean crashDetected;
    private double sparseness;

    @JsonIgnore
    private HashMap<String, String> statesMap;

    @JsonIgnore
    private HashMap<String, Integer> featureVector;

    @JsonIgnore
    private Optional<Integer> desiredSize = Optional.none();

    public TestCase(String id){
        setId(id);
        crashDetected = false;
        visitedActivities = new HashSet<>();
        coverage = 0;
        visitedStates = new HashSet<>();
        eventSequence = new Vector<>();
        sparseness = 0;
        statesMap = new HashMap<>();
        featureVector = new HashMap<String, Integer>();

    }

    public void setDesiredSize(Optional<Integer> desiredSize) {
        this.desiredSize = desiredSize;
    }

    @JsonIgnore
    public Optional<Integer> getDesiredSize() {
        return desiredSize;
    }

    public String getId() {
        return id;
    }

    private void setId(String id) {
        this.id = id;
    }

    public void addEvent(Action event){this.eventSequence.add(event);};

    public void updateVisitedActivities(String activity){this.visitedActivities.add(activity);};

    public Set<String> getVisitedActivities() {
        return visitedActivities;
    }

    public void updateVisitedStates(IScreenState GUIState){this.visitedStates.add(GUIState.getId());};

    @JsonIgnore
    public Set<String> getVisitedStates() {
        return visitedStates;
    }

    public Vector<Action> getEventSequence(){return this.eventSequence;};

    public boolean getCrashDetected(){return this.crashDetected;};

    public void setCrashDetected(){this.crashDetected=true;};

    public void setNovelty(float novelty) {
        this.novelty = novelty;
    }

    public float getNovelty() {
        return novelty;
    }

    public double getSparseness() {
        return sparseness;
    }

    public void setSparseness(double sparseness) {
        this.sparseness = sparseness;
    }

    public void updateStatesMap(String state, String event) {
        if (!statesMap.containsKey(state)){
            statesMap.put(state, event);
            //MATE.log_acc("TEST___added to states map the state: "+state+" at event: "+event);
        }
    }

    @JsonIgnore
    public HashMap<String, String> getStatesMap() {
        return statesMap;
    }

    @JsonIgnore
    public HashMap<String, Integer> getFeatureVector() {
        return featureVector;
    }

    public void updateFeatureVector(IGUIModel guiModel) {
        Vector<IScreenState> guiStates = guiModel.getStates();
        for(IScreenState state : guiStates){
            if(this.visitedStates.contains(state.getId())){
                featureVector.put(state.getId(),1);
            } else {
                featureVector.put(state.getId(),0);
            }
        }
    }

    //TODO: Load test case from cache if it was executed before
    public static TestCase fromDummy(TestCase testCase) {
        MATE.uiAbstractionLayer.resetApp();
        TestCase resultingTc = newInitializedTestCase();

        int finalSize = testCase.eventSequence.size();

        if (testCase.desiredSize.hasValue()) {
            finalSize = testCase.desiredSize.getValue();
        }

        int count = 0;
        for (Action action : testCase.eventSequence) {
            if (count < finalSize) {
                if (MATE.uiAbstractionLayer.getExecutableActions().contains(action)) {
                    if (!resultingTc.updateTestCase(action, String.valueOf(count))) {
                        break;
                    }
                    count++;
                }
            } else {
                break;
            }
        }
        for (int i = count; i < finalSize; i++) {
            Action action = Randomness.randomElement(MATE.uiAbstractionLayer.getExecutableActions());
            if(!resultingTc.updateTestCase(action, String.valueOf(count))) {
                break;
            }
        }

        return resultingTc;
    }

    /**
     * Initializes
     * @return
     */
    public static TestCase newInitializedTestCase() {
        TestCase tc = new TestCase(UUID.randomUUID().toString());
        tc.updateTestCase("init");
        return tc;
    }

    /**
     * Perform action and update TestCase accordingly.
     * @param a Action to perform
     * @param event Event name
     * @return True if action successful inbound false if outbound, crash, or some unkown failure
     */
    public boolean updateTestCase(Action a, String event) {
        if (!MATE.uiAbstractionLayer.getExecutableActions().contains(a)) {
            throw new IllegalStateException("Action not applicable to current state");
        }
        addEvent(a);
        UIAbstractionLayer.ActionResult actionResult = MATE.uiAbstractionLayer.executeAction(a);

        switch (actionResult) {
            case SUCCESS:
            case SUCCESS_NEW_STATE:
                updateTestCase(event);
                return true;
            case FAILURE_APP_CRASH:
                setCrashDetected();
            case SUCCESS_OUTBOUND:
                return false;
            case FAILURE_UNKNOWN:
            case FAILURE_EMULATOR_CRASH:
                return false;
            default:
                throw new UnsupportedOperationException("Encountered an unknown action result. Cannot continue.");
        }
    }

    private void updateTestCase(String event) {
        IScreenState currentScreenstate = MATE.uiAbstractionLayer.getCurrentScreenState();

        updateVisitedStates(currentScreenstate);
        updateVisitedActivities(currentScreenstate.getActivityName());
        updateStatesMap(currentScreenstate.getId(), event);
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public double getCoverage() {
        return this.coverage;
    }
}
