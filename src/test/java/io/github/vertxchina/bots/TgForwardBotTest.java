package io.github.vertxchina.bots;

import org.junit.jupiter.api.Test;

/**
 * @author Leibniz on 2022/3/9 10:14 PM
 */
class TgForwardBotTest {
    @Test
    public void markdownTgTest(){
       /* String botToken = "fakeBotToken";
        long chatId = 1111L;

        TelegramBot bot = new TelegramBot(botToken);
        String content = "树新蜂的 福州-赵承恩17768516 说: \n " +
                escapeUrl("https://www.baidu.com/img/pc_9c5c85e6b953f1d172e1ed6821618b91.png ");
        var response = bot.execute(new SendMessage(chatId, content).parseMode(ParseMode.Markdown));
        System.out.println("code:" + response.errorCode() + ", message:" + response.description());*/
    }

    @Test
    public void imageTgTest(){
       /* String botToken = "fakeBotToken";
        long chatId = 1111L;

        TgForwardBot bot = new TgForwardBot();
        bot.init(new JsonObject().put("telegram.botToken", botToken).put("telegram.chatId", chatId), Vertx.vertx());
        bot.sendImage("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", "*caption* test");*/
    }

/*    @Test
    public void receiveTest(){
        TelegramBot bot =  new TelegramBot("fakebottoken");
        bot.setUpdatesListener(d -> {
            for (Update update : d) {
                System.out.println(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        while (true) {

        }
    }*/
}