import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.Random
import java.io.File
import com.github.tototoshi.csv.*
import DatasetLoader.*
import ResourceLoader._
import scala.io.Source
import upickle.default.write
import upickle.default.{read, write, ReadWriter, macroRW}
import java.io.{FileWriter, BufferedWriter}

object Quiz
{
    private var questionChosen: Option[Question] = None
    private var askedQuestions: List[Option[Question]] = Nil

    def selectQuizQuestions(category: String, mode: Boolean): (String, java.util.List[String]) =
        val questionBank = if (mode) mcq else frq
        val questionsCategory = questionBank.filter(_.sport == category).filterNot(askedQuestions.contains)
        val randomQuestion = Random.shuffle(questionsCategory).head
        questionChosen = Some(randomQuestion)
        presentQuestion(randomQuestion)

    private def presentQuestion(question: Question): (String, java.util.List[String]) =
        question match
            case mc: MultipleChoice => (mc.content, mc.options.asJava)
            case fr: FreeResponse   => (fr.content, java.util.Collections.emptyList())

    def evaluateQuizAnswer(userAnswer: Any): Boolean =
        var res: Boolean = false
        userAnswer match
            case s: String =>
                res = questionChosen match
                    case Some(_: MultipleChoice) => verifyMCQ(s)
                    case Some(_: FreeResponse)   => verifyFrSingle(s)
                    case _ => false
            case _ => res = false // Unsupported type

        askedQuestions ::= questionChosen
        summarizeQuizResults(questionChosen, res)
        res

    private def verifyMCQ(userAnswer: String): Boolean =
        questionChosen match
            case Some(q: MultipleChoice) => q.answer == userAnswer
            case _ => false

    private def verifyFrSingle(userAnswer: String): Boolean =
        questionChosen match
            case Some(q: FreeResponse) =>
                val cleanedAnswer = userAnswer.trim.toLowerCase
                q.keywords.exists(k => cleanedAnswer.contains(k.toLowerCase))
            case _ => false

    private def summarizeQuizResults(qOpt: Option[Question], res: Boolean): Unit =
        if (qOpt.isEmpty) return
        val q = qOpt.get
        val file = new File("D:\\Hamdy\\ChatBot\\src\\main\\resources\\quiz_results.csv")
        val headers = List("Question", "Category", "Total Asked", "Correct Results")

        // Ensure file exists with headers
        if (!file.exists()) {
            val writer = CSVWriter.open(file)
            writer.writeRow(headers)
            writer.close()
        }

        val reader = CSVReader.open(file)
        val allRows = reader.allWithHeaders()
        reader.close()

        // Update or add row
        val (existingRows, others) = allRows.partition(_("Question") == q.content)

        val updatedRow = existingRows.headOption match {
            case Some(row) =>
                val totalAsked = row("Total Asked").toInt + 1
                val correct = row("Correct Results").toInt + (if (res) 1 else 0)
                Map(
                    "Question" -> q.content,
                    "Category" -> q.sport,
                    "Total Asked" -> totalAsked.toString,
                    "Correct Results" -> correct.toString
                )
            case None =>
                Map(
                    "Question" -> q.content,
                    "Category" -> q.sport,
                    "Total Asked" -> "1",
                    "Correct Results" -> (if (res) "1" else "0")
                )
        }

        val updatedRows = others :+ updatedRow

        val writer = CSVWriter.open(file)
        writer.writeRow(headers)
        updatedRows.foreach { row =>
            writer.writeRow(headers.map(h => row.getOrElse(h, "")))
        }
        writer.close()

    def analyzeSuccessRate(): Int = {
            val file = new File("D:\\Hamdy\\ChatBot\\src\\main\\resources\\quiz_results.csv")
            if (!file.exists()) {
              println("No quiz data found.")
              return 0
            }
          
            val lines = Source.fromFile(file).getLines().drop(1) // Skip header
            var totalAsked = 0
            var totalCorrect = 0
          
            for (line <- lines) {
              val cols = line.split(",").map(_.trim)
              if (cols.length >= 4) {
                val asked = cols(2).toIntOption.getOrElse(0)
                val correct = cols(3).toIntOption.getOrElse(0)
                totalAsked += asked
                totalCorrect += correct
              }
            }
          
            if (totalAsked == 0) 0
            else (totalCorrect.toDouble / totalAsked * 100).round.toInt
          }
      def top3CategoryPercentages(): List[(String, Double)] = {
            val file = new File("D:\\Hamdy\\ChatBot\\src\\main\\resources\\quiz_results.csv")
            if (!file.exists()) {
              println("No quiz data found.")
              return List()
            }
          
            val lines = Source.fromFile(file).getLines().drop(1) // skip header
          
            val categoryCounts = scala.collection.mutable.Map[String, Int]()
            var totalQuestions = 0
          
            for (line <- lines) {
              val cols = line.split(",").map(_.trim)
              if (cols.length >= 3) {
                val category = cols(1)
                val asked = cols(2).toIntOption.getOrElse(0)
                totalQuestions += asked
                categoryCounts(category) = categoryCounts.getOrElse(category, 0) + asked
              }
            }
          
            if (totalQuestions == 0 || categoryCounts.isEmpty) return List()
          
            categoryCounts.toList
              .sortBy(-_._2)
              .take(3)
              .map { case (category, count) =>
                val percentage = (count.toDouble / totalQuestions) * 100
                (category, BigDecimal(percentage).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
              }
          }
      def analyzeQuizPerformance(): java.util.List[(String, java.lang.Double)] = {
            val sr = analyzeSuccessRate().toDouble
            val categories = top3CategoryPercentages()
            val combined = ("Success Rate", sr) :: categories
            import scala.jdk.CollectionConverters._
            combined.map { case (s, d) => (s, d: java.lang.Double) }.asJava
          }
}
