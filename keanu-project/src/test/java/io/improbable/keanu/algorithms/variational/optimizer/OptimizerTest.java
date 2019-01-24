package io.improbable.keanu.algorithms.variational.optimizer;

import io.improbable.keanu.DeterministicRule;
import io.improbable.keanu.algorithms.variational.optimizer.gradient.AdamOptimizer;
import io.improbable.keanu.algorithms.variational.optimizer.gradient.GradientOptimizer;
import io.improbable.keanu.algorithms.variational.optimizer.gradient.SumGaussianTestCase;
import io.improbable.keanu.algorithms.variational.optimizer.nongradient.NonGradientOptimizer;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.vertices.dbl.probabilistic.GaussianVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Theories.class)
public class OptimizerTest {

    @Rule
    public DeterministicRule deterministicRule = new DeterministicRule();

    @AllArgsConstructor
    public enum OptimizerType {

        ADAM(bayesianNetwork -> {

            return AdamOptimizer.builder()
                .bayesianNetwork(new KeanuProbabilisticWithGradientGraph(bayesianNetwork))
                .build();
        }),

        APACHE_GRADIENT(KeanuOptimizer.Gradient::of),
        APACHE_BOBYQA(KeanuOptimizer.NonGradient::of);

        Function<BayesianNetwork, Optimizer> getOptimizer;
    }

    @AllArgsConstructor
    public enum FitnessFunction {
        MAP(Optimizer::maxAPosteriori),
        MLE(Optimizer::maxLikelihood);

        @Getter
        Function<Optimizer, OptimizedResult> getFitness;
    }

    @AllArgsConstructor
    public enum TestCase {
        SUM_GAUSSIAN(SumGaussianTestCase::new);

        private Supplier<GradientOptimizerTestCase> supplier;

    }

    @DataPoints
    public static OptimizerType[] getTypes() {
        return OptimizerType.values();
    }

    @DataPoints
    public static FitnessFunction[] getFitnessFunction() {
        return FitnessFunction.values();
    }

    @DataPoints
    public static TestCase[] getTestCase() {
        return TestCase.values();
    }

    @Theory
    public void canOptimize(OptimizerType type, FitnessFunction fitnessFunction, TestCase testCaseSupplier) {
        GradientOptimizerTestCase testCase = testCaseSupplier.supplier.get();
        BayesianNetwork model = testCase.getModel();
        Optimizer optimizer = type.getOptimizer.apply(model);
        OptimizedResult result = fitnessFunction.getFitness.apply(optimizer);

        if (fitnessFunction == FitnessFunction.MAP) {
            testCase.assertMAP(result);
        } else if (fitnessFunction == FitnessFunction.MLE) {
            testCase.assertMLE(result);
        }
    }

    @Test
    public void gradientOptimizerCanRemoveFitnessCalculationHandler() {
        GaussianVertex gaussianVertex = new GaussianVertex(0, 1);
        GradientOptimizer optimizer = KeanuOptimizer.Gradient.of(gaussianVertex.getConnectedGraph());
        canRemoveFitnessCalculationHandler(optimizer);
    }

    @Test
    public void nonGradientOptimizerCanRemoveFitnessCalculationHandler() {
        GaussianVertex A = new GaussianVertex(0, 1);
        GaussianVertex B = new GaussianVertex(0, 1);
        A.plus(B);
        NonGradientOptimizer optimizer = KeanuOptimizer.NonGradient.of(A.getConnectedGraph());
        canRemoveFitnessCalculationHandler(optimizer);
    }

    private void canRemoveFitnessCalculationHandler(Optimizer optimizer) {

        AtomicBoolean didCallFitness = new AtomicBoolean(false);

        BiConsumer<double[], Double> fitnessHandler = mock(BiConsumer.class);

        optimizer.addFitnessCalculationHandler(fitnessHandler);
        optimizer.removeFitnessCalculationHandler(fitnessHandler);

        optimizer.maxAPosteriori();

        assertFalse(didCallFitness.get());
        verifyNoMoreInteractions(fitnessHandler);
    }
}
