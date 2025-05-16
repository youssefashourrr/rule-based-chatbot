import DatasetLoader._
import ResourceLoader._
import Summarizer._
import scala.util.Random


object Chatbot {
    private var state: String = "greeting"
    private val quiz = Quiz
    var user: Option[String] = None

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
            "Great! Let's dive into some cool sports facts. Ask away!"
        else if (wantsChat && !wantsFacts) 
            setState("chat")
            "Sure thing! I'm always up for a nice conversation. What's on your mind?"
        else 
            "Hmm, I couldn't tell if you want to chat or learn something new. Could you rephrase that?"
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

        val bigrams = if (tokens.length >= 2) tokens.sliding(2).map(_.mkString(" ")).toList else Nil
        val trigrams = if (tokens.length >= 3) tokens.sliding(3).map(_.mkString(" ")).toList else Nil
        val quadgrams = if (tokens.length >= 4) tokens.sliding(4).map(_.mkString(" ")).toList else Nil

        tokens ++ bigrams ++ trigrams ++ quadgrams
    }



    def extractName(tokens: List[String]): Option[String] = {
        if (tokens.length == 1) return Some(tokens.head)
    
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
    
        nameIntroPatterns
          .sortBy(-_.length)
          .collectFirst {
            case pattern =>
              val idxOpt = tokens.sliding(pattern.length).zipWithIndex.find {
                case (window, _) => window.map(_.toLowerCase) == pattern
              }.map(_._2 + pattern.length)
    
              idxOpt.flatMap { idx =>
                if (idx < tokens.length) Some(tokens(idx)) else None
              }
          }
          .flatten
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


    def isChatIntent(tokens: List[String]): Boolean = {
        val keywords = Set("chat", "conversation", "convo", "speak", "gossip", "dialogue", "small talk")
        val keywordMatch = tokens.exists(keywords.contains)

        val flatChatQueries = DatasetLoader.conversationMap.keys.flatten.toSet
        val queryPhraseMatch = tokens.exists(flatChatQueries.contains)

        keywordMatch || queryPhraseMatch
    }


    def isFactIntent(tokens: List[String]): Boolean = {
        val factKeywords = Set(
            "fact", "facts", "sport", "sports",
            "soccer", "basketball", "tennis",
            "players", "players", "team", "teams",
            "match", "matches", "game", "games",
            "data", "information", "stats", "statistics"
        )
        tokens.exists(factKeywords.contains)
    }


    def interact(input: String): String = {
        val tokens = parse(input)
        var response = ""

        getState match {
            case "default" => response = nextState(tokens)
            case "greeting" =>
                user = extractName(tokens)
                if (user == None) response = "Hmm, I didn’t quite catch your name. Could you write it a bit more clearly?"
                else {
                    setState("default")
                    val username = user.get.capitalize
                    response = s"Nice to meet you, $username! Would you like to chat or explore some sports facts?"
                }
            case "chat" => 
                if (isFactIntent(tokens)) {
                    setState("facts")
                    response = "Ooh, switching gears to sports knowledge! Ask me anything."
                }
                response = chatReply(tokens).getOrElse("I'm not sure I got that. Try saying it a little differently?")
            case "facts" => 
                if (isChatIntent(tokens)) {
                    setState("chat")
                    response = "Alright! Let's take a break from the facts and just have a chat."
                }
                response = generateFact(toFactQuery(tokens))
        }

        if (user != None) logInteraction(input, response, user.get.capitalize)
        response
    } 
}