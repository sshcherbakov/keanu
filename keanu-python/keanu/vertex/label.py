from py4j.java_gateway import java_import

from keanu.base import JavaObjectWrapper
from keanu.context import KeanuContext

k = KeanuContext()

java_import(k.jvm_view(), "io.improbable.keanu.vertices.VertexLabel")


class _VertexLabel(JavaObjectWrapper):

    def __init__(self, first: str, *remainder: str):
        if len(remainder) == 0:
            java_object = k.jvm_view().VertexLabel(first)
        else:
            java_object = k.jvm_view().VertexLabel(first, k.to_java_string_array(remainder))
        super(_VertexLabel, self).__init__(java_object)
