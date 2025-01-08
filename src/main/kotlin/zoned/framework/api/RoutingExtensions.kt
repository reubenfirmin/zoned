package zoned.framework.api

import io.javalin.Javalin
import io.javalin.http.Context
import kotlinx.html.FlowContent
import zoned.framework.auth.Role
import zoned.framework.db.FormObject
import zoned.framework.form.ConvertedEntity
import zoned.framework.form.parseForm
import zoned.framework.ui.layouts.HTMXTarget
import zoned.framework.ui.libs.target
import zoned.framework.ui.libs.unwrap
import zoned.framework.util.Either
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaType

private val formContextualRoutes: MutableMap<MethodIdentifier, BaseRoute> = mutableMapOf()
private val contextualRoutes: MutableMap<MethodIdentifier, BaseRoute> = mutableMapOf()
private val nonContextualRoutes: MutableMap<MethodIdentifier, BaseRoute> = mutableMapOf()

annotation class GET(val path: String = "", vararg val roles: Role)
annotation class POST(val path: String = "", vararg val roles: Role)
annotation class PUT(val path: String = "", vararg val roles: Role)
annotation class DELETE(val path: String = "", vararg val roles: Role)
annotation class PATCH(val path: String = "", vararg val roles: Role)

fun Javalin.install(vararg resources: Api) {

    resources.forEach { resource ->
        val (basePath, baseRoles) = resource.basePath to resource.baseRoles

        // we're looking for functions that are annotated, and which are either KFunction<Response> or KFunction1<Context, Response>
        resource::class.memberFunctions.filter {
            it.returnType.classifier as KClass<*> == Response::class
        }.filter {
            it.parameters.size in 1..3
        }.forEach {
            // TODO clean up / ugly because of limitation of annotations
            val routeAnnotation = it.findAnnotation<GET>()?.let {
                ann -> AuthedRoute(BaseRoute(basePath + ann.path, Method.GET), (baseRoles + ann.roles.toList()).toTypedArray())
            } ?: it.findAnnotation<POST>()?.let {
                ann -> AuthedRoute(BaseRoute(basePath + ann.path, Method.POST), (baseRoles + ann.roles.toList()).toTypedArray())
            } ?: it.findAnnotation<PUT>()?.let {
                ann -> AuthedRoute(BaseRoute(basePath + ann.path, Method.PUT), (baseRoles + ann.roles.toList()).toTypedArray())
            } ?: it.findAnnotation<DELETE>()?.let {
                ann -> AuthedRoute(BaseRoute(basePath + ann.path, Method.DELETE), (baseRoles + ann.roles.toList()).toTypedArray())
            } ?: it.findAnnotation<PATCH>()?.let {
                ann -> AuthedRoute(BaseRoute(basePath + ann.path, Method.PATCH), (baseRoles + ann.roles.toList()).toTypedArray())
            }

            if (routeAnnotation != null) {
                when {
                    it.parameters.size == 3 -> {
                        // TODO check types
                        formContextualRoutes[it.toIdentifier()] = routeAnnotation.route
                    }
                    it.parameters.size == 2 && it.parameters[1].type.classifier as KClass<*> == Context::class -> {
                        contextualRoutes[it.toIdentifier()] = routeAnnotation.route
                    }
                    it.parameters.size == 1 -> {
                        // TODO verify that this works - not sure if used
                        nonContextualRoutes[it.toIdentifier()] = routeAnnotation.route
                    }
                    else -> {
                        throw Exception("Invalid route function signature: $it")
                    }
                }

                // this installs the route in javalin
                @Suppress("UNCHECKED_CAST")
                apiHandle(routeAnnotation) { context ->
                    context.route(routeAnnotation.route)

                    when {
                        // Case A: Context, ConvertedEntity<T: FormObject> -> Response
                        it.parameters.size == 3 && it.parameters[2].type.classifier == ConvertedEntity::class -> {
                            val formType = it.parameters[2].type.arguments.firstOrNull()?.type?.classifier as? KClass<*>
                            if (formType == null || !formType.isSubclassOf(FormObject::class)) {
                                throw IllegalArgumentException("Invalid form type: must be ConvertedEntity<T> where T is a subclass of FormObject")
                            }
                            val formObject = context.parseForm(formType as KClass<FormObject>)
                            (it as KFunction3<Any, Context, ConvertedEntity<*>, Response>).call(resource, context, formObject)
                        }

                        // Case B: Context, FormObject? -> Response
                        it.parameters.size == 3 && it.parameters[2].type.isMarkedNullable &&
                                (it.parameters[2].type.classifier as? KClass<*>)?.isSubclassOf(FormObject::class) == true -> {
                            val formType = it.parameters[2].type.classifier as KClass<*>
                            val formObject = context.parseForm(formType as KClass<FormObject>)

                            (it as KFunction3<Any, Context, FormObject?, Response>)
                                .call(resource, context, if (formObject.valid()) {
                                        formObject.entity()
                                    } else {
                                        null
                                    })
                        }

                        // Case C: Context -> Response
                        it.parameters.size == 2 && it.parameters[1].type.classifier == Context::class -> {
                            (it as KFunction2<Any, Context, Response>).call(resource, context)
                        }

                        // Case D: () -> Response
                        it.parameters.size == 1 -> {
                            (it as KFunction1<Any, Response>).call(resource)
                        }

                        else -> throw IllegalArgumentException("Unsupported function signature: $it")
                    }
                }
            }
        }
    }
}

fun printRegisteredEndpoints() {
    val allRoutes = listOf(
        formContextualRoutes.entries,
        contextualRoutes.entries,
        nonContextualRoutes.entries
    )

    allRoutes.flatten().forEach { (methodIdentifier, route) ->
        val className = methodIdentifier.declaringClass.simpleName
        val methodName = methodIdentifier.method.name
        println("${route.url()}[${route.method}] -> $className.$methodName")
    }
}

fun Javalin.apiHandle(authedRoute: AuthedRoute, eval: (Context) -> Response): BaseRoute {

    return with (authedRoute.route) {
        addHttpHandler(method.toJavalin(), path, { ctx ->
            val resp = eval(ctx)
            if (resp.unwrap) {
                ctx.unwrap()
            }
            if (!resp.redirect && !resp.handled && resp.type == ResponseType.HTML) {
                if (resp.target != null) {
                    ctx.target(resp.target)
                }
                ctx.html(resp.body.left!!)
            } else if (resp.type == ResponseType.JSON) {
                ctx.json(resp.body.right!!)
            }
        }, *authedRoute.roles)
        // TODO roles is a vararg in the annotation, then spread again here - surely more optimal approach
        this
    }
}

fun aux(target: HTMXTarget,
        elementType: ElementType = ElementType.DIV,
        classes: String = "",
        subrender: FlowContent.(Context) -> Unit) = AuxResponse(target, elementType, classes, subrender)

fun response(target: HTMXTarget? = null, code: Int = 200, fragment: () -> String) =
    Response(Either.left(fragment()), target, code)

fun route(handler: KFunction<Response>): BaseRoute {
    val identifier = handler.toIdentifier()
    return contextualRoutes[identifier] ?: nonContextualRoutes[identifier] ?: formContextualRoutes[identifier] ?: throw Exception("No registered route found for $handler")
}

private fun KFunction<*>.toIdentifier(): MethodIdentifier {
    val javaMethod = this.javaMethod!!
    val declaringClass = when {
        // used when looking up a method reference
        this     is CallableReference && (this as CallableReference).boundReceiver != CallableReference.NO_RECEIVER ->
            (this as CallableReference).boundReceiver::class.java
        // used when reflecting over methods on a class
        this.parameters.isNotEmpty() && this.parameters[0].kind == KParameter.Kind.INSTANCE -> this.parameters[0].type.javaType as Class<*>
        else -> throw Exception("Missing branch; assumption was methods should either be a callable reference or have a receiver")
    }

    return MethodIdentifier(declaringClass, javaMethod)
}

data class Response(val body: Either<String, Any>,
                    val target: HTMXTarget? = null,
                    val code: Int = 200,
                    val unwrap: Boolean = false,
                    val redirect: Boolean = false,
                    val handled: Boolean = false,
                    val type: ResponseType = ResponseType.HTML)

enum class ResponseType {
    HTML, JSON
}

data class AuxResponse(
    val target: HTMXTarget,
    val elementType: ElementType = ElementType.DIV,
    val classes: String = "",
    val subrender: FlowContent.(Context) -> Unit)

enum class ElementType {
    SPAN, DIV, P, TD
}

private data class MethodIdentifier(val declaringClass: Class<*>, val method: java.lang.reflect.Method)
