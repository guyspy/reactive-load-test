package net.aotter.vertx_mongo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.commons.lang3.time.DateUtils;

public class MainVerticle extends AbstractVerticle {


  private MongoClient mongoClient;


  private final DateFormat dateFormat = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm'Z'");


  public MainVerticle() {
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }


  /**
   * app start
   *
   * @param startPromise
   *
   * @throws Exception
   */
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    mongoClient = MongoClient.createShared(vertx,
      new JsonObject()
        .put("connection_string", "mongodb://localhost:27017")
        .put("db_name", "reactive-load-test")
    );

    vertx.createHttpServer().requestHandler(req -> {
      if (req.method() == HttpMethod.GET && req.path().equals("/report")) {
        report(req);
      } else {
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!");
      }
    }).listen(8080, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8080");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }


  /**
   * report endpoint handler
   *
   * @param req
   */
  private void report(HttpServerRequest req) {
    upsert().onComplete(res -> {
      if (res.succeeded()) {
        Long count = res.result().getLong("count");
        req.response()
          .putHeader("content-type", "text/plain")
          .end(String.format("#%d", count));
      } else {
        res.cause().printStackTrace();
        req.response()
          .putHeader("content-type", "text/plain")
          .end(res.cause().getMessage());
      }
    });
  }


  /**
   * upsert into mongoDB
   *
   * @return
   */
  private Future<JsonObject> upsert() {
    // Note: remember to add unique index to "hour" field
    return mongoClient.findOneAndUpdateWithOptions(
      "VertxMongo",
      new JsonObject().put("date",
        new JsonObject().put("$date", dateFormat.format(DateUtils.truncate(new Date(), Calendar.HOUR)))),
      new JsonObject().put("$inc", new JsonObject().put("count", 1L)),
      new FindOptions(),
      new UpdateOptions().setUpsert(true).setReturningNewDocument(true)
    );
  }

}
