package io.github.vertxchina.bots;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

/**
 * @author Leibniz on 2022/03/6 11:13 AM
 */
public class TgForwardBot implements ForwardBot {
  private TelegramBot bot;
  private Long tgChatId;

  @Override
  public void init(JsonObject config) throws IllegalArgumentException {
    //config check
    String tgBotToken = config.getString("telegram.botToken");
    if (StringUtil.isNullOrEmpty(tgBotToken)) {
      throw new IllegalArgumentException("config: telegram.botToken is empty!!!");
    }
    tgChatId = config.getLong("telegram.chatId");
    if (tgChatId == null) {
      throw new IllegalArgumentException("config: telegram.chatId is null!!!");
    }

    this.bot = new TelegramBot(tgBotToken);
  }

  @Override
  public void registerTreeNewBeeSocket(NetSocket socket) {
    // telegram -> treeNewBee
    this.bot.setUpdatesListener(updates -> {
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
  }

  @Override
  public void sendMessage(JsonObject messageJson) throws Exception {
    String content = "树新蜂的 *" + messageJson.getString("nickname") + "* 说: \n" + messageJson.getString("message");
    bot.execute(new SendMessage(tgChatId, content).parseMode(ParseMode.Markdown));
  }
}
