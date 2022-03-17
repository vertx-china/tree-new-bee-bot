package io.github.vertxchina.bots;

import io.vertx.core.Future;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Leibniz on 2022/03/6 5:39 PM
 */
public interface PictureBed {
  Future<String> upload(byte[] picBytes) throws IOException;

  Future<String> upload(InputStream is) throws IOException;
}
