package zoned.framework.libs

import kotlin.js.Promise
import web.dom.Element

/** The Leaflet module surface (the `L` object). */
external interface LeafletModule {
    fun map(elementId: String, options: dynamic = definedExternally): LeafletMap
    fun map(element: Element, options: dynamic = definedExternally): LeafletMap

    fun tileLayer(path: String, options: TileLayerOptions = definedExternally): LeafletTileLayer

    fun polygon(coords: Array<Array<Double>>, options: PolygonOptions = definedExternally): LeafletPolygon

    fun geoJSON(geojson: dynamic, options: GeoJSONOptions = definedExternally): LeafletGeoJSON

    fun control(options: dynamic = definedExternally): LeafletControl
}

private var leafletModule: LeafletModule? = null

/**
 * Load Leaflet (and its stylesheet) ON DEMAND — dynamic `import()`s, so webpack splits both out of
 * the main bundle and a session with no map on screen never fetches them. Replaces the old eager
 * `initLeaflet()` startup hook. Cached after first load.
 */
fun loadLeaflet(): Promise<LeafletModule> {
    leafletModule?.let { return Promise.resolve(it) }
    val loading = js(
        "Promise.all([import('leaflet'), import('leaflet/dist/leaflet.css')])" +
            ".then(function(r){ return r[0]; })"
    ).unsafeCast<Promise<dynamic>>()
    return loading.then<LeafletModule> { m ->
        val leaflet = unwrapModule(m, probe = "map").unsafeCast<LeafletModule>()
        leafletModule = leaflet
        leaflet
    }
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

