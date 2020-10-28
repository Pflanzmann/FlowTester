package flowTester.exception

sealed class InternalError: Throwable() {
    object WrongStepTypeException : InternalError()
}