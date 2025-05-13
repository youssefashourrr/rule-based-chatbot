import DatasetLoader._
import ResourceLoader._
import scala.util.Random
import scala.io.Source
import upickle.default.write
import java.io.File
import upickle.default.{read, write, ReadWriter, macroRW}
import java.io.{FileWriter, BufferedWriter}
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import com.github.tototoshi.csv._
import DatasetLoader.*



object summarizer {
  def logInteraction(userInput: String, chatbotResponse: String): Unit = {
    val interaction = Map("userInput" -> userInput, "chatbotResponse" -> chatbotResponse)
    val json = write(interaction) // Serialize map to JSON
    val file = new java.io.File("C:\\University\\Year two- semster two\\Advanced prog\\rule-based-chatbot\\src\\main\\resources\\chat_log.json")
    val writer = new BufferedWriter(new FileWriter(file, true)) // append mode
    try
      writer.write(json)
      writer.newLine() // for readability
    finally
      writer.close()
  }

  def getInteractionLog(): List[(Int, String, String)] = {
    val file = new java.io.File("C:\\University\\Year two- semster two\\Advanced prog\\rule-based-chatbot\\src\\main\\resources\\chat_log.json")
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

   def summarizeQuizResults(qOpt: Option[Question], res: Boolean): Unit =
    if (qOpt.isEmpty) return
    val q = qOpt.get
    val file = new File("C:\\University\\Year two- semster two\\Advanced prog\\rule-based-chatbot\\src\\main\\resources\\quizResults.csv")
    val headers = List("Question", "Category", "Total Asked", "Correct Results")

    // Ensure file exists with headers
    if (!file.exists()) {
      val writer = CSVWriter.open(file)
      writer.writeRow(headers)
      writer.close()
    }

    val reader = CSVReader.open(file)
    val allRows = reader.allWithHeaders()
    reader.close()

    // Update or add row
    val (existingRows, others) = allRows.partition(_("Question") == q.content)

    val updatedRow = existingRows.headOption match {
      case Some(row) =>
        val totalAsked = row("Total Asked").toInt + 1
        val correct = row("Correct Results").toInt + (if (res) 1 else 0)
        Map(
          "Question" -> q.content,
          "Category" -> q.sport,
          "Total Asked" -> totalAsked.toString,
          "Correct Results" -> correct.toString
        )
      case None =>
        Map(
          "Question" -> q.content,
          "Category" -> q.sport,
          "Total Asked" -> "1",
          "Correct Results" -> (if (res) "1" else "0")
        )
    }

    val updatedRows = others :+ updatedRow

    val writer = CSVWriter.open(file)
    writer.writeRow(headers)
    updatedRows.foreach { row =>
      writer.writeRow(headers.map(h => row.getOrElse(h, "")))
    }
    writer.close()

  def analyzeQuizPerformance(): String = {
    // Open the CSV file
    val reader = CSVReader.open(new java.io.File("C:\\University\\Year two- semster two\\Advanced prog\\rule-based-chatbot\\src\\main\\resources\\quizResults.csv"))
    val records = reader.all()

    // Extract and process data from CSV rows (Skipping header if needed)
    val processedData = records.drop(1).map { row =>
      val category = row(1) // Category column (index 1)
      val totalAsked = row(2).toInt // Total asked column (index 2)
      val correctResults = row(3).toInt // Correct results column (index 3)
      (category, totalAsked, correctResults)
    }

    val categoryCount = processedData.groupBy(_._1).mapValues(_.map(_._2).sum) // Group by category and sum the totalAsked values
    val mostAskedCategory = categoryCount.maxBy(_._2) // Find category with maximum totalAsked
    val mostAskedCategoryName = mostAskedCategory._1
    val mostAskedCategoryCount = mostAskedCategory._2

    // 2. Calculate the total success rate
    val totalQuestions = processedData.map(_._2).sum // Total asked across all questions
    val totalCorrectAnswers = processedData.map(_._3).sum // Total correct results across all questions
    val successRate = if (totalQuestions > 0) (totalCorrectAnswers.toDouble / totalQuestions) * 100 else 0.0

    // Format the result as a string
    val result = s"Most Asked Category: $mostAskedCategoryName ($mostAskedCategoryCount questions), " +
      f"Success Rate: $successRate%.2f%%"
    // Close the CSV reader
    reader.close()
    result
  }
}
