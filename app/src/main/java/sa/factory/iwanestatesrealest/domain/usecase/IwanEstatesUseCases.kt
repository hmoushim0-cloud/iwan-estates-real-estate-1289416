package sa.factory.iwanestatesrealest.domain.usecase

import sa.factory.iwanestatesrealest.domain.repository.IwanEstatesRepository
import sa.factory.iwanestatesrealest.model.IwanEstatesItem
import sa.factory.iwanestatesrealest.model.IwanEstatesOrder
import sa.factory.iwanestatesrealest.model.IwanEstatesTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: IwanEstatesRepository) {
    operator fun invoke(): Flow<List<IwanEstatesItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: IwanEstatesRepository) {
    val cart = repository.activeCart
    fun add(item: IwanEstatesItem) = repository.addToCart(item)
    fun remove(item: IwanEstatesItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: IwanEstatesRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): IwanEstatesOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: IwanEstatesRepository) {
    operator fun invoke(): Flow<IwanEstatesTelemetry> = repository.telemetryFlow
}
