package sa.factory.iwanestatesrealest.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.iwanestatesrealest.spatial.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "360 LiDAR Spatial Scanner + Matterport Pro3"
    private val meshDriver = MatterportMeshPointStreamDriver()
    private val spatialEngine = IwanArchitecturalSpatialEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Matterport Pro3 LiDAR 3D Photogrammetric Mesh Stream initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "Matterport Pro3 360 LiDAR: ONLINE | MESH PROTOCOL v3 READY [0x4D50]"))

        var sequence = 100
        while (true) {
            delay(1500)
            sequence++

            // Sample vertex on luxury villa salon wall
            val packet = meshDriver.serializeVertexFrame(
                sequenceNumber = sequence,
                vertexId = 409612L + sequence,
                xMm = 6500.toShort(),
                yMm = 8000.toShort(),
                zMm = 3200.toShort(),
                normalX = 0.0,
                normalY = 0.0,
                normalZ = 1.0, // Floor plane
                classification = MatterportMeshPointStreamDriver.CLASS_FLOOR
            )

            val frame = meshDriver.parseVertexFrame(packet)

            val payload = String.format(
                "Matterport Pro3 Frame #%d: Vertex #%d | Pos(%.2f, %.2f, %.2f m) | Normal(%.2f, %.2f, %.2f) | Class: %d | CRC: %s",
                frame.sequenceNumber,
                frame.vertex.vertexId,
                frame.vertex.xMm / 1000.0,
                frame.vertex.yMm / 1000.0,
                frame.vertex.zMm / 1000.0,
                frame.vertex.normalX,
                frame.vertex.normalY,
                frame.vertex.normalZ,
                frame.vertex.surfaceClassification,
                if (frame.isValidCrc) "VALID" else "INVALID"
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
