import com.github.tototoshi.csv.*

import scala.annotation.tailrec
import scala.util.Random
import scala.io.StdIn

object Quiz
{
    @tailrec def selectQuizQuestions(mode:Boolean, result:Int = 0,numberQuestions): Unit =
        val rows = CSVReader.open("data.csv").all()
        val randRow = rows(Random.nextInt(rows.length))
        val question = randRow.head
        val ans = randRow.tail
        mode match
            case true => presentQuizQuestion(question)
            case false => presentQuizQuestion(question,ans)

        val userInput: String = StdIn.readLine()
        var updatedResult = result
        evaluateQuizAnswer(userInput) match
            case true =>
                {
                    println("Correct Answer")
                    updatedResult = updatedResult+1
                }
            case false => println("Incorrect Answer")

        println("Would like to Continue")
        val cont: String = StdIn.readLine()

        //cont.map() +ve or -ve with cases
        // gets whether multiple choice or input text
        // if false summarize
        // true recurse
        selectQuizQuestions(mode,updatedResult,numberQuestions+1)

    def presentQuizQuestion(question: String,ans:List[String]=Nil): Unit =
        def displayAns(strings: List[String], i: Int):Unit=
            i match
                case 5 => return
                case _ =>
                    {
                        println(i.toString +":" + strings.head)
                        displayAns(strings.tail,i+1)
                    }
        println(question)
        ans match
            case Nil => Nil
            case head::tail => displayAns(ans,1)

    def evaluateQuizAnswer(userAnswer: String): Boolean = ??? //Yet to be done using an API

    def summarizeQuizResults(answers: List[Boolean]): String = ??? // Need to Discuss with team
}