import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.Random
import java.io.File
import com.github.tototoshi.csv._
import DatasetLoader.*


object Quiz
{
    private var questionChosen: Option[Question] = None
    private var askedQuestions:List[Option[Question]] = Nil
    def selectQuizQuestions(category:String,mode:Boolean): (String,java.util.List[String]) =
        val questionBank = if(mode) mcq else frq
        val questionsCategory = questionBank.filter(_.sport == category).filterNot(askedQuestions.contains)
        val randomQuestion = Random.shuffle(questionsCategory).head
        // make sure that this question isn't in the AskedQuestion
        questionChosen = Some(randomQuestion)
        presentQuestion(randomQuestion)

    private def presentQuestion(question:Question): (String,java.util.List[String]) =
        question match
            case mc:MultipleChoice => (mc.content,mc.options.asJava)
            case fr:FreeResponse => (fr.content,java.util.Collections.emptyList())

    def evaluateQuizAnswer(userAnswer: Any): Boolean = {
        var res:Boolean = false
        userAnswer match 
            case s: String => res = verifyMCQ(s)
            case lst: List[_] if classTag[List[String]].runtimeClass.isInstance(lst) => 
                res = verifyFr(lst.asInstanceOf[List[String]])

        askedQuestions ::= questionChosen
        summarizeQuizResults(questionChosen,res)
        res
    }

    private def verifyMCQ(userAnswer: String):Boolean=
        questionChosen match
            case Some(q:MultipleChoice) => q.answer == userAnswer
            case _ => false

    private def verifyFr(userAnswer: List[String]): Boolean =
        questionChosen match
            case Some(q: FreeResponse) => q.keywords.exists(userAnswer.contains)
            case _ => false


    private def summarizeQuizResults(qOpt: Option[Question], res: Boolean): Unit =
        if (qOpt.isEmpty) return
        val q = qOpt.get
        val file = new File("D:\\PBLB\\UNI\\CS219\\ChatBot\\RuledBased Bot\\src\\main\\resources\\quizResults.csv")
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


}