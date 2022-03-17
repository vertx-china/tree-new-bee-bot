package io.github.vertxchina.bots;

import io.vertx.core.Future;
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
  public Future<String> upload(byte[] picBytes) throws IOException {
    Buffer buffer = Buffer.buffer().appendBytes(picBytes);
    MultipartForm file = MultipartForm.create()
      .binaryFileUpload("file", "tmpPic.jpg", buffer, "application/octet-stream");
    return webClient.post(443, SERVER, UPLOAD_URI)
      .ssl(true)
      .sendMultipartForm(file)
      .map(resp -> {
        JsonArray jsonObject = resp.bodyAsJsonArray();
        String url = "https://" + SERVER + jsonObject.getJsonObject(0).getString("src");
        log.info("上传图片到Telegraph图床成功,地址:"+ url);
        return url;
      });
  }

  @Override
  public Future<String> upload(InputStream is) throws IOException {
    return upload(is.readAllBytes());
  }
}
