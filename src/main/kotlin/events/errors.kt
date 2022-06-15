package events


open class InvalidEventException(override val message: String = "Invalid event") : Exception(message)

class InvalidEventOrderException : InvalidEventException(message = "Event cannot be applied in this order")

class MultipleEventsException : InvalidEventException(message = "Cannot receive multiple events for the same policy, in the same month")
