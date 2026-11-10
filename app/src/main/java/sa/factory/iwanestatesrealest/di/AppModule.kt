package sa.factory.iwanestatesrealest.di

import sa.factory.iwanestatesrealest.data.repository.IwanEstatesRepositoryImpl
import sa.factory.iwanestatesrealest.domain.repository.IwanEstatesRepository
import sa.factory.iwanestatesrealest.domain.usecase.*
import sa.factory.iwanestatesrealest.hardware.HardwareSensorDriver
import sa.factory.iwanestatesrealest.ui.viewmodel.IwanEstatesViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: IwanEstatesRepository = IwanEstatesRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): IwanEstatesViewModel = IwanEstatesViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
