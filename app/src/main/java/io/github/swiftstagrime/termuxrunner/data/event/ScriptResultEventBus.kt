package io.github.swiftstagrime.termuxrunner.data.event

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecutionResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puente entre el BroadcastReceiver que recibe el resultado de Termux
 * (proceso que puede vivir fuera de cualquier Activity) y la UI en Compose,
 * que solo necesita suscribirse a [events] para mostrar la ventana emergente
 * con el resultado en cuanto la app esté en primer plano.
 *
 * Al ser @Singleton, Hilt entrega la misma instancia tanto al receiver/use case
 * que emite como al ViewModel que colecta.
 */
@Singleton
class ScriptResultEventBus
    @Inject
    constructor() {
        private val _events =
            MutableSharedFlow<ScriptExecutionResult>(
                replay = 0,
                extraBufferCapacity = 4,
            )
        val events: SharedFlow<ScriptExecutionResult> = _events.asSharedFlow()

        suspend fun emit(result: ScriptExecutionResult) {
            _events.emit(result)
        }
    }
