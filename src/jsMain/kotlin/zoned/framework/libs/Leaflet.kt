package zoned.framework.libs

@JsModule("leaflet")
@JsNonModule
external object Leaflet {
    fun map(elementId: String, options: dynamic = definedExternally): LeafletMap

    fun tileLayer(path: String, options: TileLayerOptions): LeafletTileLayer

    fun polygon(coords: Array<Array<Double>>, options: PolygonOptions = definedExternally): LeafletPolygon
}

external interface TileLayerOptions {
    val attribution: String
}

external interface PolygonOptions {
    var color: String?
    var fillColor: String?
    var fillOpacity: Double?
    var weight: Int?
}

external interface LeafletTileLayer {
    fun addTo(map: LeafletMap)
}

external interface LeafletPolygon {
    fun addTo(map: LeafletMap)
    fun getBounds(): LeafletBounds
}

external interface LeafletBounds

external interface LeafletMap {
    fun setView(coords: Array<Double>, zoomLevel: Int)
    fun fitBounds(bounds: LeafletBounds, options: dynamic = definedExternally)
}

/**
 * Initialize Leaflet by importing its CSS
 * Call this once during app initialization
 */
fun initLeaflet() {
    require("leaflet/dist/leaflet.css")
}

external fun require(module: String): dynamic