package io.github.vertxchina;

import io.github.vertxchina.bots.ForwardBot;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;

import java.util.List;

/**
 * @author Leibniz on 2022/03/5 7:21 PM
 */
public class BotServerVerticle extends AbstractVerticle {
  Logger log = LoggerFactory.getLogger(BotServerVerticle.class);

  @Override
  public void start() throws Exception {
    log.info("BotServerVerticle starting...");

    //config check
    var tnbServer = config().getString("treeNewBee.server");
    if (StringUtil.isNullOrEmpty(tnbServer)) {
      throw new IllegalArgumentException("config: treeNewBee.server is empty!!!");
    }
    var tnbPort = config().getInteger("treeNewBee.port");
    if (tnbPort == null) {
      throw new IllegalArgumentException("config: treeNewBee.port is empty!!!");
    }
    var tnbNickname = config().getString("treeNewBee.nickname", "3群转发Bot");
    log.info("BotServerVerticle config check pass...");

    List<ForwardBot> bots = ForwardBot.lookupAndInitAllBots(config(), vertx);
    bots.forEach(bot -> bot.registerOtherBots(bots));
    connectTreeNewBee(tnbServer, tnbPort, tnbNickname, bots);
  }

  private void connectTreeNewBee(String tnbServer, Integer tnbPort, String nickname, List<ForwardBot> bots) {
    vertx.createNetClient()
      .connect(tnbPort, tnbServer)
      .compose(socket -> {
        //上传昵称
        socket.write(new JsonObject().put("nickname", nickname).toString() + "\r\n");

        //通知bot已连接TreeNewBee
        bots.forEach(bot -> bot.updateTnbSocket(socket));

        //第一次连接后会发一堆历史消息，现在没区分，不友好 所以等5秒后再开始处理
        vertx.setTimer(5000L, tid -> {
          log.info("BotServerVerticle start to receive TreeNewBee's messages...");
          //来自 TreeNewBee 的消息转发各个平台Bot
          socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
            String json = buffer.toString();
            try {
              var messageJson = new JsonObject(json);
              if (messageJson.containsKey("message")) {
                //通知 Bot 转发到自己平台
                bots.forEach(bot -> {
                  try {
                    bot.sendMessage(messageJson, "树新蜂");
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                });
              }
            } catch (Exception e) {
              log.error("Parsing error when receive: " + json + ":" + e.getMessage());
            }
          }).maxRecordSize(1024 * 64));
        });

        //Socket断开重连
        socket.closeHandler(v -> vertx.setTimer(5000L, tid -> {
          log.warn("TreeNewBee socket is closed, trying to reconnect...");
          connectTreeNewBee(tnbServer, tnbPort, nickname, bots);
        }));
        return Future.succeededFuture();
      })
      .onSuccess(v -> log.info("Connected to TreeNewBee server: " + tnbServer + ":" + tnbPort))
      .onFailure(e -> log.error("Connect to TreeNewBee server: " + tnbServer + ":" + tnbPort + " failed: " + e.getMessage(), e));
  }
}
