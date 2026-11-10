package sa.factory.iwanestatesrealest.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.iwanestatesrealest.domain.usecase.*
import sa.factory.iwanestatesrealest.model.IwanEstatesItem
import sa.factory.iwanestatesrealest.model.IwanEstatesOrder
import sa.factory.iwanestatesrealest.model.IwanEstatesTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class IwanEstatesUiState(
    val catalog: List<IwanEstatesItem> = emptyList(),
    val filteredCatalog: List<IwanEstatesItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<IwanEstatesItem> = emptyList(),
    val telemetry: IwanEstatesTelemetry? = null,
    val lastOrder: IwanEstatesOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class IwanEstatesViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IwanEstatesUiState())
    val uiState: StateFlow<IwanEstatesUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: IwanEstatesItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: IwanEstatesItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
