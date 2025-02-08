package zoned.framework.libs

import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

@JsModule("file-saver")
@JsNonModule
external fun saveAs(blob: Blob, name: String)

fun dataURLtoBlob(dataUrl: String): Blob {
    val arr = dataUrl.split(',')
    val mime = arr[0].split(':')[1].split(';')[0]
    val bstr = atob(arr[1])
    val u8arr = mutableListOf<Byte>()

    for (i in bstr.indices) {
        u8arr.add(bstr[i].code.toByte())
    }
    return Blob(arrayOf(u8arr.toByteArray()), BlobPropertyBag(type = mime))
}

external fun atob(encodedData: String): String
