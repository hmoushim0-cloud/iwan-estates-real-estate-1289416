package sa.factory.iwanestatesrealest

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt
import sa.factory.iwanestatesrealest.hardware.MatterportMeshPointStreamDriver
import sa.factory.iwanestatesrealest.hardware.MatterportMeshVertex
import sa.factory.iwanestatesrealest.spatial.*

class IwanEstatesFeatureTest {

    private lateinit var spatialEngine: IwanArchitecturalSpatialEngine
    private lateinit var meshDriver: MatterportMeshPointStreamDriver

    @Before
    fun setUp() {
        spatialEngine = IwanArchitecturalSpatialEngine()
        meshDriver = MatterportMeshPointStreamDriver()
    }

    @Test
    fun testShoelacePolygonAreaCalculation() {
        // Rectangular Majlis: 8m (X: 0..8) by 6m (Y: 0..6) = 48.0 sq meters
        val rectPoints = listOf(
            Pair(0.0, 0.0),
            Pair(8.0, 0.0),
            Pair(8.0, 6.0),
            Pair(0.0, 6.0)
        )
        val area = spatialEngine.computePolygonAreaSqM(rectPoints)
        assertEquals(48.0, area, 0.01)

        // L-shaped room: 10x10 square minus 4x4 notch = 100 - 16 = 84.0 sq meters
        val lShapePoints = listOf(
            Pair(0.0, 0.0),
            Pair(10.0, 0.0),
            Pair(10.0, 6.0),
            Pair(6.0, 6.0),
            Pair(6.0, 10.0),
            Pair(0.0, 10.0)
        )
        val lArea = spatialEngine.computePolygonAreaSqM(lShapePoints)
        assertEquals(84.0, lArea, 0.01)
    }

    @Test
    fun testTaqeemValuationAssessment() {
        val majlis = ArchitecturalRoomPolygon(
            roomId = "RM-01",
            spaceType = ArchitecturalSpaceType.MAJLIS_MEN,
            boundaryPoints = listOf(Pair(0.0, 0.0), Pair(8.0, 0.0), Pair(8.0, 6.0), Pair(0.0, 6.0)), // 48 sqm
            ceilingHeightMeters = 3.60
        )

        val suite = ArchitecturalRoomPolygon(
            roomId = "RM-02",
            spaceType = ArchitecturalSpaceType.MASTER_BEDROOM_SUITE,
            boundaryPoints = listOf(Pair(0.0, 0.0), Pair(7.0, 0.0), Pair(7.0, 5.0), Pair(0.0, 5.0)), // 35 sqm
            ceilingHeightMeters = 3.20
        )

        val params = PropertyValuationParameters(
            districtNameArabic = "حي حطين - الرياض",
            baseSquareMeterPriceSar = 8500.0,
            streetWidthMeters = 20.0, // 8m over baseline 12m -> +4.0% bonus
            finishingGrade = PropertyFinishingGrade.ULTRA_LUXURY_CUSTOM, // 1.45x
            facade = FacadeOrientation.NORTHERN_COOL, // 1.05x
            rooms = listOf(majlis, suite)
        )

        val result = spatialEngine.calculateTaqeemValuation(params)
        assertEquals(83.0, result.totalBuiltUpAreaSqM, 0.1) // 48 + 35 = 83 sqm
        assertEquals(284.8, result.totalCubicVolumeM3, 0.5) // (48 * 3.6) + (35 * 3.2) = 172.8 + 112.0 = 284.8 m3
        assertTrue(result.estimatedMarketValueSar > 1000000.0) // > 1M SAR
        assertTrue(result.annualEjarRentalBenchmarkSar > 80000.0)
        assertTrue(result.wafiOffPlanEscrowTargetSar > 700000.0)
        assertTrue(result.valuationSummaryArabic.contains("حي حطين"))
    }

    @Test
    fun testStreetWidthAndFacadeMultipliers() {
        val room = ArchitecturalRoomPolygon(
            roomId = "R1",
            spaceType = ArchitecturalSpaceType.OPEN_LIVING_DINING,
            boundaryPoints = listOf(Pair(0.0, 0.0), Pair(10.0, 0.0), Pair(10.0, 5.0), Pair(0.0, 5.0))
        )

        val wideStreetParams = PropertyValuationParameters(
            districtNameArabic = "حي النرجس",
            baseSquareMeterPriceSar = 6000.0,
            streetWidthMeters = 30.0, // 18m over 12m -> +9% bonus
            finishingGrade = PropertyFinishingGrade.PREMIUM_DELUXE,
            facade = FacadeOrientation.EASTERN_MORNING,
            rooms = listOf(room)
        )

        val narrowStreetParams = wideStreetParams.copy(
            streetWidthMeters = 10.0 // No bonus
        )

        val wideVal = spatialEngine.calculateTaqeemValuation(wideStreetParams)
        val narrowVal = spatialEngine.calculateTaqeemValuation(narrowStreetParams)

        assertTrue(wideVal.estimatedMarketValueSar > narrowVal.estimatedMarketValueSar)
    }

    @Test
    fun testMatterportMeshSerializationAndParsing() {
        val seq = 44
        val vertexId = 981240L
        val x: Short = 4500
        val y: Short = 6000
        val z: Short = 3000
        val nx = 0.0
        val ny = 0.0
        val nz = 1.0 // Horizontal plane
        val classification = MatterportMeshPointStreamDriver.CLASS_FLOOR

        val packet = meshDriver.serializeVertexFrame(seq, vertexId, x, y, z, nx, ny, nz, classification)
        assertEquals(MatterportMeshPointStreamDriver.FRAME_SIZE_BYTES, packet.size)

        val frame = meshDriver.parseVertexFrame(packet)
        assertEquals(seq, frame.sequenceNumber)
        assertEquals(vertexId, frame.vertex.vertexId)
        assertEquals(x, frame.vertex.xMm)
        assertEquals(y, frame.vertex.yMm)
        assertEquals(z, frame.vertex.zMm)
        assertEquals(1.0, frame.vertex.normalZ, 0.05)
        assertEquals(classification, frame.vertex.surfaceClassification)
        assertTrue("CRC-16-CCITT should be verified as valid", frame.isValidCrc)

        // Corrupted packet check
        val corrupted = packet.copyOf()
        corrupted[12] = (corrupted[12].toInt() xor 0xFF).toByte()
        val corruptFrame = meshDriver.parseVertexFrame(corrupted)
        assertFalse("Corrupted frame CRC must fail verification", corruptFrame.isValidCrc)
    }

    @Test
    fun testMeshBoundingBoxAndSurfaceClassification() {
        // Vertical wall cluster (Nx ~ 1.0, Ny ~ 0.0, Nz ~ 0.0)
        val wallVertices = listOf(
            MatterportMeshVertex(1L, 0.toShort(), 0.toShort(), 0.toShort(), 1.0, 0.0, 0.0, MatterportMeshPointStreamDriver.CLASS_BEARING_WALL),
            MatterportMeshVertex(2L, 0.toShort(), 5000.toShort(), 0.toShort(), 1.0, 0.0, 0.0, MatterportMeshPointStreamDriver.CLASS_BEARING_WALL),
            MatterportMeshVertex(3L, 0.toShort(), 5000.toShort(), 3000.toShort(), 1.0, 0.0, 0.0, MatterportMeshPointStreamDriver.CLASS_BEARING_WALL),
            MatterportMeshVertex(4L, 0.toShort(), 0.toShort(), 3000.toShort(), 1.0, 0.0, 0.0, MatterportMeshPointStreamDriver.CLASS_BEARING_WALL)
        )

        val wallSummary = meshDriver.analyzeMeshCluster(wallVertices)
        assertTrue(wallSummary.isWallSurface)
        assertFalse(wallSummary.isHorizontalPlane)
        assertEquals(5.0, wallSummary.lengthMeters, 0.01)
        assertEquals(3.0, wallSummary.ceilingHeightMeters, 0.01)

        // Horizontal floor cluster (Nz ~ 1.0)
        val floorVertices = listOf(
            MatterportMeshVertex(5L, 0.toShort(), 0.toShort(), 0.toShort(), 0.0, 0.0, 1.0, MatterportMeshPointStreamDriver.CLASS_FLOOR),
            MatterportMeshVertex(6L, 4000.toShort(), 0.toShort(), 0.toShort(), 0.0, 0.0, 1.0, MatterportMeshPointStreamDriver.CLASS_FLOOR),
            MatterportMeshVertex(7L, 4000.toShort(), 5000.toShort(), 0.toShort(), 0.0, 0.0, 1.0, MatterportMeshPointStreamDriver.CLASS_FLOOR),
            MatterportMeshVertex(8L, 0.toShort(), 5000.toShort(), 0.toShort(), 0.0, 0.0, 1.0, MatterportMeshPointStreamDriver.CLASS_FLOOR)
        )

        val floorSummary = meshDriver.analyzeMeshCluster(floorVertices)
        assertTrue(floorSummary.isHorizontalPlane)
        assertFalse(floorSummary.isWallSurface)
        assertEquals(20.0, floorSummary.grossFloorAreaSqM, 0.01)
    }

    @Test
    fun testOffPlanWafiEscrowAndEjarBenchmark() {
        val penthouse = ArchitecturalRoomPolygon(
            roomId = "PH-01",
            spaceType = ArchitecturalSpaceType.PANORAMIC_TERRACE,
            boundaryPoints = listOf(Pair(0.0, 0.0), Pair(12.0, 0.0), Pair(12.0, 10.0), Pair(0.0, 10.0)), // 120 sqm
            ceilingHeightMeters = 3.50
        )

        val params = PropertyValuationParameters(
            districtNameArabic = "أبراج الكورنيش - جدة",
            baseSquareMeterPriceSar = 12000.0,
            streetWidthMeters = 40.0,
            finishingGrade = PropertyFinishingGrade.ULTRA_LUXURY_CUSTOM,
            facade = FacadeOrientation.SOUTHERN_SUNLIT,
            rooms = listOf(penthouse)
        )

        val valuation = spatialEngine.calculateTaqeemValuation(params)
        assertEquals(120.0, valuation.totalBuiltUpAreaSqM, 0.1)

        // Annual Ejar rental benchmark is 8.0% of estimated market value
        val expectedEjar = (valuation.estimatedMarketValueSar * 0.08 * 100.0).roundToInt() / 100.0
        assertEquals(expectedEjar, valuation.annualEjarRentalBenchmarkSar, 1.0)

        // Wafi Escrow account target (construction + 15% contingency)
        assertTrue(valuation.wafiOffPlanEscrowTargetSar > 0.0)
        assertTrue(valuation.wafiOffPlanEscrowTargetSar < valuation.estimatedMarketValueSar)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidParametersGuard() {
        val emptyParams = PropertyValuationParameters(
            districtNameArabic = "حي حطين",
            baseSquareMeterPriceSar = -500.0,
            streetWidthMeters = 15.0,
            finishingGrade = PropertyFinishingGrade.COMMERCIAL_STANDARD,
            facade = FacadeOrientation.SOUTHERN_SUNLIT,
            rooms = emptyList()
        )
        spatialEngine.calculateTaqeemValuation(emptyParams)
    }
}
