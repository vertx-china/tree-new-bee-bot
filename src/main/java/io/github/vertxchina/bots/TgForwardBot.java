package io.github.vertxchina.bots;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.GetFileResponse;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Leibniz on 2022/03/6 11:13 AM
 */
public class TgForwardBot implements ForwardBot {
  private final AtomicReference<NetSocket> socket = new AtomicReference<>();
  Logger log = LoggerFactory.getLogger(TgForwardBot.class);
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
  public void registerOtherBots(List<ForwardBot> bots) {
    // telegram -> treeNewBee
    this.bot.setUpdatesListener(updates -> {
      for (Update update : updates) {
        Message message = update.message();
        if (message != null) {
          User from = message.from();
          String nickName = from.lastName() + " " + from.firstName();
          final String msgText;
          String picUrl = null;
          if (message.text() != null) {
            msgText = message.text();
          } else if (message.sticker() != null) {
            //TODO
            msgText = "[发送了一个表情,暂不支持转发]";
          } else if (message.photo() != null) {
            String tmpText;
            String caption = Optional.ofNullable(message.caption()).orElse("");
            try {
              String fileId = message.photo()[message.photo().length - 1].fileId();
              GetFileResponse getFileResponse = bot.execute(new GetFile(fileId));
              byte[] photoBytes = bot.getFileContent(getFileResponse.file());
              String tmpPicUrl = pictureBed.upload(photoBytes);
              if (StringUtil.isNullOrEmpty(message.caption())) { //没有附言,直接发
                tmpText = tmpPicUrl;
              } else {
                tmpText = caption + " [发送了一张瑟图,见下条消息]";
                picUrl = tmpPicUrl;
              }
            } catch (Exception e) {
              tmpText = caption + " [发送了一张瑟图,暂不支持转发]";
            }
            msgText = tmpText;
          } else if (message.animation() != null) {
            //TODO
            msgText = "[发送了一张GIF瑟瑟动图]";
          } else {
            log.info("==>暂不支持的消息: " + new Gson().toJson(message));
            msgText = null;
          }
          if (msgText != null) {
            socket.get().write(new JsonObject().put("nickname", "Tg的 " + nickName).put("message", msgText) + "\r\n");
            if (picUrl != null) {
              //目前 TreeNewBee 只支持 纯 图片url 的图片消息
              socket.get().write(new JsonObject().put("nickname", "Tg的 " + nickName).put("message", picUrl) + "\r\n");
            }
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

  String previousUser = "";

  @Override
  public void updateTnbSocket(NetSocket socket) {
    NetSocket oldSocket = this.socket.get();
    this.socket.compareAndSet(oldSocket, socket);
  }

  @Override
  public void sendMessage(JsonObject messageJson, String msgSource) throws Exception {
    String message = "";//要被发送给电报的消息string
    var user = messageJson.getString("nickname", "匿名用户");
    if (!user.equals(previousUser)) {
      previousUser = user;
      message += msgSource + "的 *" + user + "* 说：\n";
    }

    if (messageJson.getValue("message") instanceof JsonObject jsonObject) {
      if (imgTypes.contains(jsonObject.getString("type")) &&
        jsonObject.getValue("base64") instanceof String imgBase64) {
        sendImage(imgBase64, message);
        return;
      }

      //有content就用content，没有就找url，还没有就用空字符串
      message += jsonObject.containsKey("content") ?
        escapeUrl(jsonObject.getString("content", "")) :
        escapeUrl(jsonObject.getString("url", ""));

      var response = bot.execute(new SendMessage(tgChatId, message).parseMode(ParseMode.Markdown));
      if (!response.isOk()) {
        log.warn("Send '" + message + "' to telegram but response with code:" + response.errorCode() + "and message:" + response.description());
      }
    } else {
      String messageText = messageJson.getString("message", "");
      if (imgBase64Pattern.matcher(messageText).find()) {
        sendImage(messageText, message);
        return;
      }
      message += escapeUrl(messageText);
      var response = bot.execute(new SendMessage(tgChatId, message).parseMode(ParseMode.Markdown));
      if (!response.isOk()) {
        log.warn("Send '" + message + "' to telegram but response with code:" + response.errorCode() + "and message:" + response.description());
      }
    }
  }

  void sendImage(String imgBase64, String caption) {
    Matcher matcher = imgBase64Pattern.matcher(imgBase64);
    if (matcher.find()) {
      imgBase64 = matcher.group(2);
    }
    byte[] imgBytes = Base64.getDecoder().decode(imgBase64);
    var response = bot.execute(new SendPhoto(tgChatId, imgBytes).caption(caption));
    if (!response.isOk()) {
      log.warn("Send photo with caption '" + caption + "' to telegram but response with code:" + response.errorCode() + "and message:" + response.description());
    }
  }

  private static final Set<String> imgTypes = ImmutableSet.of("image", "img", "1");
  private static final Pattern imgBase64Pattern = Pattern.compile("^data:image/(png|jpeg|jpg|gif);base64,(.+)");
  private static final Pattern urlPattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
  private static final Pattern imgPattern = Pattern.compile("(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|](png|jpg|jepg|gif)");

  public static String escapeUrl(String message) {
    return urlPattern.matcher(message).replaceAll("[$0]()");
  }

  public static String escapeImage(String message) {
    return imgPattern.matcher(message).replaceAll("![$0]($0)");
  }
}
