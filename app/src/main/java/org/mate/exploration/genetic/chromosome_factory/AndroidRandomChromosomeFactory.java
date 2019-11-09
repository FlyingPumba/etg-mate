package org.mate.exploration.genetic.chromosome_factory;

import org.mate.MATE;
import org.mate.Properties;
import org.mate.exploration.genetic.chromosome.Chromosome;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.fitness.LineCoveredPercentageFitnessFunction;
import org.mate.model.TestCase;
import org.mate.ui.Action;
import org.mate.interaction.UIAbstractionLayer;
import org.mate.ui.ActionType;
import org.mate.ui.EnvironmentManager;
import org.mate.utils.Randomness;

import java.util.ArrayList;
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

        try {
            for (int i = 0; i < maxNumEvents; i++) {
                if (!testCase.updateTestCase(selectAction(), String.valueOf(i))) {
                    return chromosome;
                }
            }
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

//mas propenso a hacer swipes down y up
    protected Action selectAction() {
        List<Action> executableActions = new ArrayList<>();

       for (Action action: uiAbstractionLayer.getExecutableActions()) {
           if (action.getActionType().equals(ActionType.SWIPE_DOWN) || action.getActionType().equals(ActionType.SWIPE_UP)) {
               executableActions.add(action);
           }
       }

        if (Randomness.getRnd().nextInt(2) != 0){
            executableActions.clear();
        }

       if (executableActions.isEmpty()){
           executableActions.addAll(uiAbstractionLayer.getExecutableActions());
       }

       return Randomness.randomElement(executableActions);

//        return Randomness.randomElement(uiAbstractionLayer.getExecutableActions());
    }
}
