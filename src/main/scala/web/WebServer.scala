package web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import helpers.{CsvHelper, LyricsApp}


import scala.concurrent.Future
import scala.io.StdIn

object WebServer {

    def main(args: Array[String]) = {

      val csvPath = sys.env("LyricsCsvPath")
      val songData = CsvHelper.parseCsv(csvPath)
      val distinctArtists = songData.map(_(0)).distinct


      implicit val system = ActorSystem("my-system")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher

      def getArtistNameFromId(id: Int) = if(distinctArtists.length < id) "ABBA" else distinctArtists(id)

      def getListOfIds() = {
        distinctArtists
          .flatMap(x => List(x -> distinctArtists.indexOf(x)))
          .map(x => s"${x._1}: ${x._2}<br/>")
          .mkString
      }

//      object LyricsRouter {
//        val route =
//          get {
//            pathPrefix("lyrics" / IntNumber) { id =>
//              val maybeLyrics: String = LyricsApp.makeLyrics(getArtistNameFromId(id), songData)
//              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
//                s"<h1>${getArtistNameFromId(id)}</h1>\n\n${maybeLyrics.replaceAll("\n", "<br/>")}"))
//            }
//          }
//      }
//      object IdsRouter {}
//        val route =
//          path("ids") {
//            get {
//              complete(getListOfIds)
//            }
//          }
//      }

      val route =
        get {
          pathPrefix("lyrics" / IntNumber) { id =>
            val maybeLyrics: String = LyricsApp.makeLyrics(getArtistNameFromId(id), songData)
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
              s"<h1>${getArtistNameFromId(id)}</h1>\n\n${maybeLyrics.replaceAll("\n", "<br/>")}"))
          }
        } ~
          path("ids") {
            get {
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, getListOfIds))
            }
          }

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      println("Server online at localhost. Press return to stop")
      StdIn.readLine()
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())

    }

}
