package events


open class InvalidEventException(override val message: String) : Exception(message)

class InvalidEventOrderException : InvalidEventException(message = "Event cannot be applied in this order")
