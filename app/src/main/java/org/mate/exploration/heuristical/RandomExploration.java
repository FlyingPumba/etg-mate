package org.mate.exploration.heuristical;

import org.mate.MATE;
import org.mate.Properties;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.chromosome_factory.AndroidRandomChromosomeFactory;
import org.mate.exploration.genetic.fitness.IFitnessFunction;
import org.mate.exploration.genetic.fitness.LineCoveredPercentageFitnessFunction;
import org.mate.exploration.genetic.fitness.StatementCoverageFitnessFunction;
import org.mate.model.TestCase;

import java.util.ArrayList;
import java.util.List;

public class RandomExploration {
    private final AndroidRandomChromosomeFactory randomChromosomeFactory;
    private final boolean alwaysReset;
    StatementCoverageFitnessFunction fitnessFunction;

    IChromosome bestIndividual = null;
    double bestFitness = 0;

    public RandomExploration(int maxNumEvents) {
        this(Properties.STORE_COVERAGE, true, maxNumEvents);
    }

    public RandomExploration(boolean storeCoverage, boolean alwaysReset, int maxNumEvents) {
        this.alwaysReset = alwaysReset;
        randomChromosomeFactory = new AndroidRandomChromosomeFactory(storeCoverage, alwaysReset, maxNumEvents);
        fitnessFunction = new StatementCoverageFitnessFunction();
    }

    public void run() {
        for (int i = 0; true; i++) {
            if (alwaysReset) {
                MATE.uiAbstractionLayer.resetApp();
            }
            MATE.log_acc("Exploration #" + (i + 1));
            IChromosome<TestCase> chromosome = randomChromosomeFactory.createChromosome();

            double fitness = fitnessFunction.getFitness(chromosome);
            if (fitness > bestFitness) {
                bestIndividual = chromosome;
                bestFitness = fitness;
            }
        }
    }

    public IChromosome<TestCase> getBestIndividual() {
        return bestIndividual;
    }

    public IFitnessFunction<TestCase> getFitnessFunctions() {
        return fitnessFunction;
    }
}
