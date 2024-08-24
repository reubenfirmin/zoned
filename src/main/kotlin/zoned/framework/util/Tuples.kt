package zoned.framework.util

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Pentuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

infix fun <A, B> A.by(that: B): Pair<A, B> = Pair(this, that)
infix fun <A, B, C> Pair<A, B>.by(that: C): Triple<A, B, C> = Triple(first, second, that)
infix fun <A, B, C, D> Triple<A, B, C>.by(that: D): Quadruple<A, B, C, D> = Quadruple(first, second, third, that)
infix fun <A, B, C, D, E> Quadruple<A, B, C, D>.by(that: E): Pentuple<A, B, C, D, E> = Pentuple(first, second, third, fourth, that)