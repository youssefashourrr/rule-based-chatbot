import com.github.tototoshi.csv._
import scala.io.Source
import upickle.default._



case class Athlete(
    id: Int,
    name: String,
    sex: String,
    age: Option[Int],
    height: Option[Double],
    weight: Option[Double],
    team: String,
    noc: String,
    games: String,
    year: Int,
    season: String,
    city: String,
    sport: String,
    event: String,
    medal: Option[String]
)


case class Fact(
    sport: String,
    content: String,
    intent: String,
    keywords: List[String]
)

object Fact {
    implicit val rw: ReadWriter[Fact] = macroRW
}


object DatasetLoader {
    val olympicsData: List[Athlete] = loadOlympicsData()
    val sportFacts: List[Fact] = loadSportFacts()

    private def loadOlympicsData(): List[Athlete] = {
        val source = CSVReader.open(Source.fromResource("olympics.csv").bufferedReader())
        val header = source.readNext().getOrElse(throw new Exception("CSV file is empty"))

        def parseOption[T](value: String)(parseFunc: String => T): Option[T] = {
            if (value == "NA" || value.isEmpty) None
            else try Some(parseFunc(value)) catch { case _: Exception => None }
        }

        val athletes = source.all().map { row =>
            Athlete(
                id = row(0).toInt,
                name = row(1),
                sex = row(2),
                age = parseOption(row(3))(_.toInt),
                height = parseOption(row(4))(_.toDouble),
                weight = parseOption(row(5))(_.toDouble),
                team = row(6),
                noc = row(7),
                games = row(8),
                year = row(9).toInt,
                season = row(10),
                city = row(11),
                sport = row(12),
                event = row(13),
                medal = parseOption(row(14))(identity)
            )
        }
        source.close()

        athletes
    }

    private def loadSportFacts(): List[Fact] = {
        val source = Source.fromResource("sports_facts.json").mkString
        read[List[Fact]](source)
    }
}