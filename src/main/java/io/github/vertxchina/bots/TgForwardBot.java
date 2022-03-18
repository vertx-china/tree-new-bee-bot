package io.github.vertxchina.bots;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
import io.netty.util.internal.StringUtil;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.*;
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
  private List<ForwardBot> allBots;
  private Vertx vertx;
  private WebClient webClient;

  @Override
  public void init(JsonObject config, Vertx vertx) throws IllegalArgumentException {
    this.vertx = vertx;
    this.webClient = WebClient.create(vertx, new WebClientOptions().setMaxPoolSize(3));
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
    this.allBots = bots;
    // telegram -> treeNewBee
    this.bot.setUpdatesListener(updates -> {
      for (Update update : updates) {
        Message message = update.message();
        if (message != null) {
          User from = message.from();
          String nickName = from.lastName() + " " + from.firstName();
          if (message.text() != null) {
            sendToTnb(nickName, message.text());
            sendToOtherBots(nickName, message.text());
          } else if (message.sticker() != null) {
            String fileId = message.sticker().fileId();
            sendImageFromFileId(message, fileId, TgMsgType.STICKER);
          } else if (message.photo() != null) {
            String fileId = message.photo()[message.photo().length - 1].fileId();
            sendImageFromFileId(message, fileId, TgMsgType.PHOTO);
          } else if (message.animation() != null) {
            String fileId = message.animation().fileId();
            sendImageFromFileId(message, fileId, TgMsgType.ANIMATION);
          } else {
            log.info("==>暂不支持的消息: " + new Gson().toJson(message));
          }
        }
      }
      return UpdatesListener.CONFIRMED_UPDATES_ALL;
    });
  }

  private void sendImageFromFileId(Message message, String fileId, TgMsgType type) {
    String nickName = message.from().lastName() + " " + message.from().firstName();
    String caption = Optional.ofNullable(message.caption()).orElse("");
    try {
      GetFileResponse getFileResponse = bot.execute(new GetFile(fileId));
      byte[] photoBytes = bot.getFileContent(getFileResponse.file());
      Future<?> msgContentFuture;
      if (photoBytes.length < base64Threshold) {
        msgContentFuture = Future.succeededFuture(new JsonObject()
          .put("type", type.tnbMsgType)
          .put("base64", "data:" + type.mimeType + ";base64," + new String(Base64.getEncoder().encode(photoBytes))));
      } else {
        msgContentFuture = pictureBed.upload(photoBytes);
      }
      msgContentFuture.onSuccess(msgContent -> {
        if (StringUtil.isNullOrEmpty(message.caption())) { //没有附言,直接发
          sendToTnb(nickName, msgContent);
        } else {
          sendToTnb(nickName, caption + " [发送了一张" + type.chineseName + ",见下条消息]");
          sendToTnb(nickName, msgContent);
        }
        sendToOtherBots(nickName, msgContent);
      }).onFailure(e ->
        log.error("上传图床失败:" + e.getMessage(), e));
    } catch (Exception e) {
      String fallbackMsg = caption + " [发送了一张" + type.chineseName + ",暂不支持转发]";
      sendToTnb(nickName, fallbackMsg);
      sendToOtherBots(nickName, fallbackMsg);
      log.error("发送TG的" + type.tnbMsgType + "时出错:" + e.getMessage(), e);
    }
  }

  private enum TgMsgType {
    PHOTO("瑟图", "image/jpg", "image"),
    STICKER("瑟瑟表情", "image/webp", "image"),
    ANIMATION("瑟瑟动图", "video/mp4", "video");

    final String chineseName;
    final String mimeType;
    final String tnbMsgType;

    TgMsgType(String chineseName, String mimeType, String tnbMsgType) {
      this.chineseName = chineseName;
      this.mimeType = mimeType;
      this.tnbMsgType = tnbMsgType;
    }
  }

  private void sendToTnb(String nickName, Object messageContent) {
    socket.get().write(new JsonObject().put("nickname", "Tg的 " + nickName).put("message", messageContent) + "\r\n");
  }

  private void sendToOtherBots(String nickName, Object messageContent) {
    this.allBots.forEach(bot -> {
      if (bot != this) {
        try {
          bot.sendMessage(new JsonObject().put("nickname", nickName).put("message", messageContent), "Tg");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

//  String previousUser = ""; //由于其他tg用户会发言,这样会造成不连贯

  @Override
  public void updateTnbSocket(NetSocket socket) {
    NetSocket oldSocket = this.socket.get();
    this.socket.compareAndSet(oldSocket, socket);
  }

  @Override
  public void sendMessage(JsonObject messageJson, String msgSource) {
    var user = messageJson.getString("nickname", "匿名用户");
    String message = msgSource + " " + user + "：\n";//要被发送给电报的消息string

    if (messageJson.getValue("message") instanceof JsonObject jsonObject) {
      //有content就用content，没有就找url，还没有就用空字符串
      var msgContent = jsonObject.containsKey("content") ?
        jsonObject.getString("content", "") :
        jsonObject.getString("url", "");
      if (imgUrlPattern.matcher(msgContent.trim()).matches()) { //消息只有一个图片url
        sendImageToTgByUrl(msgContent.trim(), message);
      } else if (imgTypes.contains(jsonObject.getString("type")) &&
        jsonObject.getValue("base64") instanceof String imgBase64) {
        sendImageToTgByBase64(imgBase64, message);
      } else {
        message += msgContent;
        sendToTgAsync(new SendMessage(tgChatId, message), "'" + message + "'");
      }
    } else {
      String messageText = messageJson.getString("message", "");
      if (imgUrlPattern.matcher(messageText).find()) { //消息有图片url
        sendImageToTgByUrl(messageText, message);
      } else if (imgBase64Pattern.matcher(messageText).find()) {
        sendImageToTgByBase64(messageText, message);
      } else {
        message += messageText;
        sendToTgAsync(new SendMessage(tgChatId, message), "'" + message + "'");
      }
    }
  }

  void sendImageToTgByBase64(String imgBase64, String caption) {
    Matcher matcher = imgBase64Pattern.matcher(imgBase64);
    if (matcher.find()) {
      imgBase64 = matcher.group(2);
    }
    byte[] imgBytes = Base64.getDecoder().decode(imgBase64);
    sendToTgAsync(new SendPhoto(tgChatId, imgBytes).caption(caption), "photo with caption '" + caption + "'");
  }

  void sendImageToTgByUrl(String msgWithImgUrl, String caption) {
    var matcher = imgUrlPattern.matcher(msgWithImgUrl);
    List<String> imgUrlList = new ArrayList<>();
    int findStart = 0;
    while (matcher.find(findStart)) {
      String imgUrl = matcher.group(0);
      imgUrlList.add(imgUrl);
      findStart = matcher.end();
    }
    var finalCaption = caption + imgUrlPattern.matcher(msgWithImgUrl).replaceAll(" ");
    CompositeFuture.all(imgUrlList.stream()
        .map(imgUrl -> (Future) webClient.getAbs(imgUrl)
          .ssl(imgUrl.startsWith("https://"))
          .send())
        .toList())
      .onSuccess(cf -> {
        if (cf.list().size() == 1) {
          HttpResponse<Buffer> resp = (HttpResponse<Buffer>) cf.list().get(0);
          sendToTgAsync(new SendPhoto(tgChatId, resp.body().getBytes())
            .caption(finalCaption), "photo with caption '" + finalCaption + "'");
        } else {
          InputMedia[] media = new InputMedia[cf.list().size()];
          for (int i = 0; i < cf.list().size(); i++) {
            HttpResponse<Buffer> resp = (HttpResponse<Buffer>) cf.list().get(i);
            media[i] = new InputMediaPhoto(resp.body().getBytes());
          }
          sendToTgAsync(new SendMediaGroup(tgChatId, media), "photo");
          sendToTgAsync(new SendMessage(tgChatId, finalCaption), "message '" + finalCaption + "'");
        }
      })
      .onFailure(e -> {
        log.error("获取消息的图片失败(" + msgWithImgUrl + "):" + e.getMessage(), e);
        var message = caption + msgWithImgUrl;
        sendToTgAsync(new SendMessage(tgChatId, message), "'" + message + "'");
      });
  }

  private <T extends BaseRequest<T, R>, R extends BaseResponse> void sendToTgAsync(BaseRequest<T, R> request, String content) {
    vertx.executeBlocking(promise -> {
      var response = bot.execute(request);
      if (!response.isOk()) {
        log.warn("Send " + content + " to telegram but response with code:" + response.errorCode() + "and message:" + response.description());
      }
    }, false, res -> {
      if (!res.succeeded()) {
        log.error(res.cause().getMessage(), res.cause());
      }
    });
  }

  private static final int base64Threshold = 64 * 1024 / 4 * 3;
  private static final Set<String> imgTypes = ImmutableSet.of("image", "img", "1");
  private static final Pattern imgBase64Pattern = Pattern.compile("^data:image/(png|jpeg|jpg|gif);base64,(.+)");

  private static final Pattern urlPattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
  private static final Pattern imgUrlPattern = Pattern.compile("(https?)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|](png|jpg|jpeg|gif)");

  public static String escapeUrlForMarkdown(String message) {
    return urlPattern.matcher(message).replaceAll("[$0]()");
  }

  public static String escapeImageForMarkdown(String message) {
    return imgUrlPattern.matcher(message).replaceAll("![$0]($0)");
  }
}
