import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import scala.concurrent.Future

import java.sql.{Connection,DriverManager}
import java.sql.ResultSet

object WebServer {

  // domain model
  final case class Salle(nom: String)
  final case class Reservation(email: String, salle: Salle, debut: String, fin: String)

  // formats for unmarshalling and marshalling
  implicit val salleFormat = jsonFormat1(Salle)
  implicit val reservationFormat = jsonFormat4(Reservation)

  // Parametres de connection
  val url = "jdbc:mysql://192.168.1.27:3306/reservation"
  val driver = "com.mysql.jdbc.Driver"
  val username = "test"
  val password = "123"
  var connection:Connection = _

  def main(args: Array[String]) {
  try {

    // besoins pour les routes
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future map/flatmap in the end
    implicit val executionContext = system.dispatcher

    // Transforme rs en Salle
    def toSalle(rs:ResultSet):Salle = {
      new Salle(rs.getString("nom"))
    }

    // Transforme rs en Reservation
    def toReservation(rs:ResultSet):Reservation = {
      new Reservation( rs.getString("email"), new Salle(rs.getString("salle")), rs.getString("debut"), rs.getString("fin") )
    }

    // Pour utiliser map sur rs
    class RsIterator(rs: ResultSet) extends Iterator[ResultSet] {
      def hasNext: Boolean = rs.next()
      def next(): ResultSet = rs
    }

    // Connection à la BDD
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)

    //////// Requetes /////////

    def affiche_salle(nom: String): Future[Option[Salle]] = Future {
      val query="SELECT nom FROM salle WHERE nom="+nom
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      Some(toSalle(rs))
    }

    def salles_disponibles(debut: String, fin: String): Future[Option[List[Salle]]] = Future {
      val query="SELECT nom FROM salle WHERE nom NOT IN (SELECT nom FROM salle JOIN reservation ON reservation.salle=salle.nom WHERE (reservation.debut>:debut and reservation.debut<:fin) OR (reservation.debut>"+debut+" and reservation.debut<"+fin+"))"
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      val rows = new RsIterator(rs).map( x => toSalle(x)).toList
      Some(rows)
    }

    def liste_des_salles(): Future[Option[List[Salle]]] = Future {
      val query="SELECT nom FROM salle"
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      val rows = new RsIterator(rs).map( x => toSalle(x)).toList
      Some(rows)
    }

    def reservations_par_salle(nom: String): Future[Option[List[Reservation]]] = Future {
      val query="SELECT * FROM reservation WHERE salle="+nom
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      val rows = new RsIterator(rs).map( x => toReservation(x)).toList
      Some(rows)
    }

    // Requete à compléter
    def ajoute_reservation(reserve: Reservation): Future[Done] = Future {
      val query="INSERT INTO reservation VALUES ..."
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      println(">> Réservation ajoutée <<")
      ???
    }

    // Requete à compléter
    def supprime_reservation(reserve: Reservation): Future[Done] = Future {
      val query="DELETE FROM reservation WHERE email=:email, salle=:nom, ..."
      val statement = connection.createStatement
      val rs = statement.executeQuery(query)
      println(">> Réservation supprimée <<")
      ???
    }

    //////// Routes /////////

    val route: Route =
      get {
        path("salle") {
          parameter("nom") { nom =>
            val maybe: Future[Option[Salle]] = affiche_salle(nom)
            onSuccess(maybe) {
              case Some(objet) => complete(objet)
              case None       => complete(StatusCodes.NotFound)
            }
          }
        } ~
        path("liste_des_salles") {
          val maybe: Future[Option[List[Salle]]] = liste_des_salles()
          onSuccess(maybe) {
            case Some(objet) => complete(objet)
            case None       => complete(StatusCodes.NotFound)
          }
        } ~
        path("salles_disponibles") {
          parameters("debut","fin") { (debut, fin) =>
            val maybe: Future[Option[List[Salle]]] = salles_disponibles(debut, fin)
            onSuccess(maybe) {
              case Some(objet) => complete(objet)
              case None       => complete(StatusCodes.NotFound)
            }
          }
        } ~
        path("reservations_par_salle") {
        parameter("nom") { nom =>
          val maybe: Future[Option[List[Reservation]]] = reservations_par_salle(nom)
          onSuccess(maybe) {
            case Some(objet) => complete(objet)
            case None       => complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      post {
        path("ajoute_reservation") {
          entity(as[Reservation]) { reservation =>
            val saved: Future[Done] = ajoute_reservation(reservation)
            onComplete(saved) {
              done => complete("Réservation ajoutée\n")
            }
          }
        } ~
        path("supprime_reservation") {
          entity(as[Reservation]) { reservation =>
            val saved: Future[Done] = supprime_reservation(reservation)
            onComplete(saved) {
              done => complete("Réservation supprimée\n")
            }
          }
        }
      }

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

    } catch {
      case e: Exception => e.printStackTrace
    }
  } // main
} // object
/*

curl -v -H "Content-Type: application/json" -X POST http://localhost:8080/ajoute_reservation -d '{"email":"titi@gmail.com","fin":"","id":1,"debut":"","salle":{"id":1,"nom":"G211"}}'

localhost:8080/reservations_par_salle?nom=G211
-> liste des reservations en json

localhost:8080/salles_disponibles?debut="2017-01-23 15:00"&fin="2017-01-23 16:00"
-> liste des salles en json

*/
