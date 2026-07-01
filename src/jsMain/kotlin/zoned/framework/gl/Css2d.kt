@file:JsModule("three/addons/renderers/CSS2DRenderer.js")
@file:JsNonModule

package zoned.framework.gl

import org.w3c.dom.HTMLElement

/** Renders HTML elements as billboarded labels at 3D positions (three.js addon): crisp, always facing
 *  the reader, styled by the app's own CSS. */
external class CSS2DRenderer {
    val domElement: HTMLElement
    fun setSize(width: Double, height: Double)
    fun render(scene: Scene, camera: PerspectiveCamera)
}

external class CSS2DObject(element: HTMLElement) : Object3D
