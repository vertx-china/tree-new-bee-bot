package io.github.vertxchina.bots;

import io.github.vertxchina.ClassUtil;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leibniz on 2022/03/6 11:10 AM
 */
public interface ForwardBot {
  static List<ForwardBot> lookupAndInitAllBots(JsonObject config, Vertx vertx) {
    List<ForwardBot> bots = new ArrayList<>();
    List<Class<ForwardBot>> allBotClasses = ClassUtil.getAllClassByInterface(ForwardBot.class);
    if (allBotClasses != null) {
      for (Class<ForwardBot> botClass : allBotClasses) {
        try {
          ForwardBot bot = botClass.getDeclaredConstructor().newInstance();
          bot.init(config, vertx);
          LoggerFactory.getLogger(ForwardBot.class).info(botClass.getSimpleName() + " init success!");
          bots.add(bot);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return bots;
  }

  /**
   * 初始化Bot
   *
   * @param config 配置
   * @throws IllegalArgumentException 配置缺失导致无法初始化的情况
   */
  void init(JsonObject config, Vertx vertx) throws IllegalArgumentException;

  /**
   * 建立与 TreeNewBee 服务器的连接后调用
   * 建议实现类在这个方法里注册接收到自己方（如Telegram）消息时转发到 TreeNewBee 及 其他bot 的操作
   *
   * @param bots 所有bot，用于调用 sendMessage() 转发其他平台
   */
  void registerOtherBots(List<ForwardBot> bots);

  void updateTnbSocket(NetSocket socket);

  /**
   * 需要往自身平台转发的消息
   *
   * @param messageJson 消息Json，包含 nickname message
   * @param msgSource   消息来源平台
   * @throws Exception 发送消息时的IO异常之类
   */
  void sendMessage(JsonObject messageJson, String msgSource) throws Exception;
}
