package org.mate.exploration.heuristical;

import org.mate.MATE;
import org.mate.exploration.genetic.chromosome.IChromosome;
import org.mate.exploration.genetic.chromosome_factory.ViewRankingChromosomeCreator;
import org.mate.exploration.genetic.fitness.IFitnessFunction;
import org.mate.exploration.genetic.fitness.StatementCoverageFitnessFunction;
import org.mate.model.TestCase;
import org.mate.ui.EnvironmentManager;

import java.util.ArrayList;
import java.util.List;

public class ViewRanking {
    StatementCoverageFitnessFunction<TestCase> fitnessFunction = new StatementCoverageFitnessFunction<>();

    private final ViewRankingChromosomeCreator chromosomeFactory;

    List<IChromosome<TestCase>> representativeIndividuals = new ArrayList<>();
    double currentCombinedCoverage = 0;

    public ViewRanking(int maxNumEvents) {
        chromosomeFactory = new ViewRankingChromosomeCreator(maxNumEvents);
    }

    public void run() {
        for (int i = 0; true; i++) {
            if (MATE.timeoutReached.get() || i == 5) {
                return;
            }

            MATE.log_acc("Exploration #" + (i + 1));
            IChromosome<TestCase> chromosome = chromosomeFactory.createChromosome();
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
