import java.time.Duration

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import org.scalatest.FunSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class AkkaStreamTest extends FunSuite {

  trait Fixture {
    implicit val system = ActorSystem("simple-stream")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
  }

  test("AkkaStreamのテスト") {
    new Fixture {
      val source: Source[Int, NotUsed] = Source(1 to 10)
      val sink: Sink[Int, Future[Done]] =
        Sink.foreach[Int](e => println(s"element=$e"))
      val result = source.toMat(sink)(Keep.right).run()
      result.onComplete(_ => system.terminate())
    }
  }

  test("Sourceのショートカットバージョン") {
    new Fixture {
      Source(1 to 10).runForeach(println).onComplete(_ => system.terminate())
    }
  }

  test("Matのテスト(keep right)") {
    new Fixture {
      val source: Source[Int, NotUsed] = Source(1 to 10)
      val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
      source.toMat(sink)(Keep.right).run().foreach { r =>
        println(s"result:$r")
      }
      print(source.toMat(sink)(Keep.left).run())
    }
  }

  test("エラー処理") {
    new Fixture {
      val source = Source(1 to 5)
      val flow = Flow[Int].map(e =>
        if (e == 3) throw new IllegalArgumentException("oops!") else e)
      val sink = Sink.foreach[Int](e => println(s"element=$e"))
      source
        .via(flow)
        .toMat(sink)(Keep.right)
        .run()
        .recover {
          case _: IllegalArgumentException =>
            println("oops! IllegalArgumentException")
            "recovered"
        }
        .onComplete { r =>
          println(s"result:$r")
          system.terminate()
        }
    }
  }

  test("代替ソースによる回復") {
    new Fixture {
      val source = Source(1 to 5)
      val flow = Flow[Int]
        .map(e =>
          if (e == 3) throw new IllegalArgumentException("oops!") else e)
        .recoverWithRetries(1, {
          case _: IllegalArgumentException => Source(List(30, 40))
        })

      val sink = Sink.foreach[Int](e => println(s"element=$e"))
      source
        .via(flow)
        .toMat(sink)(Keep.right)
        .run()
        .onComplete({ r =>
          println(r)
          system.terminate()
        })
    }
  }

  test("スーパーバイザー戦略") {
    implicit val system = ActorSystem("simple-stream")
    val decider: Supervision.Decider = {
      case _: IllegalArgumentException => Supervision.Resume
      case _                           => Supervision.Stop
    }
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withSupervisionStrategy(decider))
    implicit val executionContext = system.dispatcher
    val source = Source(1 to 5)
    val flow = Flow[Int].map(e =>
      if (e == 3) throw new IllegalArgumentException("oops!") else e)
    val sink = Sink.foreach[Int](e => println(s"element=$e"))
    source
      .via(flow)
      .toMat(sink)(Keep.right)
      .run()
      .recover {
        case _: IllegalArgumentException =>
          println("oops! IllegalArgumentException")
          "recovered"
      }
      .onComplete { r =>
        println(s"result:$r")
        system.terminate()
      }
  }

  test("AkkaStremTestKit") {
    new Fixture {
      val sourceUnderTest = Source(1 to 4).filter(_ % 2 == 0).map(_ * 2)
      sourceUnderTest
        .runWith(TestSink.probe[Int])
        .request(2)
        .expectNext(4, 8)
        .expectComplete()
    }
  }
}
