package sa.factory.iwanestatesrealest.domain.repository

import sa.factory.iwanestatesrealest.model.IwanEstatesItem
import sa.factory.iwanestatesrealest.model.IwanEstatesOrder
import sa.factory.iwanestatesrealest.model.IwanEstatesTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IwanEstatesRepository {
    val catalogFlow: Flow<List<IwanEstatesItem>>
    val activeCart: StateFlow<List<IwanEstatesItem>>
    val telemetryFlow: Flow<IwanEstatesTelemetry>
    
    fun addToCart(item: IwanEstatesItem)
    fun removeFromCart(item: IwanEstatesItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): IwanEstatesOrder
    suspend fun refreshCatalog()
}
