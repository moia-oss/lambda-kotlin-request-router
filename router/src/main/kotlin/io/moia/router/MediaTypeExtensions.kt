import com.google.common.net.MediaType

fun MediaType.isCompatibleWith(other: MediaType): Boolean =
    if (this.`is`(other))
        true
    else {
        type() == other.type() &&
        (subtype().contains("+") && other.subtype().contains("+")) &&
            this.subtype().substringBeforeLast("+") == "*" &&
                this.subtype().substringAfterLast("+") == other.subtype().substringAfterLast("+")
    }