package io.github.vertxchina.bots;

import io.github.vertxchina.ClassUtil;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leibniz on 2022/03/6 11:10 AM
 */
public interface ForwardBot {
  static List<ForwardBot> lookupAndInitAllBots(JsonObject config) {
    List<ForwardBot> bots = new ArrayList<>();
    List<Class<ForwardBot>> allBotClasses = ClassUtil.getAllClassByInterface(ForwardBot.class);
    if (allBotClasses != null) {
      for (Class<ForwardBot> botClass : allBotClasses) {
        try {
          ForwardBot bot = botClass.getDeclaredConstructor().newInstance();
          bot.init(config);
          System.out.println(botClass.getSimpleName() + " init success!");
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
  void init(JsonObject config) throws IllegalArgumentException;

  /**
   * 建立与 TreeNewBee 服务器的连接后调用
   * 建议实现类在这个方法里注册接收到自己方（如Telegram）消息时转发到 TreeNewBee 的操作
   *
   * @param socket 与 TreeNewBee 服务器的连接 Socket
   */
  void registerTreeNewBeeSocket(NetSocket socket);

  void sendMessage(JsonObject messageJson) throws Exception;
}
