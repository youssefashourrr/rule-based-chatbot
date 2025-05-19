import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import scala.util.Random
import java.io.File
import com.github.tototoshi.csv.*
import DatasetLoader._
import ResourceLoader._
import Summarizer._
import scala.io.Source
import java.net.URL
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
  
    def getQuizPerformance(): java.util.List[(String, java.lang.Double)] =
      analyzeQuizPerformance()
      

}
