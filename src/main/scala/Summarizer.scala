import DatasetLoader._
import scala.util.Random
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import upickle.default._
import java.io.File
import java.net.URL
import java.io.{FileWriter, BufferedWriter}
import com.github.tototoshi.csv._


object Summarizer {
	
	val filePath:String = "D:\\PBLB\\UNI\\CS219\\ChatBot\\RuledBased Bot\\src\\main\\resources" 
	
	def logInteraction(userInput: String, chatbotResponse: String, name: String): Unit = {
		val file = new File(filePath + "\\chat_log.json")
		// Step 1: Read existing JSON array or start fresh
		val existingLogs: List[Map[String, String]] =
			if (file.exists() && file.length() > 0)
			read[List[Map[String, String]]](Source.fromFile(file).mkString)
			else
			List()

		// Step 2: Create new log entry
		val newLog = Map(
			"Name" -> name,
			"userInput" -> userInput,
			"chatbotResponse" -> chatbotResponse
		)

		// Step 3: Append and write back as proper JSON array
		val updatedLogs = existingLogs :+ newLog
		val writer = new BufferedWriter(new FileWriter(file, false)) // overwrite mode
		try {
			writer.write(write(updatedLogs, indent = 2)) // pretty-print JSON
		} finally writer.close()
		}


	def getInteractionLog(): List[(Int, String, String,String)] = {
		val file = new File(filePath + "\\chat_log.json")
		if (!file.exists()) return List.empty
		val lines = Source.fromFile(file).getLines().toList
		lines.zipWithIndex.map { case (line, idx) =>
			val data = read[Map[String, String]](line)
			val userInput = data.getOrElse("userInput", "")
			val chatbotResponse = data.getOrElse("chatbotResponse", "")
			val name = data.getOrElse("Name", "")
			(idx + 1, name ,userInput, chatbotResponse)
		}
	}


	def analyzeInteractions(log: List[(Int, String, String)]): String =
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
				fallbackResponses.getOrElse(
					response,
					"Successfully answered the user's question"
				)
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

	def summarizeQuizResults(qOpt: Option[Question], res: Boolean): Unit =
		if (qOpt.isEmpty) return
		val q = qOpt.get
		val file = new File(filePath + "\\quiz_results.csv")
		val headers = List("Question", "Category", "Total Asked", "Correct Results")

		if (!file.exists()) {
			val writer = CSVWriter.open(file)
			writer.writeRow(headers)
			writer.close()
		}

		val reader = CSVReader.open(file)
		val allRows = reader.allWithHeaders()
		reader.close()

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

	def analyzeQuizPerformance(): String =
		val file = new File(filePath + "\\quiz_results.csv")
		val reader = CSVReader.open(file)

		val records = reader.all()

		val processedData = records.drop(1).map { row =>
			val category = row(1)
			val totalAsked = row(2).toInt
			val correctResults = row(3).toInt
			(category, totalAsked, correctResults)
		}

		val categoryCount = processedData
			.groupBy(_._1)
			.mapValues(
				_.map(_._2).sum
			)
		val mostAskedCategory =
			categoryCount.maxBy(_._2)
		val mostAskedCategoryName = mostAskedCategory._1
		val mostAskedCategoryCount = mostAskedCategory._2

		val totalQuestions =
			processedData.map(_._2).sum 
		val totalCorrectAnswers =
			processedData.map(_._3).sum
		val successRate =
			if (totalQuestions > 0)
				(totalCorrectAnswers.toDouble / totalQuestions) * 100
			else 0.0

		val result =
			s"Most Asked Category: $mostAskedCategoryName ($mostAskedCategoryCount questions), " +
				f"Success Rate: $successRate%.2f%%"
		reader.close()
		result

}