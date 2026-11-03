package sa.factory.iwanestatesrealest.data.repository

import sa.factory.iwanestatesrealest.domain.repository.IwanEstatesRepository
import sa.factory.iwanestatesrealest.hardware.HardwareSensorDriver
import sa.factory.iwanestatesrealest.model.IwanEstatesItem
import sa.factory.iwanestatesrealest.model.IwanEstatesOrder
import sa.factory.iwanestatesrealest.model.IwanEstatesTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IwanEstatesRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : IwanEstatesRepository {

    private val _catalog = MutableStateFlow<List<IwanEstatesItem>>(listOf(
        IwanEstatesItem("1", "تتبع حركة الزوار", "تحليل شامل لبيانات حركة الزوار وتوقيتهم في الموقع الإلكتروني", 150.0, "مطلوب", "المنتجات الإلكترونية"),
        IwanEstatesItem("2", "تحليل سلوك الشراء", "تحليل تفصيلي لسلوك الزوار وبيانتهم في مرحلة الشراء", 280.0, "مباشر", "الخدمات الإلكترونية"),
        IwanEstatesItem("3", "تتبع، تحليل وتحسين منصة التجارة الإلكترونية", "منظومة متكاملة تدمج تتبع وتحليل كل تفاعل وتحسين منصة التجارة الإلكترونية", 95.0, "مستقل", "الإحصائيات detteh")
    ))
    override val catalogFlow: Flow<List<IwanEstatesItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<IwanEstatesItem>>(emptyList())
    override val activeCart: StateFlow<List<IwanEstatesItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<IwanEstatesTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            IwanEstatesTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: IwanEstatesItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: IwanEstatesItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): IwanEstatesOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = IwanEstatesOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
