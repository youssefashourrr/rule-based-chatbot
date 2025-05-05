import scala.util.Random
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import DatasetLoader.*

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

    def evaluateQuizAnswer(userAnswer: Any): Boolean =
        userAnswer match 
            case s: String => verifyMCQ(s)
            case lst: List[_] if classTag[List[String]].runtimeClass.isInstance(lst) => 
                verifyFr(lst.asInstanceOf[List[String]])

    private def verifyMCQ(userAnswer: String):Boolean=
        questionChosen match
            case Some(q:MultipleChoice) => q.answer == userAnswer
            case _ => false

    private def verifyFr(userAnswer: List[String]): Boolean =
        questionChosen match
            case Some(q: FreeResponse) =>
                userAnswer.exists { userWord =>
                    q.keywords.contains(userWord) ||
                      q.regexKeywords.exists(regex => regex.r.findFirstIn(userWord).isDefined)
                }
            case _ => false


    def summarizeQuizResults(answers: List[Boolean]): String = ??? // Need to Discuss with team
}