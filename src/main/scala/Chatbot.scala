import DatasetLoader._
import ResourceLoader._
import scala.util.Random
import scala.io.Source
import upickle.default.write
import java.io.File
import upickle.default.{read, write, ReadWriter, macroRW}
import java.io.{FileWriter, BufferedWriter}

object Chatbot {
    private var state: String = ""
    private val quiz = Quiz

    def getState: String = state;
    
    def setState(newState: String): Unit = {
        state = newState
    }


    def greetUser(): String = {
        val greetings: List[String] = List(
            "Hey there! It's great to meet you. What's your name?",
            "Hi! I'm excited to chat with you. May I ask your name?",
            "Hello! Before we get started, what's your name?",
            "Hi there! I'd love to know your name.",
            "Welcome! What should I call you?",
            "Hey! It's always nice to know who I'm talking to. What's your name?",
            "Hello! Tell me your name so we can get started.",
            "Good to see you! Mind sharing your name with me?",
            "Hey! I’m ready whenever you are. First, what's your name?"
        )
        Random.shuffle(greetings).head;
    }


    def parseInput(input: String): List[String] = {
        val tokens = 
            input.toLowerCase()
            .replaceAll("""[\p{Punct}]""", "")
            .split("\\s+")
            .filter(word => !ResourceLoader.stopwords.contains(word))
            .filter(_.nonEmpty)     
            .toList

        val bigrams = tokens.sliding(2).map(_.mkString(" ")).toList

        tokens ++ bigrams
    }


    case class UserQuery(
        sport: Option[String],
        intent: Option[String],
        keywords: List[String]
    )


    def handleUserInput(tokens: List[String]): UserQuery = {
        val knownSports: Set[String] = Set("soccer", "basketball", "tennis")
        val knownIntents: Set[String] = Set("what", "when", "how", "how many", "how long", "who", "where")

        val matchedSports = tokens.filter(token => knownSports.contains(token))

        var matchedIntents = tokens.filter(token => knownIntents.contains(token))

        if (matchedIntents.contains("how") &&
            (matchedIntents.contains("how many") || matchedIntents.contains("how long"))) {
            matchedIntents = matchedIntents.filterNot(_ == "how")
        }


        val _sport: Option[String] = matchedSports.distinct match {
            case Nil => Some("no sport")
            case single :: Nil => Some(single)
            case _ => Some("multiple sports")
        }

        val _intent: Option[String] = matchedIntents.distinct match {
            case Nil => Some("no intent")
            case single :: Nil => Some(single)
            case _ => Some("multiple intents")
        }

        val _keywords = tokens
            .filterNot(token => knownSports.contains(token) || knownIntents.contains(token))
            .distinct

        UserQuery(sport = _sport, intent = _intent, keywords = _keywords)
    }


    def generateResponse(input: String ): String = {
        var query: UserQuery = handleUserInput((parseInput(input)))
        var str:String = "";
        (query.sport, query.intent) match {
            case (Some("no sport"), _) =>
               str =  "Hmm, I couldn't tell which sport you're asking about. Could you specify it more clearly?"

            case (Some("multiple sports"), _) =>
               str =  "I noticed you mentioned more than one sport. Could you ask about one sport at a time?"

            case (_, Some("no intent")) =>
               str=   "I'm not sure what you're trying to ask. Could you rephrase your question?"

            case (_, Some("multiple intents")) =>
              str=   "Looks like your question contains multiple requests. Could you focus on one thing?"

            case (Some(sport), Some(intent)) =>
                val matches = sportFacts.filter { fact =>
                    fact.sport == sport &&
                    fact.intent == intent &&
                    query.keywords.exists(kw => fact.keywords.contains(kw))
                }

                if (matches.nonEmpty) str = matches.head.content
                else str=  "Your question is a bit too vague — I couldn't match it to any specific fact. Could you include more detail?"
        
            case _ =>
                str = "Something went wrong while understanding your question. Please try again."
        }
        logInteraction(input,str);
        str
    }
    def logInteraction(userInput: String, chatbotResponse: String): Unit = {
        val interaction = Map("userInput" -> userInput, "chatbotResponse" -> chatbotResponse)
        val json = write(interaction) // Serialize map to JSON
        val file = new java.io.File("D:\\Hamdy\\ChatBot\\src\\main\\resources\\chat_log.json")
        val writer = new BufferedWriter(new FileWriter(file, true)) // append mode
        try
            writer.write(json)
            writer.newLine() // for readability
        finally
            writer.close()
    }

    def getInteractionLog(): List[(Int, String, String)] = {
        val file = new java.io.File("D:\\Hamdy\\ChatBot\\src\\main\\resources\\chat_log.json")
        if (!file.exists()) return List.empty

        val lines = Source.fromFile(file).getLines().toList
        lines.zipWithIndex.map { case (line, idx) =>
            val data = read[Map[String, String]](line)
            val userInput = data.getOrElse("userInput", "")
            val chatbotResponse = data.getOrElse("chatbotResponse", "")
            (idx + 1, userInput, chatbotResponse)
        }
    }

    def analyzeInteractions(log: List[(Int, String, String)]): String = {
        if (log.isEmpty) return "No interactions to analyze."
        val fallbackResponses = Map(
            "Hmm, I couldn't tell which sport you're asking about. Could you specify it more clearly?" ->
              "User did not mention the sport",
            "I noticed you mentioned more than one sport. Could you ask about one sport at a time?" ->
              "User mentioned multiple sports",
            "I'm not sure what you're trying to ask. Could you rephrase your question?" ->
              "Chatbot did not understand the question",
            "Looks like your question contains multiple requests. Could you focus on one thing?" ->
              "User asked multiple things",
            "Your question is a bit too vague — I couldn't match it to any specific fact. Could you include more detail?" ->
              "Question was vague",
            "Something went wrong while understanding your question. Please try again." ->
              "Unexpected error"
        )

        val summaryCounts = log
          .map { case (_, _, response) =>
              fallbackResponses.getOrElse(response, "Successfully answered the user's question")
          }
          .groupBy(identity)
          .mapValues(_.size)
          .toList
          .sortBy(-_._2)

        val total = log.size
        val reportBuilder = new StringBuilder
        reportBuilder.append(s"Total interactions: $total\n\n")
        reportBuilder.append("Chatbot response distribution:\n")

        for ((summary, count) <- summaryCounts) {
            val percentage = (count.toDouble / total) * 100
            reportBuilder.append(f" - $percentage%.0f%% $summary\n")
        }

        reportBuilder.toString()
    }

    def getQuiz(): Quiz.type = quiz
}
