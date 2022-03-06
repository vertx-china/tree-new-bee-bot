package io.github.vertxchina;

import io.github.vertxchina.bots.ForwardBot;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;

import java.util.List;

/**
 * @author Leibniz on 2022/03/5 7:21 PM
 */
public class BotServerVerticle extends AbstractVerticle {


  @Override
  public void start() throws Exception {
    System.out.println("BotServerVerticle starting...");

    //config check
    var tnbServer = config().getString("treeNewBee.server");
    if (StringUtil.isNullOrEmpty(tnbServer)) {
      throw new IllegalArgumentException("config: treeNewBee.server is empty!!!");
    }
    var tnbPort = config().getInteger("treeNewBee.port");
    if (tnbPort == null) {
      throw new IllegalArgumentException("config: treeNewBee.port is empty!!!");
    }
    System.out.println("BotServerVerticle config check pass...");

    List<ForwardBot> bots = ForwardBot.lookupAndInitAllBots(config());
    createTreeNewBeeClient(tnbServer, tnbPort, bots);
  }

  private void createTreeNewBeeClient(String tnbServer, Integer tnbPort, List<ForwardBot> bots) {
    vertx.createNetClient().connect(tnbPort, tnbServer).compose(socket -> {
        //上传昵称
        socket.write(new JsonObject().put("nickname", "Tg群转发Bot").toString() + "\r\n");

        //通知bot已连接TreeNewBee
        bots.forEach(bot -> bot.registerTreeNewBeeSocket(socket));

        //第一次连接后会发一堆历史消息，现在没区分，不友好 所以等5秒后再开始处理
        vertx.setTimer(5000L, tid -> {
          System.out.println("BotServerVerticle start to receive TreeNewBee's messages...");
          socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
            String json = buffer.toString();
            try {
              var messageJson = new JsonObject(json);
              if (messageJson.containsKey("message")) {
                //通知 Bot 转发到自己平台
                bots.forEach(bot -> {
                  try {
                    bot.sendMessage(messageJson);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                });
              }
            } catch (Exception e) {
              System.out.println("Parsing error when receive: " + json);
            }
          }).maxRecordSize(1024 * 64));
        });

        //断开重连
        socket.closeHandler(v -> vertx.setTimer(5000L, tid -> {
          System.out.println("TreeNewBee socket is closed, trying to reconnect...");
          createTreeNewBeeClient(tnbServer, tnbPort, bots);
        }));
        return Future.succeededFuture();
      })
      .onSuccess(v -> System.out.println("Connected to TreeNewBee server: " + tnbServer + ":" + tnbPort))
      .onFailure(e -> System.out.println("Connect to TreeNewBee server: " + tnbServer + ":" + tnbPort + " failed: " + e.getMessage()));
  }
}
