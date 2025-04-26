import scala.io.Source


object ResourceLoader {
    val stopwords: Set[String] = loadStopwords()

    private def loadStopwords(): Set[String] = {
        val source = Source.fromResource("stopwords.txt")
        val stopwords = source.getLines().toSet
        source.close()
        stopwords
    }
}