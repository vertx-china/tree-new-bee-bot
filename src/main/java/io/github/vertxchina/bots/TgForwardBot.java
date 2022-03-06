package io.github.vertxchina.bots;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

import java.util.List;

/**
 * @author Leibniz on 2022/03/6 11:13 AM
 */
public class TgForwardBot implements ForwardBot {
  private TelegramBot bot;
  private Long tgChatId;
  private PictureBed pictureBed;

  @Override
  public void init(JsonObject config, Vertx vertx) throws IllegalArgumentException {
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
    this.pictureBed = new TelegraphPictureBed(vertx);
  }

  @Override
  public void registerTreeNewBeeSocket(NetSocket socket, List<ForwardBot> bots) {
    // telegram -> treeNewBee
    this.bot.setUpdatesListener(updates -> {
      for (Update update : updates) {
        Message message = update.message();
        if (message != null) {
          User from = message.from();
          String nickName = from.lastName() + " " + from.firstName();
          String msgPrefix = "Tg的 " + nickName;
          final String msgText;
          if (message.text() != null) {
            msgText = message.text();
          } else if (message.sticker() != null) {
            //TODO
            msgText = "[发送了一个表情,暂不支持转发]";
          } else if (message.photo() != null) {
            //TODO
            String tmpMsgText;
            try {
              String fileId = message.photo()[message.photo().length - 1].fileId();
              GetFileResponse getFileResponse = bot.execute(new GetFile(fileId));
              byte[] photoBytes = bot.getFileContent(getFileResponse.file());
              String url = pictureBed.upload(photoBytes);
              tmpMsgText = message.caption() + "\n" + url;
            } catch (Exception e) {
              tmpMsgText = "[发送了一张瑟图并留言: " + message.caption() + ",暂不支持转发]";
            }
            msgText = tmpMsgText;
          } else if (message.animation() != null) {
            //TODO
            msgText = "[发送了一张GIF瑟瑟动图]";
          } else {
            System.out.println("==>暂不支持的消息: " + new Gson().toJson(message));
            msgText = null;
          }
          if (msgText != null) {
            socket.write(new JsonObject().put("message", msgPrefix + " 说: \n" + msgText) + "\r\n");
            bots.forEach(bot -> {
              if (bot != this) {
                try {
                  bot.sendMessage(new JsonObject().put("nickname", nickName).put("message", msgText), "Tg");
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        }
      }
      return UpdatesListener.CONFIRMED_UPDATES_ALL;
    });
  }

  @Override
  public void sendMessage(JsonObject messageJson, String msgSource) throws Exception {
    String content = msgSource + "的 *" + messageJson.getString("nickname") + "* 说: \n" + messageJson.getString("message");
    bot.execute(new SendMessage(tgChatId, content).parseMode(ParseMode.Markdown));
  }
}
