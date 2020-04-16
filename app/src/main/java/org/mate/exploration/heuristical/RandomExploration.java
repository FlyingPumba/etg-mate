package org.mate.exploration.heuristical;

import org.mate.MATE;
import org.mate.Properties;
import org.mate.exploration.genetic.chromosome.Chromosome;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.chromosome_factory.AndroidRandomChromosomeFactory;
import org.mate.exploration.genetic.fitness.IFitnessFunction;
import org.mate.exploration.genetic.fitness.StatementCoverageFitnessFunction;
import org.mate.model.TestCase;
import org.mate.ui.Action;
import org.mate.ui.EnvironmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RandomExploration {
    private final AndroidRandomChromosomeFactory randomChromosomeFactory;
    private final boolean alwaysReset;
    StatementCoverageFitnessFunction fitnessFunction;

    List<IChromosome<TestCase>> representativeIndividuals = null;
    double currentCombinedCoverage = 0;

    public RandomExploration(int maxNumEvents) {
        this(Properties.STORE_COVERAGE, true, maxNumEvents);
    }

    public RandomExploration(boolean storeCoverage, boolean alwaysReset, int maxNumEvents) {
        this.alwaysReset = alwaysReset;
        randomChromosomeFactory = new AndroidRandomChromosomeFactory(storeCoverage, alwaysReset, maxNumEvents);
        fitnessFunction = new StatementCoverageFitnessFunction();
        representativeIndividuals = new ArrayList<>();
    }

    public void run() {
        for (int i = 0; true; i++) {
            MATE.log_acc("Exploration #" + (i + 1));
            IChromosome<TestCase> chromosome = randomChromosomeFactory.createChromosome();
            // double fitness = fitnessFunction.getFitness(chromosome);

            double combinedCoverage = EnvironmentManager.getCombinedCoverage();
            if (combinedCoverage > currentCombinedCoverage) {
                representativeIndividuals.add(chromosome);
                currentCombinedCoverage = combinedCoverage;
            }
        }
    }

    public void takeScreenshotsToRepresentativeIndividuals() {
        for (int i = 0; i < representativeIndividuals.size(); i++) {
            IChromosome<TestCase> individual = representativeIndividuals.get(i);
            String hash = individual.toString().split("@")[1];

            MATE.uiAbstractionLayer.resetApp();

            TestCase testCase = individual.getValue();
            Vector<Action> actions = testCase.getEventSequence();
            for (int j = 0; j < actions.size(); j++) {
                String name = String.format("MATE_%d_%s_%d", i, hash, j);
                EnvironmentManager.namedScreenShot(name);

                Action action = actions.get(j);
                MATE.uiAbstractionLayer.executeAction(action);
            }

            String name = String.format("MATE_%d_%s_%d", i, hash, actions.size());
            EnvironmentManager.namedScreenShot(name);
        }
    }

    public List<IChromosome<TestCase>> getRepresentativeIndividual() {
        return representativeIndividuals;
    }

    public IFitnessFunction<TestCase> getFitnessFunctions() {
        return fitnessFunction;
    }
}
