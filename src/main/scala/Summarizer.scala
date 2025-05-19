

import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}
import upickle.default.*

import java.io.File
import java.io.{BufferedWriter, FileWriter}
import com.github.tototoshi.csv.*


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

		// Ensure file exists with headers
		if (!file.exists())
			val writer = CSVWriter.open(file)
			writer.writeRow(headers)
			writer.close()


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

	private def analyzeSuccessRate(): Int =
			val file = new File(filePath + "\\quiz_results.csv")
			if (!file.exists())
				println("No quiz data found.")
				0

			val lines = Source.fromFile(file).getLines().drop(1) // Skip header
			var totalAsked = 0
			var totalCorrect = 0
			for (line <- lines)
				val cols = line.split(",").map(_.trim)
				if (cols.length >= 4)
					val asked = cols(2).toIntOption.getOrElse(0)
					val correct = cols(3).toIntOption.getOrElse(0)
					totalAsked += asked
					totalCorrect += correct


			if (totalAsked == 0) 0
			else (totalCorrect.toDouble / totalAsked * 100).round.toInt

	private def top3CategoryPercentages(): List[(String, Double)] =
			val file = new File(filePath + "\\quiz_results.csv")
			if (!file.exists())
				println("No quiz data found.")
				return List()


			val lines = Source.fromFile(file).getLines().drop(1) // skip header

			val categoryCounts = scala.collection.mutable.Map[String, Int]()
			var totalQuestions = 0

			for (line <- lines)
				val cols = line.split(",").map(_.trim)
				if (cols.length >= 3)
					val category = cols(1)
					val asked = cols(2).toIntOption.getOrElse(0)
					totalQuestions += asked
					categoryCounts(category) = categoryCounts.getOrElse(category, 0) + asked


			if (totalQuestions == 0 || categoryCounts.isEmpty) return List()

			categoryCounts.toList
				.sortBy(-_._2)
				.take(3)
				.map { case (category, count) =>
					val percentage = (count.toDouble / totalQuestions) * 100
					(category, BigDecimal(percentage).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
				}

	def analyzeQuizPerformance(): java.util.List[(String, java.lang.Double)] =
		val sr = analyzeSuccessRate().toDouble
		val categories = top3CategoryPercentages()
		val combined = ("Success Rate", sr) :: categories
		combined.map { case (s, d) => (s, d: java.lang.Double) }.asJava
}