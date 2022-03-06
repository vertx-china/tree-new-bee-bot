package io.github.vertxchina.bots;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.response.GetFileResponse;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Leibniz on 2022/03/6 5:58 PM
 */
class TelegraphPictureBedTest {

  @Test
  public void uploadByteArrayTest() throws IOException {
    /*TelegramBot bot = new TelegramBot("fakeBotToken");
    GetFileResponse getFileResponse = bot.execute(new GetFile("AgACAgUAAx0CSbRx8AACAVZiI16-qJS17IIXYijVL9zOXiE32AAC8K4xG_oWIVW3IKQOpIkO6gEAAwIAA20AAyME"));
    byte[] photoBytes = bot.getFileContent(getFileResponse.file());
    String url = new TelegraphPictureBed(Vertx.vertx()).upload(photoBytes);
    System.out.println(url);*/
  }
}