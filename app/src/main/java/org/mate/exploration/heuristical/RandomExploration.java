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
            if (MATE.timeoutReached.get()) {
                return;
            }

            MATE.log_acc("Exploration #" + (i + 1));
            IChromosome<TestCase> chromosome = randomChromosomeFactory.createChromosome();
            if (chromosome == null) {
                // timeout reached during chromosome creation
                return;
            }

            // double fitness = fitnessFunction.getFitness(chromosome);

            // get the combined coverage of representative individuals + the new individual
            List<IChromosome<TestCase>> individuals = new ArrayList<>(representativeIndividuals);
            individuals.add(chromosome);

            double combinedCoverage = EnvironmentManager.getCombinedCoverage(individuals);
            boolean combinedCoverageIncreased = combinedCoverage > currentCombinedCoverage;
            MATE.log_acc("Combined coverage after: " + chromosome.toString() + ": " +
                    combinedCoverage + (combinedCoverageIncreased ? " -> INCREASED" : ""));
            if (combinedCoverageIncreased) {
                representativeIndividuals.add(chromosome);
                currentCombinedCoverage = combinedCoverage;
            }
        }
    }

    public List<IChromosome<TestCase>> getRepresentativeIndividual() {
        return representativeIndividuals;
    }

    public IFitnessFunction<TestCase> getFitnessFunctions() {
        return fitnessFunction;
    }
}
