package hu.sztaki.spark

import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, Response}
import hu.sztaki.spark
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.conn.ssl.{TrustAllStrategy, TrustSelfSignedStrategy}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, FullTypeHints}
import retry.Success

import javax.net.ssl.SSLSession
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Failure

class Elastic()(implicit configuration: disqus.Configuration) extends Logger {

  val & = new Serializable {

    val host =
      configuration.get[String]("squs.output.elastic-search.host")

    val index =
      configuration.get[String]("squs.output.elastic-search.index")

    val `allow-insecure` =
      configuration.get[Boolean]("squs.output.elastic-search.allow-insecure")

    val user =
      configuration.get[String]("squs.output.elastic-search.user")

    val password =
      configuration.get[String]("squs.output.elastic-search.password")

  }

  protected lazy val authenticationProvider = {
    val provider = new BasicCredentialsProvider
    val credentials = new UsernamePasswordCredentials(&.user, &.password)
    provider.setCredentials(AuthScope.ANY, credentials)
    provider
  }

  val client =
    if (&.`allow-insecure`) {
      val sslContext = SSLContexts
        .custom()
        .loadTrustMaterial(new TrustAllStrategy())
        .build()

      object trustAllHostnameVerifier extends javax.net.ssl.HostnameVerifier {
        def verify(h: String, s: SSLSession) = true
      }

      val sslSessionStrategy = new SSLIOSessionStrategy(
        sslContext,
        trustAllHostnameVerifier
      )

      val myHttpAsyncClientCallback = new RestClientBuilder.HttpClientConfigCallback() {
        override def customizeHttpClient(
          httpClientBuilder: HttpAsyncClientBuilder
        ): HttpAsyncClientBuilder =
          httpClientBuilder
            .setSSLStrategy(sslSessionStrategy)
            .setDefaultCredentialsProvider(authenticationProvider)
      }

      ElasticClient(JavaClient(
        props = ElasticProperties(
          &.host
        ),
        httpClientConfigCallback = myHttpAsyncClientCallback,
        requestConfigCallback = (requestConfigBuilder: Builder) => requestConfigBuilder
      ))
    } else {
      ElasticClient(JavaClient(
        props = ElasticProperties(
          &.host
        )
      ))
    }

  implicit val insertSuccess: Success[Response[_]] = new Success[Response[_]](_.isSuccess)

  def insertSync(video: spark.Thread): Response[IndexResponse] =
    Await.result(
      insertAsync(video),
      Int.MaxValue seconds
    )

  def insertSync(comment: Comment): Response[IndexResponse] =
    Await.result(
      insertAsync(comment),
      Int.MaxValue seconds
    )

  def insertAsync(comment: Comment): Future[Response[IndexResponse]] =
    retry.Backoff(max = Int.MaxValue)(odelay.Timer.default) {
      () =>
        log.trace("Write attempt of comment [{}] to Elastic.", comment.ID)
        val f = client.execute {
          indexInto(&.index).withId(comment.ID.stringify).doc(
            Serialization.write(comment)(Elastic.formats)
          ).refresh(
            RefreshPolicy.Immediate
          )
        }
        f.onComplete {
          case Failure(exception) =>
            log.trace(
              "Failed to write comment [{}] to Elastic due to error [{}] " +
                "with message [{}]!",
              comment.ID,
              exception.getClass.getName,
              exception.getMessage
            )
          case util.Success(_) =>
            log.trace("Successful write of comment [{}] to Elastic.", comment.ID)
        }
        f
    }

  def insertAsync(thread: spark.Thread): Future[Response[IndexResponse]] =
    retry.Backoff(max = Int.MaxValue)(odelay.Timer.default) {
      () =>
        log.trace("Write attempt of video [{}] to Elastic.", thread.ID)
        val f = client.execute {
          indexInto(&.index).withId(thread.ID.stringified).doc(
            Serialization.write(thread)(Elastic.formats)
          ).refresh(
            RefreshPolicy.Immediate
          )
        }
        f.onComplete {
          case Failure(exception) =>
            log.trace(
              "Failed to write video [{}] to Elastic due to error [{}] " +
                "with message [{}]!",
              thread.ID,
              exception.getClass.getName,
              exception.getMessage
            )
          case util.Success(_) =>
            log.trace("Successful write of video [{}] to Elastic.", thread.ID)
        }
        f
    }

}

object Elastic {

  val formats = DefaultFormats
    .withHints(FullTypeHints(typeHintFieldName = "type", hints = List.empty))

  object Cache {
    protected var elastic: Option[Elastic] = None

    def get()(implicit configuration: disqus.Configuration): Elastic =
      synchronized {
        elastic match {
          case Some(e) => e
          case None =>
            elastic = Try.tryHard {
              Some(new Elastic())
            }
            elastic.get
        }
      }

  }

}
