package org.mate.exploration.genetic;

public class Chromosome<T> implements IChromosome<T> {
    private T value;

    public Chromosome(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }
}