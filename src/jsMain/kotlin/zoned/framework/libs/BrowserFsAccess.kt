package zoned.framework.libs

import org.w3c.files.Blob
import org.w3c.files.File
import kotlin.js.Promise

@JsModule("browser-fs-access")
@JsNonModule
external object BrowserFsAccess {
    fun directoryOpen(options: BrowserFsOptions): Promise<Array<FileWithDirectoryAndFileHandle>>

    interface BrowserFsOptions {
        var extensions: Array<String>
        var recursive: Boolean
        var startIn: String
        var mode: String
    }
}

external interface FileWithDirectoryAndFileHandle {
    val directoryHandle: FSDirectoryHandle
    val fileHandle: FSFileHandle
}

external interface FSDirectoryHandle {
    fun getFileHandle(name: String, fileOptions: FileHandleOptions): Promise<FSFileHandle>
}

external interface FileHandleOptions {
    var create: Boolean
}

external interface FSFileHandle {
    fun getFile(): Promise<File>
    fun createWritable(): Promise<FileSystemWritableFileStream>
}

external interface FileSystemWritableFileStream {
    fun write(data: Blob): Promise<Unit>
    fun truncate(size: Long): Promise<Unit>
}

