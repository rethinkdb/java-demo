package com.rethinkdb.demo;

import io.vertx.core.Vertx;
import io.vertx.core.json.*;
import io.vertx.core.eventbus.EventBus;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.*;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;

import java.util.HashMap;
import java.util.List;

public class RethinkDemo {
  public static final RethinkDB r = RethinkDB.r;
  public static final String DBHOST = "rethinkdb-stable";

  public static void main(String[] args) {
    System.out.println("Attempting to start the application.");

    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);
    EventBus bus = vertx.eventBus();

    new Thread(() -> {
      Connection<?> conn = null;

      try {
        conn = r.connection().hostname(DBHOST).connect();
        Cursor<HashMap> cur = r.db("chat").table("messages").changes()
                               .getField("new_val").without("time").run(conn);

        while (cur.hasNext())
          bus.publish("chat", new JsonObject(cur.next()));
      }
      catch (Exception e) {
        System.err.println("Error: changefeed failed");
      }
      finally {
        conn.close();
      }
    }).start();

    router.route("/eventbus/*").handler(
      SockJSHandler.create(vertx).bridge(
        new BridgeOptions().addOutboundPermitted(
          new PermittedOptions().setAddress("chat"))));

    router.route(HttpMethod.GET, "/messages").blockingHandler(ctx -> {
      Connection<?> conn = null;

      try {
        conn = r.connection().hostname(DBHOST).connect();

        List<HashMap> items = r.db("chat").table("messages")
                               .orderBy().optArg("index", r.desc("time"))
                               .limit(20)
                               .orderBy("time")
                               .run(conn);

        ctx.response()
           .putHeader("content-type", "application/json")
           .end(Json.encodePrettily(items));
      }
      catch (Exception e) {
        ctx.response()
           .setStatusCode(500)
           .putHeader("content-type", "application/json")
           .end("{\"success\": false}");
      }
      finally {
        conn.close();
      }
    });

    router.route(HttpMethod.POST, "/send").handler(BodyHandler.create());
    router.route(HttpMethod.POST, "/send").blockingHandler(ctx -> {
      JsonObject data = ctx.getBodyAsJson();

      if (data.getString("user") == null || data.getString("text") == null) {
        ctx.response()
           .setStatusCode(500)
           .putHeader("content-type", "application/json")
           .end("{\"success\": false, \"err\": \"Invalid message\"}");

        return;
      }

      Connection<?> conn = null;

      try {
        conn = r.connection().hostname(DBHOST).connect();

        r.db("chat").table("messages").insert(
          r.hashMap("text", data.getString("text"))
              .with("user", data.getString("user"))
              .with("time", r.now())).run(conn);

        ctx.response()
           .putHeader("content-type", "application/json")
           .end("{\"success\": true}");
      }
      catch (Exception e) {
        ctx.response()
           .setStatusCode(500)
           .putHeader("content-type", "application/json")
           .end("{\"success\": false}");
      }
      finally {
        conn.close();
      }
    });

    router.route().handler(StaticHandler.create().setWebRoot("public").setCachingEnabled(false));
    vertx.createHttpServer().requestHandler(router::accept).listen(8000);
  }
}
