package org.mate.exploration.genetic;

import org.mate.model.TestCase;

public class AndroidStateFitnessFunction implements IFitnessFunction<TestCase> {
    public static final String FITNESS_FUNCTION_ID = "android_state_fitness_function";

    @Override
    public double getFitness(IChromosome<TestCase> chromosome) {
        return chromosome.getValue().getVisitedStates().size();
    }
}
