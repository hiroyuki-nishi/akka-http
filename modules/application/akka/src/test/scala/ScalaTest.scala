import java.net.URI
import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import company.ParseFlow
import org.scalatest.FunSuite
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.{
  CreateQueueRequest,
  DeleteQueueRequest,
  DeleteQueueResponse,
  ReceiveMessageRequest
}
import software.amazon.awssdk.services.sqs.{SqsAsyncClient, model}

import scala.concurrent.ExecutionContext

class ScalaTest extends FunSuite with ParseFlow {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val sqsEndpoint = "http://localhost:4576"
  val queueUrl = "http://localhost:4576/queue/lspan-sqs"
  implicit lazy val sqsClient = createAsyncClient(sqsEndpoint)

  def createAsyncClient(endPoint: String): SqsAsyncClient = {
    implicit val awsSqsClient = SqsAsyncClient
      .builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create("", "")))
      .endpointOverride(URI.create(sqsEndpoint))
      .region(Region.AP_NORTHEAST_1)
      .build()
    system.registerOnTermination(awsSqsClient.close())
    awsSqsClient
  }

  private def createQueueRequest(queName: String): CreateQueueRequest =
    CreateQueueRequest
      .builder()
      .queueName(queName)
      .build()

  private def createQueue(queName: String): String =
    sqsClient.createQueue(createQueueRequest(queName)).get().queueUrl()

  private def deleteQueue(queUrl: String): DeleteQueueResponse =
    sqsClient
      .deleteQueue(DeleteQueueRequest.builder().queueUrl(queUrl).build())
      .get()

  private def receive(queUrl: String): util.List[model.Message] = {
    sqsClient
      .receiveMessage(ReceiveMessageRequest.builder().queueUrl(queUrl).build())
      .get()
      .messages()
  }

  test("SQSのキューを作成できる") {
    println(createQueue("hoge-sqs"))
  }

  test("SQSのキューを削除できる") {
    println(deleteQueue("http://localhost:4576/queue/hoge-sqs"))
  }

  test("SQSのキューを受信できる") {
    println(receive(queueUrl))
  }
}
