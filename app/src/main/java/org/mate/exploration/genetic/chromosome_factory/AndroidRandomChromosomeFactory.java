package org.mate.exploration.genetic.chromosome_factory;

import org.mate.MATE;
import org.mate.Properties;
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
import org.mate.ui.ActionType;
import org.mate.ui.EnvironmentManager;
import org.mate.utils.Randomness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AndroidRandomChromosomeFactory implements IChromosomeFactory<TestCase> {
    public static final String CHROMOSOME_FACTORY_ID = "android_random_chromosome_factory";

    protected UIAbstractionLayer uiAbstractionLayer;
    private int maxNumEvents;
    private boolean storeCoverage;
    private boolean resetApp;

    public AndroidRandomChromosomeFactory(int maxNumEvents) {
        this(Properties.STORE_COVERAGE, true, maxNumEvents);
    }

    public AndroidRandomChromosomeFactory(boolean storeCoverage, boolean resetApp, int maxNumEvents) {
        this.uiAbstractionLayer = MATE.uiAbstractionLayer;
        this.maxNumEvents = maxNumEvents;
        this.storeCoverage = storeCoverage;
        this.resetApp = resetApp;
    }

    @Override
    public IChromosome<TestCase> createChromosome() {
        if (resetApp) {
            uiAbstractionLayer.resetApp();
        }

        TestCase testCase = TestCase.newInitializedTestCase();
        Chromosome<TestCase> chromosome = new Chromosome<>(testCase);

        LoginStrategy[] loginStrategies = {
                new SuccessfulLogin( this, true),
                new SuccessfulLogin( this, false),
                new SuccessfulLogin(this, true, true),
                new SuccessfulLogin(this, false, true),
//                new MissingDNILogin(this),
//                new MissingNroTramiteLogin(this),
//                new MissingSexLogin(this),
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
                    selectedAction = selectRandomAction();
                }

                if (!testCase.updateTestCase(selectedAction, String.valueOf(i))) {
                    return chromosome;
                }
            }
        } catch (Exception e) {
            // do nothing
            MATE.log_acc("An exception was found while creating Android Random Chromosome: " +
                    e.getMessage());
        } finally {
            //store coverage in an case
            if (storeCoverage) {
                EnvironmentManager.storeCoverageData(chromosome, null);

                MATE.log_acc("Coverage of: " + chromosome.toString() + ": " + EnvironmentManager
                        .getCoverage(chromosome));
                MATE.log_acc("Found crash: " + String.valueOf(chromosome.getValue().getCrashDetected()));

                //TODO: remove hack, when better solution implemented
                LineCoveredPercentageFitnessFunction.retrieveFitnessValues(chromosome);
            }
        }


        return chromosome;
    }

    @Override
    public UIAbstractionLayer getUiAbstractionLayer() {
        return uiAbstractionLayer;
    }

    protected Action selectRandomAction() {
        List<Action> executableActions = uiAbstractionLayer.getExecutableActions();

        // Give extra probability to all actions that are not BACK
        List<Action> actions = new ArrayList<>();
        for (Action action : executableActions) {
            if (action.getActionType() != ActionType.BACK) {
                actions.add(action);
            }

            actions.add(action);
        }

        return Randomness.randomElement(actions);
    }
}
