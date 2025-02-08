package zoned.framework.util

data class Either<L, R>(val left: L?, val right: R?) {

    init {
        if (!isLeft() && !isRight()) {
            throw Exception("Cannot supply both sides")
        }
    }

    fun isLeft() = left != null
    fun isRight() = right != null

    fun <T> execute(leftPath: (L) -> T, rightPath: (R) -> T): T {
        return if (isLeft()) {
            leftPath(this.left!!)
        } else {
            rightPath(this.right!!)
        }
    }

    companion object {
        fun <L, R> left(left: L) = Either<L, R>(left, null)

        fun <L, R> right(right: R) = Either<L, R>(null, right)
    }
}
