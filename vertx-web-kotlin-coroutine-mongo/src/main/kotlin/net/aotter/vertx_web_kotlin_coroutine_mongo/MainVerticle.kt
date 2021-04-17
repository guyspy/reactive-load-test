package net.aotter.vertx_web_kotlin_coroutine_mongo

import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.FindOptions
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import net.aotter.vertx_web_kotlin_coroutine_mongo.extension.coroutineHandler
import org.apache.commons.lang3.time.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MainVerticle : CoroutineVerticle() {

  lateinit var mongoClient: MongoClient

  private val dateFormat: DateFormat = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm'Z'"
  )

  init {
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
  }


  /**
   * app init
   */
  override suspend fun start() {

    // connect to mongoDB
    mongoClient = MongoClient.createShared(
      vertx,
      jsonObjectOf(
        "connection_string" to "mongodb://localhost:27017",
        "db_name" to "reactive-load-test"
      )
    )

    // declare route
    val router = Router.router(vertx)
    router.get("/report").produces("text/plain").coroutineHandler {
      val result = upsert()
      it.response().end("#${result?.getLong("count") ?: "N/A"}")
    }

    // init server
    vertx.createHttpServer().requestHandler(router)
      .listen(8080) { println("HTTP server started on port 8080 succeeded: ${it.succeeded()}") }
  }


  /**
   * upsert mongo
   * Note: remember to add unique index to "hour" field
   */
  private suspend fun upsert(): JsonObject? = mongoClient.findOneAndUpdateWithOptions(
    "VertxWebKotlinCoroutineMongo",
    jsonObjectOf("date" to jsonObjectOf("\$date" to Date().toStartOfCurrentHour().formatIsoDate())),
    jsonObjectOf("\$inc" to jsonObjectOf("count" to 1L)),
    FindOptions(),
    UpdateOptions().setUpsert(true).setReturningNewDocument(true)
  ).await()


  private fun Date.toStartOfCurrentHour(): Date = DateUtils.truncate(this, Calendar.HOUR)


  private fun Date.formatIsoDate(): String = dateFormat.format(this)

}

