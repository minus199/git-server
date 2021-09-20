package com.minus.git.reactive

import reactor.util.function.Tuple2


operator fun <T1, T2> Tuple2<T1, T2>.component1(): T1 = t1
operator fun <T1, T2> Tuple2<T1, T2>.component2(): T2 = t2