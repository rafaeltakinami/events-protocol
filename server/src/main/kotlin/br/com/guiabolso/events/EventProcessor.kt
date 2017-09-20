package br.com.guiabolso.events

import br.com.guiabolso.events.builder.EventBuilder.Companion.badProtocol
import br.com.guiabolso.events.builder.EventBuilder.Companion.errorFor
import br.com.guiabolso.events.builder.EventBuilder.Companion.notFoundFor
import br.com.guiabolso.events.exception.EventExceptionHandler
import br.com.guiabolso.events.handler.EventHandlerDiscovery
import br.com.guiabolso.events.input.InputEvent
import br.com.guiabolso.events.metric.CompositeMetricReporter
import br.com.guiabolso.events.metric.MDCMetricReporter
import br.com.guiabolso.events.metric.MetricReporter
import br.com.guiabolso.events.metric.NewrelicMetricReporter
import br.com.guiabolso.events.model.Event
import br.com.guiabolso.events.model.EventErrorType.Generic
import br.com.guiabolso.events.model.EventMessage
import br.com.guiabolso.events.model.RequestEvent
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.slf4j.LoggerFactory.getLogger

class EventProcessor(
        private val discovery: EventHandlerDiscovery,
        private val reporter: MetricReporter = CompositeMetricReporter(MDCMetricReporter(), NewrelicMetricReporter())) {

    companion object {
        private val logger = getLogger(EventProcessor::class.java)!!
        private val mapper = Gson()
    }

    private val handlers: MutableMap<Class<*>, EventExceptionHandler<*>> = mutableMapOf()

    fun <T : Exception> addExceptionHandler(exception: Class<T>, exceptionHandler: EventExceptionHandler<T>) {
        handlers.put(exception, exceptionHandler)
    }

    fun processEvent(rawEvent: String): Event {
        val event = parseAndValidateEvent(rawEvent)
        val handler = discovery.eventHandlerFor(event.name, event.version)

        return if (handler == null) {
            notFoundFor(event)
        } else {
            try {
                reporter.startProcessingEvent(event)
                handler.handle(event)
            } catch (e: Exception) {
                logger.error("Error processing event.", e)
                reporter.notifyError(e)
                errorFor(
                        event, Generic(),
                        EventMessage("UNHANDLED_ERROR", mapOf("message" to e.message, "exception" to getStackTrace(e)))
                )
            } finally {
                reporter.eventProcessFinished(event)
            }
        }
    }

    private fun parseAndValidateEvent(rawEvent: String): Event =
            try {
                val input = mapper.fromJson(rawEvent, InputEvent::class.java)
                RequestEvent(
                        name = input.name.required("name"),
                        version = input.version.required("version"),
                        id = input.id.required("id"),
                        flowId = input.flowId.required("flowId"),
                        payload = input.payload.required("payload"),
                        identity = input.identity ?: JsonObject(),
                        auth = input.auth ?: JsonObject(),
                        metadata = input.metadata ?: JsonObject()
                )
            } catch (e: IllegalArgumentException) {
                logger.error("Missing required property ${e.message}.", e)
                reporter.notifyError(e)
                badProtocol(EventMessage(
                        "INVALID_COMMUNICATION_PROTOCOL",
                        mapOf("missingProperty" to e.message)
                ))
            } catch (e: Exception) {
                logger.error("Error parsing event.", e)
                reporter.notifyError(e)
                badProtocol(EventMessage(
                        "INVALID_COMMUNICATION_PROTOCOL",
                        mapOf("message" to e.message, "exception" to getStackTrace(e))
                ))
            }
}

private fun <T> T?.required(name: String): T {
    return this ?: throw IllegalArgumentException(name)
}