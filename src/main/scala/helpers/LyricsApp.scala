package helpers


import scala.annotation.tailrec
import scala.util.Random


object LyricsApp {

  case class WordsPair(previous: String, next: String)
  case class WordWithPossibleNextWords(word: String, possibleNext: List[String])

  val csvPath = sys.env("LyricsCsvPath")
  val songData = CsvHelper.parseCsv(csvPath)

  println(makeLyrics("ABBA", songData))

  def makeLyrics(maybeArtist: String, songData: List[List[String]]): String = {

      val trimmedSongsByArtist: List[List[String]] = songData
        .filterNot(_ (0) == "artist") // for when using the whole dataset and not filtering by artist
        .filter(_ (0) == maybeArtist)
        .map(songData => songData(3)) // just take the lyrics from the dataset, ignore other metadata
        .map(songLyrics => songLyrics.toLowerCase
        .trim
        .split(" ").toList
      )

      val slidingLyricsBySongs: List[List[WordsPair]] = trimmedSongsByArtist
        .map(songLyrics => {
          songLyrics
            .sliding(2, 1).toList
            .map(adjacentWords => WordsPair(adjacentWords(0), adjacentWords(1)))
        }
        )

      val allSortedLyrics: List[WordsPair] = slidingLyricsBySongs
        .flatMap(wordPairsBySong => wordPairsBySong.sortBy(_.previous))
      //flatmap as we no longer need to separate by song

      val lyricsProbabilityChain = allSortedLyrics
        .groupBy(_.previous).toList
        .map(x => WordWithPossibleNextWords(
          x._1,
          x._2.map(wordPair => wordPair.next))
        )

      val randomStartingWord = Random.shuffle(lyricsProbabilityChain).head.word

      val rawUnfilteredOuput = buildNewLyrics(randomStartingWord, lyricsProbabilityChain)
        .replaceAll("  ", ",")
        .replaceAll("\n,\n", "\n\n")


      // there's probably some (gross|nice) regex to do all of the following in a lot less lines but...
      """[\.!?\n]\s+[a-z]""".r.replaceAllIn(rawUnfilteredOuput, _.matched.toUpperCase)
        .replaceAll("[\\[\\]\\.\"()$||;|]", "")
        .split('\n').toList.map(_.capitalize).mkString("\n")
        .trim
        .capitalize
        .reverse.replaceFirst(",", ".").reverse // this is pretty gross and should be changed



  }

  def buildNewLyrics(firstWord: String, possibleNextWords: List[WordWithPossibleNextWords]) = {
    @tailrec
    def helper(word: String, possibleNextWords: List[WordWithPossibleNextWords], incompleteSentence: String): String = {
      if(incompleteSentence.length > 750 && incompleteSentence.last == ',') incompleteSentence else {
        helper(
          Random.shuffle(possibleNextWords.filter(_.word == word).head.possibleNext).head,
          possibleNextWords,
          s"$incompleteSentence $word"
        )
      }
    }

    helper(firstWord, possibleNextWords, "")
  }

  def mostSongs() = {
    val songData = CsvHelper.parseCsv("/Users/gunnh/Dev/Lyrics/src/main/resources/songdata.csv")
    val artistsAndSongCount = songData.map(csvRow => csvRow.head)
      .groupBy(identity)
      .mapValues(_.size)

    val artistsBySongCount = artistsAndSongCount.toSeq.sortWith(_._2 > _._2)
    println(artistsBySongCount)
  }
}
