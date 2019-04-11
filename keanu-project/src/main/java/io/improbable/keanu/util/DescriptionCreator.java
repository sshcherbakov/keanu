package io.improbable.keanu.util;

import io.improbable.keanu.tensor.Tensor;
import io.improbable.keanu.vertices.LoadVertexParam;
import io.improbable.keanu.vertices.SaveVertexParam;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.bool.BooleanVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.BooleanIfVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.AndBinaryVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.BooleanBinaryOpVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.OrBinaryVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.EqualsVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanOrEqualVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.LessThanOrEqualVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.LessThanVertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.NotEqualsVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.DoubleIfVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary.AdditionVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary.DifferenceVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary.DivisionVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary.MultiplicationVertex;
import io.improbable.keanu.vertices.intgr.nonprobabilistic.IntegerIfVertex;
import io.improbable.keanu.vertices.intgr.nonprobabilistic.operators.binary.IntegerAdditionVertex;
import io.improbable.keanu.vertices.intgr.nonprobabilistic.operators.binary.IntegerDifferenceVertex;
import io.improbable.keanu.vertices.intgr.nonprobabilistic.operators.binary.IntegerMultiplicationVertex;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class DescriptionCreator {

    private Map<Class, String> delimiters;

    public DescriptionCreator() {
        delimiters = new HashMap<>();
        delimiters.put(AdditionVertex.class, " + ");
        delimiters.put(IntegerAdditionVertex.class, " + ");
        delimiters.put(DifferenceVertex.class, " - ");
        delimiters.put(IntegerDifferenceVertex.class, " - ");
        delimiters.put(MultiplicationVertex.class, " * ");
        delimiters.put(IntegerMultiplicationVertex.class, " * ");
        delimiters.put(DivisionVertex.class, " / ");
        delimiters.put(AndBinaryVertex.class, " && ");
        delimiters.put(EqualsVertex.class, " == ");
        delimiters.put(GreaterThanOrEqualVertex.class, " >= ");
        delimiters.put(GreaterThanVertex.class, " > ");
        delimiters.put(LessThanOrEqualVertex.class, " <= ");
        delimiters.put(LessThanVertex.class, " < ");
        delimiters.put(NotEqualsVertex.class, " != ");
        delimiters.put(OrBinaryVertex.class, " || ");
    }

    /**
     * This method constructs an equation to describe how a vertex is calculated.
     * The description is generated by recursively stepping up through the BayesNet and generating descriptions.
     * Descriptions of common vertices will use infix operators.
     * Descriptions will not recurse any further than labelled vertices.
     *
     * It is suggested that to use this feature, you label as many vertices as possible to avoid complex descriptions.
     * @param vertex The vertex to create the description of
     * @return An String equation describing how this vertex is calculated.<br>
     * E.g. "This Vertex = that + (three * Const(4))"
     */
    public String createDescription(Vertex<?> vertex) {
        Collection<Vertex> parents = vertex.getParents();

        if (parents.size() == 0) {
            StringBuilder builder = new StringBuilder("This Vertex = ");
            builder.append(getLeafDescription(vertex));
            return builder.toString();
        }

        String thisLabel = vertex.getLabel() != null ? vertex.getLabel().toString() : "This Vertex";

        return thisLabel + " = " + generateDescription(vertex, false, false);
    }

    private String generateDescription(Vertex<?> vertex, boolean allowLabels, boolean includeBrackets) {
        if (allowLabels && vertex.getLabel() != null) {
            return vertex.getLabel().toString();
        }

        Collection<Vertex> parents = vertex.getParents();

        if (parents.size() == 0) {
            return getLeafDescription(vertex);
        }

        Optional<String> irregularDescription = checkForIrregularExpressions(vertex, includeBrackets);
        if (irregularDescription.isPresent()) {
            return irregularDescription.get();
        }

        if (delimiters.containsKey(vertex.getClass())) {
            CharSequence delimiter = delimiters.get(vertex.getClass());
            return getDelimiterVertexDescription(vertex, delimiter, includeBrackets);
        }

        Optional<String> saveLoadDescription = tryCreateDescriptionFromSaveLoadAnnotations(vertex, includeBrackets);
        return saveLoadDescription.orElseGet(() -> getDelimiterVertexDescription(vertex, ", ", includeBrackets));

    }

    private String getDelimiterVertexDescription(Vertex<?> vertex, CharSequence delimiter, boolean includeBrackets) {
        Stream<String> parentStream = vertex
            .getParents()
            .stream()
            .map(parent -> generateDescription(parent, true, true));

        String[] parentStrings = parentStream.toArray(String[]::new);

        StringBuilder builder = new StringBuilder();

        if (includeBrackets) {
            builder.append("(");
        }
        builder.append(String.join(delimiter, parentStrings));
        if (includeBrackets) {
            builder.append(")");
        }
        return builder.toString();
    }

    private static String getLeafDescription(Vertex<?> vertex) {
        if (vertex.getLabel() != null) {
            return vertex.getLabel().toString();
        }
        StringBuilder builder = new StringBuilder();
        Optional<String> scalarValue = tryGetScalarValue(vertex);
        if (scalarValue.isPresent()) {
            builder.append("Const(");
            builder.append(scalarValue.get());
            builder.append(")");
        } else {
            builder.append(vertex.getClass().getSimpleName());
            builder.append(" with shape: ");
            builder.append(Arrays.toString(vertex.getShape()));
        }
        return builder
            .toString();
    }

    private static Optional<String> tryGetScalarValue(Vertex<?> vertex) {
        Tensor tensor = (Tensor) vertex.getValue();
        if (tensor.isScalar()) {
            return Optional.of(tensor.scalar().toString());
        }
        return Optional.empty();
    }

    private Optional<String> checkForIrregularExpressions(Vertex<?> vertex, boolean includeBrackets) {
        if (vertex instanceof BooleanIfVertex || vertex instanceof DoubleIfVertex || vertex instanceof IntegerIfVertex) {
            Optional<String> description = createIfStringDescription(vertex, includeBrackets);
            if (description.isPresent()) {
                return Optional.of(description.get());
            }
        } else if (vertex instanceof BooleanBinaryOpVertex) {
            String booleanBinaryDescription = createBooleanBinaryOpDescription(
                (BooleanBinaryOpVertex) vertex,
                delimiters.getOrDefault(vertex.getClass(), ", "),
                includeBrackets);
            return Optional.of(booleanBinaryDescription);
        }

        return Optional.empty();
    }

    private Optional<String> createIfStringDescription(Vertex<?> vertex, boolean includeBrackets) {
        BooleanVertex predicate;
        Vertex<?> thn;
        Vertex<?> els;

        if (vertex instanceof IntegerIfVertex) {
            predicate = ((IntegerIfVertex) vertex).getPredicate();
            thn = ((IntegerIfVertex) vertex).getThn();
            els = ((IntegerIfVertex) vertex).getEls();
        } else if (vertex instanceof BooleanIfVertex) {
            predicate = ((BooleanIfVertex) vertex).getPredicate();
            thn = ((BooleanIfVertex) vertex).getThn();
            els = ((BooleanIfVertex) vertex).getEls();
        } else if (vertex instanceof DoubleIfVertex) {
            predicate = ((DoubleIfVertex) vertex).getPredicate();
            thn = ((DoubleIfVertex) vertex).getThn();
            els = ((DoubleIfVertex) vertex).getEls();
        } else {
            return Optional.empty();
        }

        StringBuilder builder = new StringBuilder();

        if (includeBrackets) {
            builder.append("(");
        }
        builder.append(generateDescription(predicate, true, includeBrackets));
        builder.append(" ? ");
        builder.append(generateDescription(thn, true, includeBrackets));
        builder.append(" : ");
        builder.append(generateDescription(els, true, includeBrackets));
        if (includeBrackets) {
            builder.append(")");
        }

        return Optional.of(builder.toString());
    }

    private Optional<String> tryCreateDescriptionFromSaveLoadAnnotations(Vertex vertex, boolean includeBrackets) {
        Optional<Constructor<?>> vertexConstructor = Arrays.stream(vertex.getClass().getConstructors())
            .filter(constructor -> Arrays.stream(constructor.getParameterAnnotations())
                .anyMatch(annotationsList -> annotationsList.length > 0))
            .findFirst();

        if (!vertexConstructor.isPresent()) {
            return Optional.empty();
        }

        StringBuilder builder = new StringBuilder(includeBrackets ? "(" : "");
        String vertexName = vertex.getClass().getSimpleName();
        builder.append(vertexName);
        builder.append("(");

        String[] loadVertexParams = Arrays.stream(vertexConstructor.get().getParameterAnnotations())
            .map(DescriptionCreator::tryGetLoadVertexParamAnnotation)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(LoadVertexParam::value)
            .toArray(String[]::new);

        if (loadVertexParams.length == 0) {
            return Optional.empty();
        }

        for (String paramName : loadVertexParams) {
            appendParamToBuilder(paramName, vertex, builder);
        }

        builder.delete(builder.length() - 2, builder.length());

        return Optional.of(builder.append(")")
            .append(includeBrackets ? ")" : "")
            .toString());
    }

    private void appendParamToBuilder(String paramName, Vertex<?> parentVertex, StringBuilder builder) {
        try {
            Optional<Vertex<?>> paramVertex = getSaveVertexParamVertex(paramName, parentVertex);
            if (paramVertex.isPresent()) {
                builder.append(paramName).append("=");
                builder.append(generateDescription(paramVertex.get(), true, true));
                builder.append(", ");
            }
        } catch (Exception ignored) {}
    }

    private static Optional<LoadVertexParam> tryGetLoadVertexParamAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations)
            .filter(annotation -> annotation.annotationType().equals(LoadVertexParam.class))
            .map(annotation -> (LoadVertexParam) annotation)
            .findAny();
    }

    private Optional<Vertex<?>> getSaveVertexParamVertex(String saveVertexParamName, Vertex parentVertex) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Optional<Method> retrievedMethod = Arrays.stream(parentVertex.getClass().getMethods())
            .filter(method ->
                method.isAnnotationPresent(SaveVertexParam.class)
                    && method.getAnnotation(SaveVertexParam.class).value().equals(saveVertexParamName))
            .findFirst();
        if (!retrievedMethod.isPresent()) {
            throw new NullPointerException("");
        }
        Object retrievedVertex = retrievedMethod.get().invoke(parentVertex);
        if (retrievedVertex instanceof Vertex) {
            return Optional.of((Vertex<?>) retrievedVertex);
        }
        return Optional.empty();
    }

    private String createBooleanBinaryOpDescription(BooleanBinaryOpVertex<?, ?> opVertex, String operation, boolean includeBrackets) {
        StringBuilder builder = new StringBuilder();

        if (includeBrackets) {
            builder.append("(");
        }

        builder.append(generateDescription(opVertex.getA(), true, includeBrackets));
        builder.append(operation);
        builder.append(generateDescription(opVertex.getB(), true, includeBrackets));

        if (includeBrackets) {
            builder.append(")");
        }
        return builder.toString();
    }
}
