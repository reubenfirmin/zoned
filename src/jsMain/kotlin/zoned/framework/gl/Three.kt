@file:JsModule("three")
@file:JsNonModule

package zoned.framework.gl

import org.w3c.dom.HTMLCanvasElement

// Minimal typed binding for the three.js primitives zoned exposes. Not a full wrapper — the generic
// scene primitives live here; app-specific scene construction stays in the app.

external open class Object3D {
    val position: Vector3
    val rotation: Euler
    var visible: Boolean
    fun add(obj: Object3D)
    fun remove(obj: Object3D)
    fun getWorldPosition(target: Vector3): Vector3
    fun updateMatrixWorld(force: Boolean = definedExternally)
}

external class Euler {
    var x: Double
    var y: Double
    var z: Double
    fun set(x: Double, y: Double, z: Double): Euler
}

external class Group : Object3D

external class Scene : Object3D {
    var fog: Fog
}

external class PerspectiveCamera(fov: Double, aspect: Double, near: Double, far: Double) : Object3D {
    var aspect: Double
    fun updateProjectionMatrix()
    fun lookAt(x: Double, y: Double, z: Double)
}

external class WebGLRenderer(parameters: dynamic = definedExternally) {
    val domElement: HTMLCanvasElement
    fun setSize(width: Double, height: Double)
    fun setPixelRatio(value: Double)
    fun setClearColor(color: Int, alpha: Double)
    fun render(scene: Scene, camera: PerspectiveCamera)
}

external class Color(color: String)

external class Fog(color: Int, near: Double, far: Double)

external class Vector3(x: Double = definedExternally, y: Double = definedExternally, z: Double = definedExternally) {
    var x: Double
    var y: Double
    var z: Double
    fun set(x: Double, y: Double, z: Double): Vector3
}

external class SphereGeometry(radius: Double, widthSegments: Int, heightSegments: Int)

external class MeshBasicMaterial {
    var color: Color
    var transparent: Boolean
    var opacity: Double
}

external class Mesh(geometry: SphereGeometry, material: MeshBasicMaterial) : Object3D

external class BufferGeometry {
    fun setFromPoints(points: Array<Vector3>): BufferGeometry
    fun setAttribute(name: String, attribute: Float32BufferAttribute): BufferGeometry
}

external class Float32BufferAttribute(array: FloatArray, itemSize: Int)

external class LineBasicMaterial {
    var color: Color
    var transparent: Boolean
    var opacity: Double
}

external class LineSegments(geometry: BufferGeometry, material: LineBasicMaterial) : Object3D

external class PointsMaterial {
    var size: Double
    var color: Color
    var transparent: Boolean
    var opacity: Double
    var sizeAttenuation: Boolean
    var fog: Boolean
}

external class Points(geometry: BufferGeometry, material: PointsMaterial) : Object3D
