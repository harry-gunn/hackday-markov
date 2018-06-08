import com.github.tototoshi.csv.{CSVReader, MalformedCSVException}

object CsvHelper {

  def parseCsv(pathToCsv: String) = {

    try {
      val reader = CSVReader.open(pathToCsv)
      reader.all()
    }
    catch {
      case malformedError: MalformedCSVException => List()
    }
  }
}
