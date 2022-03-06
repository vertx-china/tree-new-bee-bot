package io.github.vertxchina;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainLauncher extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    System.out.println("Starting deploy verticle(s)...");
    vertx.deployVerticle(BotServerVerticle.class.getName(), new DeploymentOptions().setConfig(config()))
      .onSuccess(id -> System.out.println("deploy " + BotServerVerticle.class.getSimpleName() + " success!"))
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
