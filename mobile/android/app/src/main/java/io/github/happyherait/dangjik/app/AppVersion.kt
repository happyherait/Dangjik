package io.github.happyherait.dangjik.app

/** 서버의 VersionPolicy와 같은 규칙. 서버 응답이 없을 때도 앱이 스스로 판단할 수 있게 둔다. */
object AppVersion {
    private val pattern = Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}""")

    fun isValid(version: String): Boolean = pattern.matches(version)

    fun compare(a: String, b: String): Int {
        val x = a.split(".").map { it.toInt() }
        val y = b.split(".").map { it.toInt() }
        for (i in 0 until 3) {
            val c = x[i].compareTo(y[i])
            if (c != 0) return c
        }
        return 0
    }

    fun mustUpdate(current: String, minSupported: String): Boolean =
        !isValid(current) || compare(current, minSupported) < 0
}
