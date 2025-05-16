import com.github.tototoshi.csv._
import scala.io.Source
import upickle.default._


case class ConversationEntry(
  queries: List[String],
  replies: List[String]
)

object ConversationEntry {
  implicit val rw: ReadWriter[ConversationEntry] = macroRW
}


case class Fact(
    sport: String,
    content: String,
    intent: String,
    keywords: List[String]
)

object Fact {
    implicit val rw: ReadWriter[Fact] = macroRW
}


trait Question {
    def sport: String
    def content:String
}


case class MultipleChoice(
    sport: String,
    content: String,
    options: List[String],
    answer: String
) extends Question

object MultipleChoice {
    implicit val rw: ReadWriter[MultipleChoice] = macroRW
}


case class FreeResponse(
    sport: String,
    content: String,
    keywords: List[String]
) extends Question

object FreeResponse {
    implicit val rw: ReadWriter[FreeResponse] = macroRW
}


object DatasetLoader {
    val sportFacts: List[Fact] = loadSportFacts()
    val mcq: List[MultipleChoice] = loadMCQ()
    val frq: List[FreeResponse] = loadFRQ();
    val conversationMap: Map[List[String], List[String]] = loadConversation();

    private def loadSportFacts(): List[Fact] = {
        val source = Source.fromResource("sports_facts.json").mkString
        read[List[Fact]](source)
    }

    private def loadMCQ(): List[MultipleChoice] = {
        val source = Source.fromResource("mcq_bank.json").mkString
        read[List[MultipleChoice]](source)
    }

    private def loadFRQ(): List[FreeResponse] = {
        val source = Source.fromResource("frq_bank.json").mkString
        read[List[FreeResponse]](source)
    }

    private def loadConversation(): Map[List[String], List[String]] = {
        val source = Source.fromResource("conversation_responses.json").mkString
        val entries: List[ConversationEntry] = read[List[ConversationEntry]](source)
      
        entries.map(entry => entry.queries -> entry.replies).toMap
    }  
}