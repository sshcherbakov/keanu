# Stubs for py4j.finalizer (Python 3.6)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from typing import Any

class ThreadSafeFinalizer:
    finalizers: Any = ...
    lock: Any = ...
    @classmethod
    def add_finalizer(cls, id: Any, weak_ref: Any) -> None: ...
    @classmethod
    def remove_finalizer(cls, id: Any) -> None: ...
    @classmethod
    def clear_finalizers(cls, clear_all: bool = ...) -> None: ...

class Finalizer:
    finalizers: Any = ...
    @classmethod
    def add_finalizer(cls, id: Any, weak_ref: Any) -> None: ...
    @classmethod
    def remove_finalizer(cls, id: Any) -> None: ...
    @classmethod
    def clear_finalizers(cls, clear_all: bool = ...) -> None: ...

def clear_finalizers(clear_all: bool = ...) -> None: ...
