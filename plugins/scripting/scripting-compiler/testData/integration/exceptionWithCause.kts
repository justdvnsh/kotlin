try {
    throw Exception("Error!")
} catch (e: Exception) {
    throw Exception("Oh no", e)
}