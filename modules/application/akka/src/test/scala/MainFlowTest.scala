import java.net.URI

import akka.actor.ActorSystem
import akka.stream._
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import company.MainFlow
import domain.company.CompanyRepository
import dynamodb.company.CompanyRepositoryOnDynamoDB
import org.scalatest.FunSuite
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.ExecutionContext

class MainFlowTest extends FunSuite with MainFlow {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit lazy val sqsClient = createAsyncClient("http://localhost:4576")
  override val companyRepository = createDynamoDBClient("http://localhost:4569")

  def createDynamoDBClient(
      endPoint: String): CompanyRepository with CompanyRepositoryOnDynamoDB =
    new CompanyRepository with CompanyRepositoryOnDynamoDB {
      override lazy val dynamoDBClient =
        AmazonDynamoDBClient
          .builder()
          .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("", "")))
          .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
              endPoint,
              Regions.AP_NORTHEAST_1.getName))
          .build()
    }

  def createAsyncClient(endPoint: String): SqsAsyncClient = {
    implicit val awsSqsClient = SqsAsyncClient
      .builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      .endpointOverride(URI.create(endPoint))
      .region(Region.AP_NORTHEAST_1)
      .build()
    system.registerOnTermination(awsSqsClient.close())
    awsSqsClient
  }

  test("Main Flowのテスト") {
    mainFlow("http://localhost:4576/queue/lspan-sqs").run().foreach(println)
    system.registerOnTermination(sqsClient.close())
  }
}
