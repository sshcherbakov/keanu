package io.improbable.keanu.codegen.python.vertex;

import com.google.common.collect.ImmutableList;
import com.google.gson.internal.Primitives;
import io.improbable.keanu.codegen.python.PythonParam;
import io.improbable.keanu.tensor.bool.BooleanTensor;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.tensor.intgr.IntegerTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.bool.BooleanVertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.intgr.IntegerVertex;
import org.apache.commons.lang3.NotImplementedException;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Apache FreeMarker Format:
 * def {ClassName}({TypedParams}) -> Vertex:
 *      return {ChildClassName}({CastedParams})
 *
 * e.g.
 * Java method signature:
 * public GaussianVertex(DoubleVertex mu, DoubleVertex sigma)
 *
 * Python:
 * def Gaussian(mu: vertex_constructor_param_types, sigma: vertex_constructor_param_types, label: Optional[str]=None) -> Vertex:
 *     return Double(context.jvm_view().GaussianVertex, cast_to_double_vertex(mu), cast_to_double_vertex(sigma), optional_label=label)
 */
class JavaVertexToPythonConverter {

    private static final List<PythonParam> EXTRA_PARAMS = ImmutableList.of(
        new PythonParam("label", String.class, "None")
    );
    private Constructor javaConstructor;
    private List<PythonParam> allParams;
    private List<PythonParam> commonParams;

    JavaVertexToPythonConverter(Constructor javaConstructor, Reflections reflections) {
       this.javaConstructor = javaConstructor;
       createListOfPythonVertexParam(javaConstructor, reflections);
    }

    private void createListOfPythonVertexParam(Constructor javaConstructor, Reflections reflections) {
        List<String> paramNames = reflections.getConstructorParamNames(javaConstructor);
        List<Class> paramTypes = Arrays.asList(javaConstructor.getParameterTypes());

        this.allParams = new ArrayList<>();
        this.commonParams = new ArrayList<>();

        for (int i = 0; i < javaConstructor.getParameterCount(); i++) {
            this.allParams.add(new PythonParam(paramNames.get(i), paramTypes.get(i)));
        }

        for (PythonParam extraParam : EXTRA_PARAMS) {
            if (!paramNames.contains(extraParam.getName())) {
                this.commonParams.add(extraParam);
            }
        }
    }

    String getClassName() {
        return javaConstructor.getDeclaringClass().getSimpleName().replaceAll("Vertex$", "");
    }

    String getChildClassName() {
        Class<?> javaClass = javaConstructor.getDeclaringClass();
        if (DoubleVertex.class.isAssignableFrom(javaClass)) {
            return "Double";
        } else if (IntegerVertex.class.isAssignableFrom(javaClass)) {
            return "Integer";
        } else if (BooleanVertex.class.isAssignableFrom(javaClass)) {
            return "Boolean";
        } else {
            return "Vertex";
        }
    }

    String getTypedParams() {
        String[] pythonParams = new String[allParams.size() + commonParams.size()];

        for (int i = 0; i < allParams.size(); i++) {
            PythonParam param = allParams.get(i);
            pythonParams[i] = param.getName() + ": " + toTypedParam(param.getKlass());
        }

        for (int i = 0; i < commonParams.size(); i++) {
            PythonParam param = commonParams.get(i);
            pythonParams[allParams.size() + i] = param.getName() + ": Optional[" + toTypedParam(param.getKlass()) + "]=" + param.getDefaultValue();
        }

        return String.join(", ", pythonParams);
    }

    private String toTypedParam(Class<?> parameterClass) {
        Class parameterType = Primitives.wrap(parameterClass);

        if (Vertex.class.isAssignableFrom(parameterType)) {
            return "vertex_constructor_param_types";
        } else if (DoubleTensor.class.isAssignableFrom(parameterType) ||
            IntegerTensor.class.isAssignableFrom(parameterType) ||
            BooleanTensor.class.isAssignableFrom(parameterType)) {
            return "tensor_arg_types";
        } else if (Double.class.isAssignableFrom(parameterType)) {
            return "float";
        } else if (Boolean.class.isAssignableFrom(parameterType)) {
            return "bool";
        } else if (Integer.class.isAssignableFrom(parameterType) || Long.class.isAssignableFrom(parameterType)) {
            return "int";
        } else if (String.class.isAssignableFrom(parameterType)) {
            return "str";
        } else if (Long[].class.isAssignableFrom(parameterType) || Integer[].class.isAssignableFrom(parameterType) ||
            long[].class.isAssignableFrom(parameterType) || int[].class.isAssignableFrom(parameterType)) {
            return "Collection[int]";
        } else if (Vertex[].class.isAssignableFrom(parameterType)) {
            return "Collection[Vertex]";
        } else {
            throw new NotImplementedException(String.format("Mapping from Java type %s is not defined.", parameterType.getName()));
        }
    }

    String getCastedParams() {
        String[] pythonParams = new String[EXTRA_PARAMS.size() + allParams.size()];

        for (int i = 0; i < EXTRA_PARAMS.size(); i++) {
            PythonParam param = EXTRA_PARAMS.get(i);
            pythonParams[i] = toCastedParam(param.getName(), param.getKlass());
        }

        for (int i = 0; i < allParams.size(); i++) {
            PythonParam param = allParams.get(i);
            pythonParams[EXTRA_PARAMS.size() + i] = toCastedParam(param.getName(), param.getKlass());
        }

        return String.join(", ", pythonParams);
    }

    private String toCastedParam(String pythonParameter, Class<?> parameterClass) {
        Class parameterType = Primitives.wrap(parameterClass);

        if (DoubleVertex.class.isAssignableFrom(parameterType)) {
            return "cast_to_double_vertex(" + pythonParameter + ")";
        } else if (IntegerVertex.class.isAssignableFrom(parameterType)) {
            return "cast_to_integer_vertex(" + pythonParameter + ")";
        } else if (BooleanVertex.class.isAssignableFrom(parameterType)) {
            return "cast_to_boolean_vertex(" + pythonParameter + ")";
        } else if (Vertex.class.isAssignableFrom(parameterType)) {
            return "cast_to_vertex(" + pythonParameter + ")";
        } else if (DoubleTensor.class.isAssignableFrom(parameterType)) {
            return "cast_to_double_tensor(" + pythonParameter + ")";
        } else if (IntegerTensor.class.isAssignableFrom(parameterType)) {
            return "cast_to_integer_tensor(" + pythonParameter + ")";
        } else if (BooleanTensor.class.isAssignableFrom(parameterType)) {
            return "cast_to_boolean_tensor(" + pythonParameter + ")";
        } else if (Double.class.isAssignableFrom(parameterType)) {
            return "cast_to_double(" + pythonParameter + ")";
        } else if (Integer.class.isAssignableFrom(parameterType) || Long.class.isAssignableFrom(parameterType)) {
            return "cast_to_integer(" + pythonParameter + ")";
        } else if (String.class.isAssignableFrom(parameterType)) {
            return pythonParameter;
        } else if (Boolean.class.isAssignableFrom(parameterType)) {
            return "cast_to_boolean(" + pythonParameter + ")";
        } else if (Long[].class.isAssignableFrom(parameterType) || long[].class.isAssignableFrom(parameterType)) {
            return "cast_to_long_array(" + pythonParameter + ")";
        } else if (Integer[].class.isAssignableFrom(parameterType) || int[].class.isAssignableFrom(parameterType)) {
            return "cast_to_int_array(" + pythonParameter + ")";
        } else if (Vertex[].class.isAssignableFrom(parameterType)) {
            return "cast_to_vertex_array(" + pythonParameter + ")";
        } else {
            throw new IllegalArgumentException("Failed to Encode " + pythonParameter + " of type: " + parameterType);
        }
    }
}