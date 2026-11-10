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
        IwanEstatesItem("1", "تحليل حركة الزوار ثلاثي الأبعاد", "تحليل تفاعلي لمسارات العملاء داخل المساحات الافتراضية باستخدام بيانات ليدار", 150.0, "معتمد", "تحليلات الزوار"),
        IwanEstatesItem("2", "تتبع معاملات البائعين الفوري", "لوحة تحكم في الوقت الفعلي لتحليل أداء البائعين عبر قنوات متعددة", 280.0, "فوري", "مسح ثلاثي الأبعاد"),
        IwanEstatesItem("3", "تحسين تخطيط المساحات", "خوارزميات ذكاء اصطناعي لزيادة كفاءة تخطيط المنتجات في المساحات ثلاثية الأبعاد", 95.0, "جديد", "تتبع المعاملات")
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
