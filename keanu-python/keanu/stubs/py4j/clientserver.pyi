# Stubs for py4j.clientserver (Python 3.6)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from py4j.java_gateway import CallbackServer, CallbackServerParameters, GatewayClient, GatewayConnectionGuard, GatewayParameters, JavaGateway
from threading import Thread
from typing import Any, Optional

logger: Any
SHUTDOWN_FINALIZER_WORKER: str
DEFAULT_WORKER_SLEEP_TIME: int

class FinalizerWorker(Thread):
    deque: Any = ...
    def __init__(self, deque: Any) -> None: ...
    def run(self) -> None: ...

class JavaParameters(GatewayParameters):
    auto_gc: Any = ...
    daemonize_memory_management: Any = ...
    def __init__(self, address: Any = ..., port: Any = ..., auto_field: bool = ..., auto_close: bool = ..., auto_convert: bool = ..., eager_load: bool = ..., ssl_context: Optional[Any] = ..., enable_memory_management: bool = ..., auto_gc: bool = ..., read_timeout: Optional[Any] = ..., daemonize_memory_management: bool = ..., auth_token: Optional[Any] = ...) -> None: ...

class PythonParameters(CallbackServerParameters):
    auto_gc: Any = ...
    def __init__(self, address: Any = ..., port: Any = ..., daemonize: bool = ..., daemonize_connections: bool = ..., eager_load: bool = ..., ssl_context: Optional[Any] = ..., auto_gc: bool = ..., accept_timeout: Any = ..., read_timeout: Optional[Any] = ..., propagate_java_exceptions: bool = ..., auth_token: Optional[Any] = ...) -> None: ...

class JavaClient(GatewayClient):
    java_parameters: Any = ...
    python_parameters: Any = ...
    thread_connection: Any = ...
    finalizer_deque: Any = ...
    def __init__(self, java_parameters: Any, python_parameters: Any, gateway_property: Optional[Any] = ..., finalizer_deque: Optional[Any] = ...) -> None: ...
    def garbage_collect_object(self, target_id: Any, enqueue: bool = ...) -> None: ...
    def set_thread_connection(self, connection: Any) -> None: ...
    def shutdown_gateway(self) -> None: ...
    def get_thread_connection(self): ...

class ClientServerConnectionGuard(GatewayConnectionGuard):
    def __exit__(self, type: Any, value: Any, traceback: Any) -> None: ...

class PythonServer(CallbackServer):
    java_parameters: Any = ...
    python_parameters: Any = ...
    gateway_property: Any = ...
    def __init__(self, java_client: Any, java_parameters: Any, python_parameters: Any, gateway_property: Any) -> None: ...

class ClientServerConnection:
    java_parameters: Any = ...
    python_parameters: Any = ...
    address: Any = ...
    port: Any = ...
    java_address: Any = ...
    java_port: Any = ...
    python_address: Any = ...
    python_port: Any = ...
    ssl_context: Any = ...
    socket: Any = ...
    stream: Any = ...
    gateway_property: Any = ...
    pool: Any = ...
    is_connected: bool = ...
    java_client: Any = ...
    python_server: Any = ...
    initiated_from_client: bool = ...
    def __init__(self, java_parameters: Any, python_parameters: Any, gateway_property: Any, java_client: Any, python_server: Optional[Any] = ...) -> None: ...
    def connect_to_java_server(self) -> None: ...
    def init_socket_from_python_server(self, socket: Any, stream: Any) -> None: ...
    def shutdown_gateway(self) -> None: ...
    def start(self) -> None: ...
    def run(self) -> None: ...
    def send_command(self, command: Any): ...
    def close(self, reset: bool = ...): ...
    def wait_for_commands(self) -> None: ...

class ClientServer(JavaGateway):
    java_parameters: Any = ...
    python_parameters: Any = ...
    def __init__(self, java_parameters: Optional[Any] = ..., python_parameters: Optional[Any] = ..., python_server_entry_point: Optional[Any] = ...) -> None: ...
