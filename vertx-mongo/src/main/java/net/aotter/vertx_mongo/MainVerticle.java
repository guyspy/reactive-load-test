package net.aotter.vertx_mongo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.commons.lang3.time.DateUtils;

public class MainVerticle extends AbstractVerticle {

  MongoClient mongoClient;

  DateFormat dateFormat;

  public MainVerticle() {
    dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

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

  private void report(io.vertx.core.http.HttpServerRequest req) {
    // Note: remember to add unique index to "hour" field
    mongoClient.findOneAndUpdateWithOptions(
      "VertxMongo",
      new JsonObject().put("date",
        new JsonObject().put("$date", dateFormat.format(DateUtils.round(new Date(), Calendar.HOUR)))),
      new JsonObject().put("$inc", new JsonObject().put("count", 1L)),
      new FindOptions(),
      new UpdateOptions().setUpsert(true).setReturningNewDocument(true)
    ).onComplete(res -> {
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
}
