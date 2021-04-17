package net.aotter.vertx_web_mongo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.netty.handler.codec.http.HttpContent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.time.DateUtils;

public class MainVerticle extends AbstractVerticle {


  private MongoClient mongoClient;


  private final DateFormat dateFormat = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm'Z'");


  public MainVerticle() {
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }


  /**
   * app init here
   * @param startPromise
   * @throws Exception
   */
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    mongoClient = MongoClient.createShared(vertx,
      new JsonObject()
        .put("connection_string", "mongodb://localhost:27017")
        .put("db_name", "reactive-load-test")
    );

    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);

    router.get("/report").produces("text/plain").handler(this::report);

    server.requestHandler(router).listen(8080, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8080");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }


  /**
   * request handler
   * @param ctx
   */
  private void report(RoutingContext ctx) {
    upsert().onComplete(res -> {
      if (res.succeeded()) {
        Long count = res.result().getLong("count");
        ctx.response().end(String.format("#%d", count));
      } else {
        res.cause().printStackTrace();
        ctx.response().setStatusCode(500).end(res.cause().getMessage());
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
      "VertxWebMongo",
      new JsonObject().put("date",
        new JsonObject().put("$date", dateFormat.format(DateUtils.truncate(new Date(), Calendar.HOUR)))),
      new JsonObject().put("$inc", new JsonObject().put("count", 1L)),
      new FindOptions(),
      new UpdateOptions().setUpsert(true).setReturningNewDocument(true)
    );
  }

}
