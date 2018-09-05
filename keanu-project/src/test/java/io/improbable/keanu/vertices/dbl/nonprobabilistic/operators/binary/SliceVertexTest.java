package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary;

import com.google.common.collect.ImmutableSet;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.dbl.Differentiator;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.PartialDerivatives;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.unary.SliceVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.UniformVertex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class SliceVertexTest {

    private DoubleVertex matrixA;

    @Before
    public void setup() {
       matrixA = new ConstantDoubleVertex(DoubleTensor.create(new double[]{1, 2, 3, 4, 5, 6}, 2, 3));
    }

    @Test
    public void canGetTensorAlongDimensionOfRank2() {
        SliceVertex rowOne = new SliceVertex(matrixA, 0, 0);

        Assert.assertArrayEquals(new double[]{1, 2, 3}, rowOne.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{1, 3}, rowOne.getShape());

        SliceVertex rowTwo = new SliceVertex(matrixA, 0, 1);

        Assert.assertArrayEquals(new double[]{4, 5, 6}, rowTwo.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{1, 3}, rowTwo.getShape());

        SliceVertex columnOne = new SliceVertex(matrixA, 1, 0);

        Assert.assertArrayEquals(new double[]{1, 4}, columnOne.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 1}, columnOne.getShape());

        SliceVertex columnTwo = new SliceVertex(matrixA, 1, 1);

        Assert.assertArrayEquals(new double[]{2, 5}, columnTwo.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 1}, columnTwo.getShape());

        SliceVertex columnThree = new SliceVertex(matrixA, 1, 2);

        Assert.assertArrayEquals(new double[]{3, 6}, columnThree.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 1}, columnThree.getShape());
    }

    @Test
    public void canRepeatablySliceForAPick() {
        DoubleVertex m = new UniformVertex(0, 10);
        m.setValue(DoubleTensor.create(new double[]{1, 2, 3, 4}, 2, 2));

        SliceVertex columnZero = new SliceVertex(m, 0, 0);
        SliceVertex elementZero = new SliceVertex(columnZero, 0, 0);

        Assert.assertEquals(elementZero.getValue().scalar(), 1, 1e-6);
    }

    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimZeroIndexZero() {
        assertSlice(0, 0, new double[]{11, 17}, new int[]{1, 2}, new int[]{1, 2, 4, 2});
    }

    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimZeroIndexOne() {
        assertSlice(0, 1, new double[]{23, 29}, new int[]{1, 2}, new int[]{1, 2, 4, 2});
    }

    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimZeroIndexTwo() {
        assertSlice(0, 2, new double[]{36, 42}, new int[]{1, 2}, new int[]{1, 2, 4, 2});
    }

    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimZeroIndexThree() {
        assertSlice(0, 3, new double[]{48, 54}, new int[]{1, 2}, new int[]{1, 2, 4, 2});
    }

    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimOneIndexZero() {
        assertSlice(1, 0, new double[]{11, 23, 36, 48}, new int[]{4, 1}, new int[]{4, 1, 4, 2});
    }
    
    @Test
    public void sliceCorrectlySplitsRowOfPartialDerivativeDimOneIndexOne() {
        assertSlice(1, 1, new double[]{17, 29, 42, 54}, new int[]{4, 1}, new int[]{4, 1, 4, 2});
    }

    private void assertSlice(int dim, int ind, double[] expectedValue, int[] expectedShape, int[] expectedPartialShape) {
        DoubleVertex m = new UniformVertex(0, 10);
        m.setValue(DoubleTensor.create(new double[]{1, 2, 3, 4, 6, 7, 8, 9}, 4, 2));

        DoubleVertex alpha = new UniformVertex(0, 10);
        alpha.setValue(DoubleTensor.create(new double[]{10, 15, 20, 25, 30, 35, 40, 45}, 4, 2));

        DoubleVertex N = m.plus(alpha);

        SliceVertex sliceN = new SliceVertex(N, dim, ind);

        PartialDerivatives forward = Differentiator.forwardModeAutoDiff(sliceN, Arrays.asList(m, alpha));
        PartialDerivatives backward = Differentiator.reverseModeAutoDiff(sliceN, ImmutableSet.of(m, alpha));

        DoubleTensor originalPartial = N.getDualNumber().getPartialDerivatives().withRespectTo(m);

        Assert.assertArrayEquals(sliceN.getValue().asFlatDoubleArray(), expectedValue, 1e-6);
        Assert.assertArrayEquals(expectedShape, sliceN.getShape());

        Assert.assertArrayEquals(originalPartial.slice(dim, ind).asFlatDoubleArray(), forward.withRespectTo(m).asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(expectedPartialShape, forward.withRespectTo(m).getShape());

        Assert.assertArrayEquals(originalPartial.slice(dim, ind).asFlatDoubleArray(), backward.withRespectTo(m).asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(expectedPartialShape, backward.withRespectTo(m).getShape());
    }

    @Test
    public void sliceCorrectlySplitsColumnOfPartialDerivative() {
        DoubleVertex m = new UniformVertex(0, 10);
        m.setValue(DoubleTensor.create(new double[]{1, 2, 3, 4}, 2, 2));

        DoubleVertex alpha = new UniformVertex(0, 10);
        alpha.setValue(DoubleTensor.create(new double[]{10, 15, 20, 25}, 2, 2));

        DoubleVertex N = m.matrixMultiply(alpha);

        SliceVertex sliceN = new SliceVertex(N, 1, 1);

        DoubleTensor originalPartial = N.getDualNumber().getPartialDerivatives().withRespectTo(m);
        DoubleTensor slicePartial = sliceN.getDualNumber().getPartialDerivatives().withRespectTo(m);

        Assert.assertArrayEquals(sliceN.getValue().asFlatDoubleArray(), new double[]{65, 145}, 1e-6);
        Assert.assertArrayEquals(new int[]{2, 1}, sliceN.getShape());

        Assert.assertArrayEquals(originalPartial.slice(1, 1).asFlatDoubleArray(), slicePartial.asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 1, 2, 2}, slicePartial.getShape());
    }

    @Test
    public void canGetTensorAlongDimensionOfRank3() {
        DoubleVertex cube = new UniformVertex(0, 10);
        cube.setValue(DoubleTensor.create(new double[]{1, 2, 3, 4, 5, 6, 7, 8}, new int[]{2, 2, 2}));

        SliceVertex dimenZeroFace = new SliceVertex(cube, 0, 0);
        Assert.assertArrayEquals(new double[]{1, 2, 3, 4}, dimenZeroFace.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 2}, dimenZeroFace.getShape());

        SliceVertex dimenOneFace = new SliceVertex(cube, 1, 0);
        Assert.assertArrayEquals(new double[]{1, 2, 5, 6}, dimenOneFace.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 2}, dimenOneFace.getShape());

        SliceVertex dimenTwoFace = new SliceVertex(cube, 2, 0);
        Assert.assertArrayEquals(new double[]{1, 3, 5, 7}, dimenTwoFace.getValue().asFlatDoubleArray(), 1e-6);
        Assert.assertArrayEquals(new int[]{2, 2}, dimenTwoFace.getShape());
    }

}
