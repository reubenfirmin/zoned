package zoned.framework.libs

import web.dom.Element

@JsModule("leaflet")
@JsNonModule
external object Leaflet {
    fun map(elementId: String, options: dynamic = definedExternally): LeafletMap
    fun map(element: Element, options: dynamic = definedExternally): LeafletMap

    fun tileLayer(path: String, options: TileLayerOptions = definedExternally): LeafletTileLayer

    fun polygon(coords: Array<Array<Double>>, options: PolygonOptions = definedExternally): LeafletPolygon

    fun geoJSON(geojson: dynamic, options: GeoJSONOptions = definedExternally): LeafletGeoJSON

    fun control(options: dynamic = definedExternally): LeafletControl
}

external interface TileLayerOptions {
    var attribution: String?
    var maxZoom: Int?
    var minZoom: Int?
}

external interface PolygonOptions {
    var color: String?
    var fillColor: String?
    var fillOpacity: Double?
    var weight: Int?
}

external interface GeoJSONOptions {
    var style: dynamic
    var pointToLayer: dynamic
    var onEachFeature: dynamic
}

external interface LeafletTileLayer {
    fun addTo(map: LeafletMap): LeafletTileLayer
}

external interface LeafletPolygon {
    fun addTo(map: LeafletMap): LeafletPolygon
    fun getBounds(): LeafletBounds
}

external interface LeafletGeoJSON {
    fun addTo(map: LeafletMap): LeafletGeoJSON
    fun getBounds(): LeafletBounds
}

external interface LeafletControl {
    fun addTo(map: LeafletMap): LeafletControl
}

external interface LeafletBounds {
    fun isValid(): Boolean
}

external interface LeafletMap {
    fun setView(coords: Array<Double>, zoomLevel: Int): LeafletMap
    fun fitBounds(bounds: LeafletBounds, options: dynamic = definedExternally): LeafletMap
    fun invalidateSize(animate: Boolean = definedExternally): LeafletMap
    fun remove(): LeafletMap
}

/**
 * Initialize Leaflet by importing its CSS
 * Call this once during app initialization
 */
fun initLeaflet() {
    require("leaflet/dist/leaflet.css")
}

external fun require(module: String): dynamic