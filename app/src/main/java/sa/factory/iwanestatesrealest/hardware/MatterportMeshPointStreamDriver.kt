package sa.factory.iwanestatesrealest.hardware

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt

data class MatterportMeshVertex(
    val vertexId: Long,
    val xMm: Short,
    val yMm: Short,
    val zMm: Short,
    val normalX: Double,
    val normalY: Double,
    val normalZ: Double,
    val surfaceClassification: Int
)

data class MatterportMeshFrame(
    val sequenceNumber: Int,
    val vertex: MatterportMeshVertex,
    val isValidCrc: Boolean
)

data class ArchitecturalMeshBoundingSummary(
    val widthMeters: Double,
    val lengthMeters: Double,
    val ceilingHeightMeters: Double,
    val grossFloorAreaSqM: Double,
    val grossRoomVolumeM3: Double,
    val isWallSurface: Boolean,
    val isHorizontalPlane: Boolean
)

class MatterportMeshPointStreamDriver {

    companion object {
        const val MAGIC_HEADER: Short = 0x4D50.toShort() // "MP"
        const val PROTOCOL_VERSION: Byte = 0x03
        const val FRAME_SIZE_BYTES: Int = 20

        const val CLASS_UNCLASSIFIED = 0
        const val CLASS_FLOOR = 1
        const val CLASS_CEILING = 2
        const val CLASS_BEARING_WALL = 3
        const val CLASS_WINDOW_GLAZING = 4
    }

    fun computeCrc16Ccitt(data: ByteArray, offset: Int, length: Int): Short {
        var crc = 0xFFFF
        for (i in offset until (offset + length)) {
            val b = data[i].toInt() and 0xFF
            crc = crc xor (b shl 8)
            for (bit in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return (crc and 0xFFFF).toShort()
    }

    fun serializeVertexFrame(
        sequenceNumber: Int,
        vertexId: Long,
        xMm: Short,
        yMm: Short,
        zMm: Short,
        normalX: Double,
        normalY: Double,
        normalZ: Double,
        classification: Int
    ): ByteArray {
        val buffer = ByteBuffer.allocate(FRAME_SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(MAGIC_HEADER)
        buffer.put(PROTOCOL_VERSION)
        buffer.put((sequenceNumber and 0xFF).toByte())

        buffer.putInt((vertexId and 0xFFFFFFFFL).toInt())

        buffer.putShort(xMm)
        buffer.putShort(yMm)
        buffer.putShort(zMm)

        val nx = (normalX * 127.0).roundToInt().coerceIn(-128, 127).toByte()
        val ny = (normalY * 127.0).roundToInt().coerceIn(-128, 127).toByte()
        val nz = (normalZ * 127.0).roundToInt().coerceIn(-128, 127).toByte()

        buffer.put(nx)
        buffer.put(ny)
        buffer.put(nz)

        buffer.put((classification and 0xFF).toByte())

        val crc = computeCrc16Ccitt(buffer.array(), 0, FRAME_SIZE_BYTES - 2)
        buffer.putShort(crc)

        return buffer.array()
    }

    fun parseVertexFrame(packet: ByteArray): MatterportMeshFrame {
        if (packet.size < FRAME_SIZE_BYTES) {
            throw IllegalArgumentException("Packet too short for Matterport Mesh frame: ${packet.size} bytes")
        }

        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.short
        if (magic != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid Matterport magic: 0x${Integer.toHexString(magic.toInt() and 0xFFFF)}")
        }

        val version = buffer.get()
        if (version != PROTOCOL_VERSION) {
            throw IllegalArgumentException("Unsupported Matterport version: $version")
        }

        val seq = buffer.get().toInt() and 0xFF
        val vertexId = buffer.int.toLong() and 0xFFFFFFFFL

        val x = buffer.short
        val y = buffer.short
        val z = buffer.short

        val nx = buffer.get().toDouble() / 127.0
        val ny = buffer.get().toDouble() / 127.0
        val nz = buffer.get().toDouble() / 127.0

        val classification = buffer.get().toInt() and 0xFF

        val packetCrc = buffer.short
        val computedCrc = computeCrc16Ccitt(packet, 0, FRAME_SIZE_BYTES - 2)
        val isValidCrc = packetCrc == computedCrc

        val vertex = MatterportMeshVertex(
            vertexId = vertexId,
            xMm = x,
            yMm = y,
            zMm = z,
            normalX = (nx * 100.0).roundToInt() / 100.0,
            normalY = (ny * 100.0).roundToInt() / 100.0,
            normalZ = (nz * 100.0).roundToInt() / 100.0,
            surfaceClassification = classification
        )

        return MatterportMeshFrame(
            sequenceNumber = seq,
            vertex = vertex,
            isValidCrc = isValidCrc
        )
    }

    fun analyzeMeshCluster(vertices: List<MatterportMeshVertex>): ArchitecturalMeshBoundingSummary {
        if (vertices.isEmpty()) {
            return ArchitecturalMeshBoundingSummary(0.0, 0.0, 0.0, 0.0, 0.0, false, false)
        }

        var minX = vertices[0].xMm.toInt()
        var maxX = vertices[0].xMm.toInt()
        var minY = vertices[0].yMm.toInt()
        var maxY = vertices[0].yMm.toInt()
        var minZ = vertices[0].zMm.toInt()
        var maxZ = vertices[0].zMm.toInt()

        var sumNz = 0.0
        for (v in vertices) {
            if (v.xMm < minX) minX = v.xMm.toInt()
            if (v.xMm > maxX) maxX = v.xMm.toInt()
            if (v.yMm < minY) minY = v.yMm.toInt()
            if (v.yMm > maxY) maxY = v.yMm.toInt()
            if (v.zMm < minZ) minZ = v.zMm.toInt()
            if (v.zMm > maxZ) maxZ = v.zMm.toInt()
            sumNz += abs(v.normalZ)
        }

        val widthM = (maxX - minX) / 1000.0
        val lengthM = (maxY - minY) / 1000.0
        val heightM = (maxZ - minZ) / 1000.0
        val area = widthM * lengthM
        val volume = area * heightM

        val avgNz = sumNz / vertices.size
        val isHorizontal = avgNz > 0.80
        val isWall = avgNz < 0.25

        return ArchitecturalMeshBoundingSummary(
            widthMeters = (widthM * 100.0).roundToInt() / 100.0,
            lengthMeters = (lengthM * 100.0).roundToInt() / 100.0,
            ceilingHeightMeters = (heightM * 100.0).roundToInt() / 100.0,
            grossFloorAreaSqM = (area * 10.0).roundToInt() / 10.0,
            grossRoomVolumeM3 = (volume * 10.0).roundToInt() / 10.0,
            isWallSurface = isWall,
            isHorizontalPlane = isHorizontal
        )
    }
}
