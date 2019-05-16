import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class AkkaStreamNotAsyncTest extends FunSuite with BeforeAndAfterAll {

  trait Fixture {}

  ignore("asyncなしのテスト順番に実行される") {
    new Fixture {
      implicit val system: ActorSystem = ActorSystem("HelloWorld")
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      implicit val executionContext: ExecutionContextExecutor =
        system.dispatcher

      val logger: LoggingAdapter = Logging(system, "HelloWorld")
      val source: Source[Int, NotUsed] = Source(1 to 10)
        .log("source")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      val flow: Flow[Int, Int, NotUsed] = Flow[Int]
        .map { i =>
          Thread.sleep((10 - i) * 200)
          i
        }
        .log("flow", elem => s"[$elem]")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))

      val runnable = source.via(flow).toMat(Sink.ignore)(Keep.right)

      runnable.run().onComplete {
        case Success(_) =>
          logger.info("success")
          system.terminate()
        case Failure(cause) =>
          logger.error(cause, "run")
          system.terminate()
      }
    }
  }

  test("asyncのテスト順番に実行される") {
    new Fixture {
      implicit val system: ActorSystem = ActorSystem("HelloWorld2")
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      implicit val executionContext: ExecutionContextExecutor =
        system.dispatcher

      val logger: LoggingAdapter = Logging(system, "HelloWorld2")
      val source: Source[Int, NotUsed] = Source(1 to 10).async
        .log("source")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      val flow: Flow[Int, Int, NotUsed] = Flow[Int]
        .map { i =>
          Thread.sleep((10 - i) * 200)
          i
        }
        .async
        .log("flow", elem => s"[$elem]")
        .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))

      val runnable = source.via(flow).toMat(Sink.ignore)(Keep.right)

      runnable.run().onComplete {
        case Success(_) =>
          logger.info("success")
        case Failure(cause) =>
          logger.error(cause, "run")
          system.terminate()
      }
    }
  }
}
