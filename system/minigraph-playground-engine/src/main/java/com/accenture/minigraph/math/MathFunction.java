package com.accenture.minigraph.math;

@FunctionalInterface
public interface MathFunction {
    double apply(double... args);
}