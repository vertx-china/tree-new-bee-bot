package io.github.vertxchina.bots;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Leibniz on 2022/03/6 5:39 PM
 */
public interface PictureBed {
  String upload(byte[] picBytes) throws IOException;

  String upload(InputStream is) throws IOException;
}
