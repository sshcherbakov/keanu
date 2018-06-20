package io.improbable.keanu.research;

import io.improbable.keanu.research.randomfactory.RandomFactory;
import io.improbable.keanu.research.randomfactory.VertexBackedRandomFactory;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.generic.nonprobabilistic.operators.unary.UnaryOpLambda;

import java.util.function.BiFunction;

public class DoubleListLambdaVertex extends UnaryOpLambda<DoubleTensor[], DoubleTensor[]> {

    public DoubleListLambdaVertex(Vertex<DoubleTensor[]> arrayOfInputs,
                                  BiFunction<DoubleTensor[], RandomFactory, DoubleTensor[]> lambda,
                                  VertexBackedRandomFactory random) {
        super(arrayOfInputs, (DoubleTensor[] in) -> lambda.apply(in, random));
    }
}