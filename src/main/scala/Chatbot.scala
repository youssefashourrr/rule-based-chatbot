import Quiz._


object Chatbot {
    private var state: String = "conversation"

    def getState: String = state;
    
    def setState(newState: String): Unit = {
        state = newState
    }


    def greetUser(): String = { 
    }


    handleUserInput(input: String): String = {
    }


    def parseInput(input: String): String = {
    }


    def generateResponse(query: String): String = {  
    }
}