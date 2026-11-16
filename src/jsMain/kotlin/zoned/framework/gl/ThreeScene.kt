@file:JsModule("three")
@file:JsNonModule

package zoned.framework.gl

import js.objects.Record
import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.Uint8Array

/** Native scene capabilities shared by lit meshes, instanced wires, particles and billboards. */
@JsName("Mesh")
external class GeometryMesh : Object3D {
    constructor(geometry: BufferGeometry, material: MeshBasicMaterial)
    constructor(geometry: BufferGeometry, material: MeshPhysicalMaterial)
    constructor(geometry: BufferGeometry, material: MeshStandardMaterial)
    constructor(geometry: BufferGeometry, material: ShaderMaterial)
    constructor(geometry: SphereGeometry, material: MeshStandardMaterial)
    constructor(geometry: SphereGeometry, material: MeshBasicMaterial)
    constructor(geometry: SphereGeometry, material: ShaderMaterial)
    constructor(geometry: BoxGeometry, material: MeshStandardMaterial)
    constructor(geometry: PlaneGeometry, material: MeshStandardMaterial)
}
external class BoxGeometry(width: Double, height: Double, depth: Double)
external class PlaneGeometry(width: Double, height: Double)
external class CylinderGeometry(radiusTop: Double, radiusBottom: Double, height: Double, radialSegments: Int)
external class Matrix4
external class InstancedMesh : Object3D {
    constructor(geometry: CylinderGeometry, material: MeshStandardMaterial, count: Int)
    constructor(geometry: BufferGeometry, material: MeshStandardMaterial, count: Int)
    var count: Int
    val instanceMatrix: PositionAttribute
    val instanceColor: PositionAttribute?
    fun setMatrixAt(index: Int, matrix: Matrix4)
    fun setColorAt(index: Int, color: Color)
    fun computeBoundingSphere()
    fun dispose()
}
external class MeshStandardMaterial {
    var color: Color
    var metalness: Double
    var roughness: Double
    var transparent: Boolean
    var opacity: Double
    var map: CanvasTexture?
    var emissive: Color
    var emissiveIntensity: Double
    var depthWrite: Boolean
}
external class MeshPhysicalMaterial {
    var color: Color
    var emissive: Color
    var emissiveIntensity: Double
    var metalness: Double
    var roughness: Double
    var clearcoat: Double
    var clearcoatRoughness: Double
    var flatShading: Boolean
    var side: Int
    var envMap: ReflectionTexture
    var envMapIntensity: Double
}
external interface ReflectionTexture {
    var generateMipmaps: Boolean
    var needsPMREMUpdate: Boolean
}
external open class WebGLRenderTarget(width: Int = definedExternally, height: Int = definedExternally) {
    val texture: ReflectionTexture
    var samples: Int
    fun setSize(width: Int, height: Int)
    fun dispose()
}
external class WebGLCubeRenderTarget(size: Int) : WebGLRenderTarget
external class CubeCamera(near: Double, far: Double, target: WebGLCubeRenderTarget) : Object3D {
    var coordinateSystem: Int
    val children: Array<PerspectiveCamera>
    fun updateCoordinateSystem()
    fun update(renderer: WebGLRenderer, scene: Scene)
}
external interface XrSettings { var enabled: Boolean }
external class HemisphereLight(sky: Int, ground: Int, intensity: Double) : Object3D
external class DirectionalLight(color: Int, intensity: Double) : Object3D
external class PointLight(color: Int, intensity: Double, distance: Double, decay: Double) : Object3D {
    var intensity: Double
    val color: Color
}
external class SpotLight(color: Int, intensity: Double, distance: Double, angle: Double,
                         penumbra: Double, decay: Double) : Object3D {
    val target: Object3D
    val color: Color
    var intensity: Double
    var angle: Double
    var penumbra: Double
    val shadow: SpotlightShadow
    fun dispose()
}
external interface SpotlightShadow {
    val mapSize: ShadowMapSize
    var bias: Double
    var normalBias: Double
    var radius: Double
    val camera: ShadowCamera
    val matrix: Matrix4
    val map: ShadowTarget?
    var autoUpdate: Boolean
    var needsUpdate: Boolean
}
external interface ShadowMapSize { var width: Int; var height: Int }
external interface ShadowCamera { var near: Double; var far: Double }
external interface ShadowTarget { val texture: ReflectionTexture }
external interface ShadowSettings { var enabled: Boolean; var type: Int }
external val PCFSoftShadowMap: Int
external class Quaternion {
    fun setFromUnitVectors(from: Vector3, to: Vector3): Quaternion
    fun setFromAxisAngle(axis: Vector3, angle: Double): Quaternion
    fun multiply(other: Quaternion): Quaternion
    fun premultiply(other: Quaternion): Quaternion
    fun copy(other: Quaternion): Quaternion
    fun slerp(other: Quaternion, amount: Double): Quaternion
}
external interface ThreeTransform {
    val quaternion: Quaternion
    val scale: Vector3
    val matrix: Matrix4
    fun updateMatrix()
    fun worldToLocal(point: Vector3): Vector3
    fun localToWorld(point: Vector3): Vector3
}
external interface RotatableVector { fun applyQuaternion(rotation: Quaternion): Vector3 }
external interface GeometryAttributes {
    fun getAttribute(name: String): PositionAttribute
    fun computeBoundingSphere()
    fun computeVertexNormals()
}
external interface PositionAttribute {
    var needsUpdate: Boolean
    fun setX(index: Int, x: Double)
    fun setXYZ(index: Int, x: Double, y: Double, z: Double)
}
external interface GlOpacity { var opacity: Double }
external interface VertexColorMaterial { var vertexColors: Boolean }
external val DoubleSide: Int
external val BackSide: Int
external val RedFormat: Int
external val LinearFilter: Int
/** Native WebGL2 volume texture; useful for cached procedural fields and volume rendering. */
external class Data3DTexture(data: Uint8Array, width: Int, height: Int, depth: Int) : ReflectionTexture {
    override var generateMipmaps: Boolean
    override var needsPMREMUpdate: Boolean
    var format: Int
    var minFilter: Int
    var magFilter: Int
    var wrapS: Int
    var wrapT: Int
    var wrapR: Int
    var unpackAlignment: Int
    var needsUpdate: Boolean
    fun dispose()
}
external class Uniform<T>(value: T) { var value: T }
external class ShaderMaterial {
    var toneMapped: Boolean
    var side: Int
    var vertexShader: String
    var fragmentShader: String
    var uniforms: Record<String, Uniform<*>>
    var transparent: Boolean
    var depthTest: Boolean
    var depthWrite: Boolean
    var blending: Int
}
@JsName("Points")
external class ShaderPoints(geometry: BufferGeometry, material: ShaderMaterial) : Object3D
external interface GlDisposable { fun dispose() }
external class CanvasTexture(canvas: HTMLCanvasElement) : ReflectionTexture {
    override var generateMipmaps: Boolean
    override var needsPMREMUpdate: Boolean
    var colorSpace: String
    var wrapS: Int
    var wrapT: Int
    val repeat: Vector2
    fun dispose()
}
external class Vector2(x: Double = definedExternally, y: Double = definedExternally) {
    fun set(x: Double, y: Double): Vector2
}
external interface SpriteColor { fun setStyle(color: String) }
external val AdditiveBlending: Int
external val RepeatWrapping: Int
external class SpriteMaterial {
    var map: CanvasTexture
    val color: SpriteColor
    var opacity: Double
    var transparent: Boolean
    var depthTest: Boolean
    var depthWrite: Boolean
    var alphaTest: Double
    var blending: Int
    var sizeAttenuation: Boolean
    var fog: Boolean
    var toneMapped: Boolean
    fun dispose()
}
external class Sprite(material: SpriteMaterial) : Object3D
