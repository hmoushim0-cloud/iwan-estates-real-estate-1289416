package sa.factory.iwanestatesrealest.spatial

import kotlin.math.abs
import kotlin.math.roundToInt

enum class ArchitecturalSpaceType(val arabicName: String) {
    MAJLIS_MEN("مجلس الضيافة الرئيسي للرجال"),
    MAJLIS_WOMEN("مجلس العائلة والنساء"),
    MASTER_BEDROOM_SUITE("جناح النوم الماستر التنفيذي"),
    COURTYARD_FINA("الفناء الداخلي والحديقة الزجاجية"),
    OPEN_LIVING_DINING("صالة المعيشة وتناول الطعام المفتوحة"),
    CHEF_KITCHEN("المطبخ التحضيري ومستودع المؤن"),
    PANORAMIC_TERRACE("الشرفة البانورامية والرووف")
}

enum class PropertyFinishingGrade(val priceMultiplier: Double) {
    ULTRA_LUXURY_CUSTOM(1.45),
    PREMIUM_DELUXE(1.25),
    COMMERCIAL_STANDARD(1.00)
}

enum class FacadeOrientation(val valueMultiplier: Double) {
    NORTHERN_COOL(1.05),
    SOUTHERN_SUNLIT(1.02),
    EASTERN_MORNING(1.04),
    WESTERN_AFTERNOON(0.98)
}

data class ArchitecturalRoomPolygon(
    val roomId: String,
    val spaceType: ArchitecturalSpaceType,
    val boundaryPoints: List<Pair<Double, Double>>, // (X, Y) coordinates in meters
    val ceilingHeightMeters: Double = 3.20,
    val windowWallRatio: Double = 0.30
)

data class PropertyValuationParameters(
    val districtNameArabic: String,
    val baseSquareMeterPriceSar: Double,
    val streetWidthMeters: Double,
    val finishingGrade: PropertyFinishingGrade,
    val facade: FacadeOrientation,
    val rooms: List<ArchitecturalRoomPolygon>
)

data class PropertyValuationAssessment(
    val districtNameArabic: String,
    val totalBuiltUpAreaSqM: Double,
    val totalCubicVolumeM3: Double,
    val estimatedMarketValueSar: Double,
    val annualEjarRentalBenchmarkSar: Double,
    val wafiOffPlanEscrowTargetSar: Double,
    val roomAreaBreakdown: Map<String, Double>,
    val valuationSummaryArabic: String
)

class IwanArchitecturalSpatialEngine {

    companion object {
        const val STANDARD_PRIME_RENTAL_YIELD: Double = 0.080 // 8.0% standard annual yield
        const val BASELINE_STREET_WIDTH_METERS: Double = 12.0
        const val WAFI_ESCROW_CONTINGENCY_FACTOR: Double = 1.15
    }

    fun computePolygonAreaSqM(points: List<Pair<Double, Double>>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += (points[i].first * points[j].second) - (points[j].first * points[i].second)
        }
        return abs(sum) / 2.0
    }

    fun calculateTaqeemValuation(params: PropertyValuationParameters): PropertyValuationAssessment {
        if (params.baseSquareMeterPriceSar <= 0.0 || params.rooms.isEmpty()) {
            throw IllegalArgumentException("Base price and room list must be valid and positive")
        }

        var totalArea = 0.0
        var totalVolume = 0.0
        val breakdown = mutableMapOf<String, Double>()

        for (room in params.rooms) {
            val area = computePolygonAreaSqM(room.boundaryPoints)
            val volume = area * room.ceilingHeightMeters
            totalArea += area
            totalVolume += volume
            breakdown[room.spaceType.arabicName] = (area * 10.0).roundToInt() / 10.0
        }

        // Street width modifier: +0.5% per meter over baseline 12m
        val streetDelta = (params.streetWidthMeters - BASELINE_STREET_WIDTH_METERS).coerceAtLeast(0.0)
        val streetMultiplier = 1.0 + (streetDelta * 0.005)

        // Combined valuation multiplier
        val combinedMultiplier = params.finishingGrade.priceMultiplier * params.facade.valueMultiplier * streetMultiplier
        val effectiveSqmRate = params.baseSquareMeterPriceSar * combinedMultiplier

        val estimatedMarketValue = totalArea * effectiveSqmRate
        val annualEjarRent = estimatedMarketValue * STANDARD_PRIME_RENTAL_YIELD
        val wafiEscrow = estimatedMarketValue * 0.65 * WAFI_ESCROW_CONTINGENCY_FACTOR

        val summary = String.format(
            "تقييم عقاري معتمد لـ %s: المساحة الإجمالية: %.1f م2 | القيمة التقديرية: %.2f ريال | العائد الإيجاري السنوي المقدر (منصة إيجار): %.2f ريال",
            params.districtNameArabic,
            totalArea,
            estimatedMarketValue,
            annualEjarRent
        )

        return PropertyValuationAssessment(
            districtNameArabic = params.districtNameArabic,
            totalBuiltUpAreaSqM = (totalArea * 10.0).roundToInt() / 10.0,
            totalCubicVolumeM3 = (totalVolume * 10.0).roundToInt() / 10.0,
            estimatedMarketValueSar = (estimatedMarketValue * 100.0).roundToInt() / 100.0,
            annualEjarRentalBenchmarkSar = (annualEjarRent * 100.0).roundToInt() / 100.0,
            wafiOffPlanEscrowTargetSar = (wafiEscrow * 100.0).roundToInt() / 100.0,
            roomAreaBreakdown = breakdown,
            valuationSummaryArabic = summary
        )
    }
}
