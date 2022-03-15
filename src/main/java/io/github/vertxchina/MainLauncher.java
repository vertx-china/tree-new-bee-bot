package io.github.vertxchina;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

public class MainLauncher extends AbstractVerticle {
  Logger log = LoggerFactory.getLogger(MainLauncher.class);
  @Override
  public void start(Promise<Void> startPromise) {
    log.info("Starting deploy verticle(s)...");
    vertx.deployVerticle(BotServerVerticle.class.getName(), new DeploymentOptions().setConfig(config()))
      .onSuccess(id -> log.info("deploy " + BotServerVerticle.class.getSimpleName() + " success!"))
      .onFailure(startPromise::fail);
  }

  //FOR 本地测试
  public static void main(String... args) {
    Vertx vertx = Vertx.vertx();
    vertx
      .fileSystem()
      .readFile("config.json")
      .compose(buffer -> vertx.deployVerticle(MainLauncher.class.getName(),
        new DeploymentOptions().setConfig(new JsonObject(buffer))))
      .onFailure(e -> {
        e.printStackTrace();
        System.exit(1);
      });
  }
}
