package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.unary;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.Ints;

import io.improbable.keanu.tensor.TensorShape;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.VertexId;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.PartialDerivatives;

public class SumVertex extends DoubleUnaryOpVertex {

    private final int[] overDimensions;

    /**
     * Performs a sum across specified dimensions
     *
     * @param inputVertex    the vertex to have its values summed
     * @param overDimensions dimensions to sum over
     */
    public SumVertex(DoubleVertex inputVertex, int[] overDimensions) {
        super(getSummationResultShape(inputVertex.getShape(), overDimensions), inputVertex);
        this.overDimensions = overDimensions;
    }

    /**
     * Performs a sum across all dimensions
     *
     * @param inputVertex the vertex to have its values summed
     */
    public SumVertex(DoubleVertex inputVertex) {
        this(inputVertex, TensorShape.dimensionRange(0, inputVertex.getShape().length));
    }

    /**
     * This is here due to strange behavior in tensor summing over dimensions where
     * dimensions are not dropped if the rank is 2 or less.
     */
    private static int[] getSummationResultShape(int[] inputShape, int[] sumOverDimensions) {
        List<Integer> inputShapeList = new ArrayList<>(Ints.asList(inputShape));

        for (int dim : sumOverDimensions) {
            inputShapeList.set(dim, 0);
        }

        for (int i = inputShapeList.size() - 1; i >= 0; i--) {
            if (inputShapeList.get(i) == 0) {
                if (inputShapeList.size() > 2) {
                    inputShapeList.remove(i);
                } else {
                    inputShapeList.set(i, 1);
                }
            }
        }

        return Ints.toArray(inputShapeList);
    }

    @Override
    protected DoubleTensor op(DoubleTensor value) {
        return value.sum(overDimensions);
    }

    @Override
    protected DualNumber dualOp(DualNumber dualNumber) {
        return dualNumber.sum(overDimensions);
    }

    @Override
    public Map<Vertex, PartialDerivatives> reverseModeAutoDifferentiation(PartialDerivatives derivativeOfOutputsWithRespectToSelf) {

        int[] wrtShapeWithoutRankLoss = summedOverShapeWithoutRankLoss(inputVertex.getShape(), overDimensions);

        PartialDerivatives reshapedDiffWrtSelf = new PartialDerivatives(new HashMap<>());
        for (Map.Entry<VertexId, DoubleTensor> partialDerivative : derivativeOfOutputsWithRespectToSelf.asMap().entrySet()) {
            DoubleTensor partial = partialDerivative.getValue();

            int[] newPartialShape = TensorShape.concat(
                TensorShape.selectDimensions(0, partial.getRank() - getShape().length, partial.getShape()),
                wrtShapeWithoutRankLoss
            );

            DoubleTensor reshapedPartialDerivative = partialDerivative.getValue().reshape(newPartialShape);

            reshapedDiffWrtSelf.putWithRespectTo(partialDerivative.getKey(), reshapedPartialDerivative);
        }

        PartialDerivatives derivativesWrtInput = reshapedDiffWrtSelf
            .multiplyAlongWrtDimensions(DoubleTensor.ones(inputVertex.getShape()), wrtShapeWithoutRankLoss);

        return singletonMap(inputVertex, derivativesWrtInput);
    }

    private static int[] summedOverShapeWithoutRankLoss(int[] shape, int[] sumOverDimensions) {
        int[] shapeCopy = Arrays.copyOf(shape, shape.length);
        for (int sumOverDimension : sumOverDimensions) {
            shapeCopy[sumOverDimension] = 1;
        }
        return shapeCopy;
    }
}
