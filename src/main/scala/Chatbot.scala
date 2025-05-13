import DatasetLoader._
import ResourceLoader._
import Summarizer._
import scala.util.Random


object Chatbot {
    private var state: String = "greeting"
    private val quiz = Quiz

    def getState: String = state
    def getQuiz(): Quiz.type = quiz

    def setState(newState: String): Unit = {
        state = newState
    }


    def nextState(tokens: List[String]): String = {
        val factKeywords = Set("sport", "sports", "basketball", "football", "tennis", "fact", "facts", "know", "learn", "information", "knowledge", "game", "match", "team", "teams", "player", "players")
        val chatKeywords = Set("hi", "hello", "weather", "joke", "day", "life", "chat", "small talk", "casual", "normal", "normally")
    
        val wantsFacts = tokens.exists(token => factKeywords.contains(token))
        val wantsChat = tokens.exists(token => chatKeywords.contains(token))
    
        if (wantsFacts && !wantsChat) 
            setState("facts")
            "Sure! I was made for talking about sports anyway"
        else if (wantsChat && !wantsFacts) 
            setState("chat")
            "Okay! Let's get to know each other more."
        else 
            "I didn't quite get what you want. Please try again."
    }


    def greet(): String = {
        val greetings: List[String] = List(
            "Hey there! It's great to meet you. What's your name?",
            "Hi! I'm excited to chat with you. May I ask your name?",
            "Hello! Before we get started, what's your name?",
            "Hi there! I'd love to know your name.",
            "Welcome! What should I call you?",
            "Hey! It's always nice to know who I'm talking to. What's your name?",
            "Hello! Tell me your name so we can get started.",
            "Good to see you! Mind sharing your name with me?",
            "Hey! I'm ready whenever you are. First, what's your name?"
        )
        Random.shuffle(greetings).head;
    }


    def parse(input: String): List[String] = {
        val tokens = 
            input.toLowerCase()
            .replaceAll("""[\p{Punct}]""", "")
            .split("\\s+")
            .filter(word => !ResourceLoader.stopwords.contains(word))
            .filter(_.nonEmpty)     
            .toList

        val bigrams = tokens.sliding(2).map(_.mkString(" ")).toList
        val trigrams = tokens.sliding(3).map(_.mkString(" ")).toList

        tokens ++ bigrams ++ trigrams
    }


    def extractName(tokens: List[String]): Option[String] = {
        val nameIntroPatterns = List(
            List("my", "name", "is"),
            List("i", "am"),
            List("im"),
            List("i'm"),
            List("call", "me"),
            List("you", "can", "call", "me"),
            List("they", "call", "me"),
            List("it", "is"),
            List("this", "is"),
            List("known", "as"),
            List("name", "is")
        )

        def matchesPattern(tokens: List[String], pattern: List[String]): Boolean = {
            pattern.exists { subPattern =>
                tokens.sliding(subPattern.length).exists(_.sameElements(subPattern))
            }
        }

        val matchedPattern = nameIntroPatterns.find(pattern => matchesPattern(tokens, pattern))

        matchedPattern match {
                case Some(pattern) =>
                    val nameIndex = tokens.indexOf(pattern.last) + 1
                    if (nameIndex < tokens.length) Some(tokens(nameIndex)) else None
                case None => None
        }
    }


    case class UserQuery(
        sport: Option[String],
        intent: Option[String],
        keywords: List[String]
    )


    def toFactQuery(tokens: List[String]): UserQuery = {
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


    def chatReply(tokens: List[String]): Option[String] = {
        conversationMap.collectFirst {
          case (triggers, replies) if triggers.exists(tokens.contains) =>
            Random.shuffle(replies).head
        }
    }


    def generateFact(query: UserQuery): String = {
        (query.sport, query.intent) match {
            case (Some("no sport"), _) =>
                "Hmm, I couldn't tell which sport you're asking about. Could you specify it more clearly?"

            case (Some("multiple sports"), _) =>
                "I noticed you mentioned more than one sport. Could you ask about one sport at a time?"

            case (_, Some("no intent")) =>
                "I'm not sure what you're trying to ask. Could you rephrase your question?"

            case (_, Some("multiple intents")) =>
                "Looks like your question contains multiple requests. Could you focus on one thing?"

            case (Some(sport), Some(intent)) =>
                val matches = sportFacts.filter { fact =>
                    fact.sport == sport &&
                    fact.intent == intent &&
                    query.keywords.exists(kw => fact.keywords.contains(kw))
                }

                if (matches.nonEmpty) matches.head.content
                else "Your question is a bit too vague — I couldn't match it to any specific fact. Could you include more detail?"

            case _ =>
                "Something went wrong while understanding your question. Please try again."
        }
    }


    def interact(input: String): String = {
        val tokens = parse(input)
        var response = ""

        getState match {
            case "default" => response = nextState(tokens)
            case "greeting" =>
                val username = extractName(tokens)
                if (username == None) response = "I was not able to catch that. Please write your name clearly"
                else {
                    setState("default")
                    response = s"Nice to meet you $username, do you have any questions about sports or do you just want to chat with me?"
                }
            case "chat" => response = chatReply(tokens).getOrElse("I'm not sure I understand what you mean. Maybe try asking in a different way")
            case "facts" => response = generateFact(toFactQuery(tokens))
        }

        logInteraction(input, response)
        response
    } 
}