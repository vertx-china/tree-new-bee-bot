package io.github.vertxchina;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import io.netty.util.internal.StringUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;

/**
 * @author Leibniz on 2022/03/5 7:21 PM
 */
public class TgBotServerVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    System.out.println("TgBotServerVerticle starting...");

    //config check
    var botToken = config().getString("botToken");
    if (StringUtil.isNullOrEmpty(botToken)) {
      throw new IllegalArgumentException("config: botToken is empty!!!");
    }
    var chatId = config().getLong("chatId");
    if (chatId == null) {
      throw new IllegalArgumentException("config: chatId is null!!!");
    }
    var tnbServer = config().getString("treeNewBee.server");
    if (StringUtil.isNullOrEmpty(tnbServer)) {
      throw new IllegalArgumentException("config: treeNewBee.server is empty!!!");
    }
    var tnbPort = config().getInteger("treeNewBee.port");
    if (tnbPort == null) {
      throw new IllegalArgumentException("config: treeNewBee.port is empty!!!");
    }
    System.out.println("TgBotServerVerticle config check pass...");

    TelegramBot bot = new TelegramBot(botToken);

    createTreeNewBeeClient(chatId, tnbServer, tnbPort, bot);
  }

  private void createTreeNewBeeClient(Long chatId, String tnbServer, Integer tnbPort, TelegramBot bot) {
    vertx.createNetClient().connect(tnbPort, tnbServer).compose(socket -> {
      //上传昵称
      socket.write(new JsonObject().put("nickname", "Tg群转发Bot").toString() + "\r\n");

      // telegram -> treeNewBee
      bot.setUpdatesListener(updates -> {
        for (Update update : updates) {
          Message message = update.message();
          if (message != null) {
            User from = message.from();
            String msgPrefix = "Tg的 " + from.lastName() + " " + from.firstName();
            if (message.text() != null) {
              socket.write(new JsonObject().put("message", msgPrefix + " 说: \n" + message.text()).toString() + "\r\n");
            } else if (message.sticker() != null) {
              //TODO
              socket.write(new JsonObject().put("message", msgPrefix + " \n" + "发送了一个表情,暂不支持转发").toString() + "\r\n");
            } else if (message.photo() != null) {
              //TODO
              socket.write(new JsonObject().put("message", msgPrefix + " \n" + "发送了一张瑟图并留言: " + message.caption() + ",特别特别瑟瑟,暂不支持转发").toString() + "\r\n");
            } else if (message.animation() != null) {
              //TODO
              socket.write(new JsonObject().put("message", msgPrefix + " \n" + "发送了一张GIF瑟瑟动图").toString() + "\r\n");
            } else {
              System.out.println("==>暂不支持的消息: " + new Gson().toJson(message));
            }
          }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
      });

      // treeNewBee -> telegram
      //第一次连接后会发一堆历史消息，现在没区分，不友好 所以等5秒后再开始处理
      vertx.setTimer(5000L, tid -> {
        System.out.println("TgBotServerVerticle start to receive TreeNewBee's messages...");
        socket.handler(RecordParser.newDelimited("\r\n", buffer -> {
          String json = buffer.toString();
          try {
            var messageJson = new JsonObject(json);
            if (messageJson.containsKey("message")) {
              String content = "树新蜂的 *" + messageJson.getString("nickname") + "* 说: \n" + messageJson.getString("message");
              bot.execute(new SendMessage(chatId, content).parseMode(ParseMode.Markdown));
            }
          } catch (Exception e) {
            System.out.println("Parsing error when receive: " + json);
          }
        }).maxRecordSize(1024 * 64));
      });

      //断开重连
      socket.closeHandler(v -> vertx.setTimer(5000L, tid -> createTreeNewBeeClient(chatId, tnbServer, tnbPort, bot)));
      return Future.succeededFuture();
    });
  }
}
