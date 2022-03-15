package io.github.vertxchina.bots;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Leibniz on 2022/03/6 5:40 PM
 */
public class TelegraphPictureBed implements PictureBed {
  Logger log = LoggerFactory.getLogger(TelegraphPictureBed.class);
  private static final String SERVER = "telegra.ph";
  private static final String UPLOAD_URI = "/upload";

  private final WebClient webClient;

  public TelegraphPictureBed(Vertx vertx) {
    this.webClient = WebClient.create(vertx, new WebClientOptions().setMaxPoolSize(2));
  }

  @Override
  public String upload(byte[] picBytes) throws IOException {
    MultipartForm file = MultipartForm.create().binaryFileUpload("file", "tmpPic.jpg", Buffer.buffer().appendBytes(picBytes), "application/octet-stream");
    var response = webClient.post(443, SERVER, UPLOAD_URI)
      .ssl(true)
      .sendMultipartForm(file);

    //FIXME 错误姿势，但现在调用者需要阻塞等待它
    while (!response.isComplete()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    JsonArray jsonObject = response.result().bodyAsJsonArray();
    log.debug(jsonObject.encodePrettily());
    return "https://" + SERVER + jsonObject.getJsonObject(0).getString("src");
  }

  @Override
  public String upload(InputStream is) throws IOException {
    //TODO
    return null;
  }
}
