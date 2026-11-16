@file:JsModule("three")
@file:JsNonModule

package zoned.framework.gl

import org.w3c.dom.HTMLCanvasElement

// Minimal typed binding for the three.js primitives zoned exposes. Not a full wrapper — the generic
// scene primitives live here; app-specific scene construction stays in the app.

external open class Object3D {
    val position: Vector3
    val rotation: Euler
    val quaternion: Quaternion
    val scale: Vector3
    val matrix: Matrix4
    val matrixWorld: Matrix4
    var visible: Boolean
    var castShadow: Boolean
    var receiveShadow: Boolean
    var renderOrder: Int
    var frustumCulled: Boolean
    fun add(obj: Object3D)
    fun remove(obj: Object3D)
    fun getWorldPosition(target: Vector3): Vector3
    fun updateMatrixWorld(force: Boolean = definedExternally)
    fun updateMatrix()
    fun worldToLocal(point: Vector3): Vector3
    fun localToWorld(point: Vector3): Vector3
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

external class WebGLRenderer(parameters: RendererOptions = definedExternally) {
    val domElement: HTMLCanvasElement
    val shadowMap: ShadowSettings
    val coordinateSystem: Int
    val xr: XrSettings
    fun setSize(width: Double, height: Double)
    fun setPixelRatio(value: Double)
    fun setClearColor(color: Int, alpha: Double)
    fun render(scene: Scene, camera: PerspectiveCamera)
    fun compileAsync(scene: Scene, camera: PerspectiveCamera): kotlin.js.Promise<Scene>
    fun compile(scene: Scene, camera: PerspectiveCamera)
    fun getRenderTarget(): WebGLRenderTarget?
    fun getActiveCubeFace(): Int
    fun getActiveMipmapLevel(): Int
    fun setRenderTarget(target: WebGLRenderTarget?, activeCubeFace: Int = definedExternally,
                        activeMipmapLevel: Int = definedExternally)
    fun dispose()
}

external class Color(color: String) {
    val r: Double
    val g: Double
    val b: Double
    fun setStyle(color: String): Color
    fun setRGB(r: Double, g: Double, b: Double): Color
}

external class Fog(color: Int, near: Double, far: Double)

external class Vector3(x: Double = definedExternally, y: Double = definedExternally, z: Double = definedExternally) {
    var x: Double
    var y: Double
    var z: Double
    fun set(x: Double, y: Double, z: Double): Vector3
    fun applyMatrix4(matrix: Matrix4): Vector3
    fun project(camera: PerspectiveCamera): Vector3
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
    fun setDrawRange(start: Int, count: Int)
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
