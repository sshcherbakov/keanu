package io.improbable.keanu.randomfactory;

import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.GaussianVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.UniformVertex;

import java.util.Random;

public class DoubleVertexFactory implements RandomFactory<DoubleVertex> {

    private Random random = new Random();

    @Override
    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public UniformVertex nextDouble(double min, double max) {
        UniformVertex uniformVertex = new UniformVertex(min, max, random);
        uniformVertex.setValue(uniformVertex.sample(random));
        return uniformVertex;
    }

    @Override
    public ConstantDoubleVertex nextConstant(double value) {
        return new ConstantDoubleVertex(value);
    }

    @Override
    public GaussianVertex nextGaussian(double mu, double sigma) {
        GaussianVertex gaussianVertex =  new GaussianVertex(mu, sigma, random);
        gaussianVertex.setValue(gaussianVertex.sample(random));
        return gaussianVertex;
    }

    @Override
    public GaussianVertex nextGaussian(DoubleVertex mu, double sigma) {
        GaussianVertex gaussianVertex = new GaussianVertex(mu, sigma, random);
        gaussianVertex.setValue(gaussianVertex.sample(random));
        return gaussianVertex;
    }

    @Override
    public GaussianVertex nextGaussian(DoubleVertex mu, DoubleVertex sigma) {
        GaussianVertex gaussianVertex =  new GaussianVertex(mu, sigma, random);
        gaussianVertex.setValue(gaussianVertex.sample(random));
        return gaussianVertex;
    }

    @Override
    public DoubleVertex nextGaussian(double mu, DoubleVertex sigma) {
        GaussianVertex gaussianVertex =  new GaussianVertex(mu, sigma, random);
        gaussianVertex.setValue(gaussianVertex.sample(random));
        return gaussianVertex;
    }

}
