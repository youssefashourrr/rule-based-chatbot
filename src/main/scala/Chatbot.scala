import Quiz._
import ResourceLoader._

import scala.util.Random


object Chatbot {
    private var state: String = ""

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
        input.toLowerCase()
            .replaceAll("""[\p{Punct}]""", "")
            .split("\\s+")
            .filter(word => !ResourceLoader.stopwords.contains(word))
            .filter(_.nonEmpty)      
            .toList
    }


    def handleUserInput(input: String): String = {}


    def generateResponse(query: String): String = {}
}
