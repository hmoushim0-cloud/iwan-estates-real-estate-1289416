package sa.factory.iwanestatesrealest.data.remote

import sa.factory.iwanestatesrealest.model.IwanEstatesItem
import sa.factory.iwanestatesrealest.model.IwanEstatesOrder

interface IwanEstatesApiService {
    suspend fun fetchCatalog(): List<IwanEstatesItem>
    suspend fun submitOrder(order: IwanEstatesOrder): Boolean
}
