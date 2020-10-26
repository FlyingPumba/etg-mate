package org.mate.exploration.genetic.chromosome_factory;

import org.mate.MATE;
import org.mate.exploration.genetic.chromosome.Chromosome;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.fitness.LineCoveredPercentageFitnessFunction;
import org.mate.exploration.login_strategies.LoginStrategy;
import org.mate.exploration.login_strategies.MissingDNILogin;
import org.mate.exploration.login_strategies.MissingNroTramiteLogin;
import org.mate.exploration.login_strategies.MissingSexLogin;
import org.mate.exploration.login_strategies.SuccessfulLogin;
import org.mate.interaction.UIAbstractionLayer;
import org.mate.model.TestCase;
import org.mate.ui.Action;
import org.mate.ui.EnvironmentManager;
import org.mate.utils.Randomness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ViewRankingChromosomeCreator implements IChromosomeFactory<TestCase> {

    HashMap<String, ActionNode> nodes = new HashMap<>();

    private final int maxNumEvents;
    private final UIAbstractionLayer uiAbstractionLayer;

    public ViewRankingChromosomeCreator(int maxNumEvents) {
        this.maxNumEvents = maxNumEvents;
        this.uiAbstractionLayer = MATE.uiAbstractionLayer;
    }

    @Override
    public IChromosome<TestCase> createChromosome() {
        uiAbstractionLayer.resetApp();
        initNodes();

        TestCase testCase = TestCase.newInitializedTestCase();
        Chromosome<TestCase> chromosome = new Chromosome<>(testCase);

        LoginStrategy[] loginStrategies = {
                new SuccessfulLogin( this, true),
                new SuccessfulLogin( this, false),
                new SuccessfulLogin(this, true, true),
                new SuccessfulLogin(this, false, true),
                new MissingDNILogin(this),
                new MissingNroTramiteLogin(this),
                new MissingSexLogin(this),
        };
        LoginStrategy loginStrategy = Randomness.randomElement(Arrays.asList(loginStrategies));

        try {
            for (int i = 0; i < maxNumEvents; i++) {
                if (MATE.timeoutReached.get()) {
                    return null;
                }

                Action selectedAction;
                if (loginStrategy.hasNextAction()) {
                    selectedAction = loginStrategy.getNextAction();
                } else {
                    selectedAction = getBestRankingAction();
                }

                String previousActivityName = EnvironmentManager.getCurrentActivityName();

                boolean inboundResult = testCase.updateTestCase(selectedAction, String.valueOf(i));

                updateNodes(selectedAction, previousActivityName, inboundResult);

                if (!inboundResult) {
                    return chromosome;
                }
            }
        } catch (Exception e) {
            // do nothing
            MATE.log_acc("An exception was found while creating View Ranking Chromosome: " +
                    e.getMessage());
        } finally {
            //store coverage
            EnvironmentManager.storeCoverageData(chromosome, null);

            MATE.log_acc("Coverage of: " + chromosome.toString() + ": " + EnvironmentManager
                    .getCoverage(chromosome));
            MATE.log_acc("Found crash: " + String.valueOf(chromosome.getValue().getCrashDetected()));

            //TODO: remove hack, when better solution implemented
            LineCoveredPercentageFitnessFunction.retrieveFitnessValues(chromosome);
        }


        return chromosome;
    }

    private Action getBestRankingAction() {
        List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
        String activityName = EnvironmentManager.getCurrentActivityName();

        // classify available actions in current screen
        List<ActionNode> bestRankedActions = new ArrayList<>();
        float bestRankingValue = -1;
        for (Action action : executableActions) {
            String actionHash = ActionNode.getActionHash(action, activityName);

            ActionNode node = nodes.get(actionHash);

            if (node == null) {
                throw new Error("Node not found in getBestRankingAction method");
            }

            float nodeRankingValue = node.getRankingValue();
            if (nodeRankingValue > bestRankingValue) {
                bestRankedActions.clear();
                bestRankedActions.add(node);
                bestRankingValue = nodeRankingValue;
            } else if (nodeRankingValue == bestRankingValue) {
                bestRankedActions.add(node);
                bestRankingValue = nodeRankingValue;
            }
        }

        ActionNode actionNode = Randomness.randomElement(bestRankedActions);
        actionNode.setActionExecuted();

        return actionNode.getAction();
    }

    /**
     * Insert all executable actions into the hash map of known actions
     */
    private void initNodes() {
        List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
        String activityName = EnvironmentManager.getCurrentActivityName();

        for (Action action : executableActions) {
            String actionHash = ActionNode.getActionHash(action, activityName);

            if (!nodes.containsKey(actionHash)) {
                nodes.put(actionHash, new ActionNode(action, activityName));
            }
        }
    }

    private void updateNodes(Action executedAction, String previousActivityName, boolean inboundResult) {
        List<Action> executableActions = uiAbstractionLayer.getExecutableActions();
        String currentActivityName = EnvironmentManager.getCurrentActivityName();

        // which nodes are available after we executed the action?
        List<ActionNode> reachedNodes = new ArrayList<>();
        for (Action action : executableActions) {
            String actionHash = ActionNode.getActionHash(action, currentActivityName);

            if (!nodes.containsKey(actionHash)) {
                ActionNode node = new ActionNode(action, currentActivityName);
                nodes.put(node.getNodeHash(), node);
            }

            ActionNode actionNode = nodes.get(actionHash);
            reachedNodes.add(actionNode);
        }


        // update reachable nodes of executed action
        String executedActionHash = ActionNode.getActionHash(executedAction, previousActivityName);
        ActionNode executedActionNode = nodes.get(executedActionHash);
        if (executedActionNode == null) {
            throw new Error("Executed action node not found in updateNodes method");
        }
        executedActionNode.updateReachableNodes(reachedNodes);
    }

    @Override
    public UIAbstractionLayer getUiAbstractionLayer() {
        return uiAbstractionLayer;
    }

    static class ActionNode {
        private final Action action;
        private final String activityName;
        private boolean actionExecuted = false;
        private float rankingValue;

        private HashMap<String, ActionNode> reachableNodes = new HashMap<>();

        public ActionNode(Action action, String activityName) {
            this.action = action;
            this.activityName = activityName;
            updateRankingValue();
        }

        public float getRankingValue() {
            return rankingValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ActionNode that = (ActionNode) o;
            return this.getNodeHash().equals(that.getNodeHash());
        }

        public String getNodeHash() {
            return getActionHash(action, activityName);
        }

        public static String getActionHash(Action action, String activityName) {
            return String.format(Locale.US, "%s_%d", activityName, action.hashCode());
        }

        public void setActionExecuted() {
            actionExecuted = true;
        }

        public boolean wasExecuted() {
            return actionExecuted;
        }

        public Action getAction() {
            return action;
        }

        public void updateReachableNodes(List<ActionNode> reachedNodes) {
            for (ActionNode node : reachedNodes) {
                if (!reachableNodes.containsKey(node.getNodeHash())) {
                    reachableNodes.put(node.getNodeHash(), node);
                }
            }
            updateRankingValue();
        }

        private void updateRankingValue() {
            if (actionExecuted) {
                rankingValue = 0f;
            }
            if (reachableNodes.size() == 0) {
                rankingValue = 0.0001f;
            }

            float reachableNodesNotExecuted = 0f;
            for (ActionNode node : reachableNodes.values()) {
                if (!node.wasExecuted()) {
                    reachableNodesNotExecuted += 1f;
                }
            }

            this.rankingValue = reachableNodesNotExecuted / (float)reachableNodes.size();
        }
    }
}
