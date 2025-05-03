import com.github.tototoshi.csv.*
import scala.annotation.tailrec
import scala.util.Random
import scala.jdk.CollectionConverters._
import java.util.Collections
import DatasetLoader._

object Quiz
{
    private var questionChosen: Option[Question] = None
    def selectQuizQuestions(category:String,mode:Boolean): (String,java.util.List[String]) =
        val questionBank = if(mode) mcq else frq
        val questionsCategory = questionBank.filter(_.sport == category)
        val randomQuestion = Random.shuffle(questionsCategory).head
        questionChosen = Some(randomQuestion)
        presentQuestion(randomQuestion)

    private def presentQuestion(question:Question): (String,java.util.List[String]) =
        question match
            case mc:MultipleChoice => (mc.content,mc.options.asJava)
            case fr:FreeResponse => (fr.content,java.util.Collections.emptyList())

    def evaluateQuizAnswer(userAnswer:String): Boolean =
        questionChosen match
            case Some(mc: MultipleChoice) => verifyMCQ(userAnswer)
            case Some(fr: FreeResponse) => verifyFr(userAnswer)

    private def verifyMCQ(userAnswer: String):Boolean=
        questionChosen match
            case Some(mc:MultipleChoice) => mc.answer == userAnswer
            case _ => false

    private def verifyFr(userAnswer: String):Boolean = false
    def summarizeQuizResults(answers: List[Boolean]): String = ??? // Need to Discuss with team
}