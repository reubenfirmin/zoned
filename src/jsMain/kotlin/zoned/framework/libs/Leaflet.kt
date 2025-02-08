package zoned.framework.libs

@JsModule("leaflet")
@JsNonModule
external object Leaflet {
    fun map(elementId: String): LeafletMap

    fun tileLayer(path: String, options: TileLayerOptions): LeafletTileLayer
}

external interface TileLayerOptions {
    val attribution: String
}

external interface LeafletTileLayer {
    fun addTo(map: LeafletMap)
}

external interface LeafletMap {
    fun setView(coords: Array<Double>, zoomLevel: Int)
}