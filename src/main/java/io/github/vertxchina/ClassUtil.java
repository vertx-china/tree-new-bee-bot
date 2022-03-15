package io.github.vertxchina;

/**
 * @author Leibniz on 2022/03/6 11:41 AM
 */
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtil {
  static Logger log = LoggerFactory.getLogger(ClassUtil.class);

  /*
   * 获取指定接口的所有实现实例
   */
  public static <T> List<Object> getAllObjectByInterface(Class<T> c)
    throws InstantiationException, IllegalAccessException {
    List<Object> list = new ArrayList<Object>();
    List<Class<T>> classes = getAllClassByInterface(c);
    for (int i = 0; i < classes.size(); i++) {
      list.add(classes.get(i).newInstance());
    }
    return list;
  }

  /*
   * 获取指定接口的实例的Class对象
   */
  public static <T> List<Class<T>> getAllClassByInterface(Class<T> c) {
    // 如果传入的参数不是接口直接结束
    if (!c.isInterface()) {
      return null;
    }

    // 获取当前包名
    String packageName = c.getPackage().getName();
    List<Class<T>> allClass = null;
    try {
      allClass = getAllClassFromPackage(packageName);
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }

    ArrayList<Class<T>> list = new ArrayList<>();
    for (int i = 0; i < allClass.size(); i++) {
      if (c.isAssignableFrom(allClass.get(i))) {
        if (!c.equals(allClass.get(i))) {
          list.add(allClass.get(i));
        }
      }
    }

    return list;
  }

  private static <T> List<Class<T>> getAllClassFromPackage(String packageName) throws IOException, ClassNotFoundException{
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String path = packageName.replace(".", "/");
    Enumeration<URL> enumeration = classLoader.getResources(path);
    List<String> classNames = getClassNames(enumeration, packageName);
    log.info(classNames);

    ArrayList<Class<T>> classes = new ArrayList<>();
    for (int i = 0; i < classNames.size(); i++) {
      classes.add((Class<T>)Class.forName(classNames.get(i)));
    }

    return classes;
  }

  public static List<String> getClassNames(Enumeration<URL> enumeration, String packageName) {
    List<String> classNames = null;
    while (enumeration.hasMoreElements()) {
      URL url = enumeration.nextElement();
      if (url != null) {
        String type = url.getProtocol();
        if (type.equals("file")) {
          log.info("type : file");
          String fileSearchPath = url.getPath();
          if(fileSearchPath.contains("META-INF")) {
            log.info("continue + " + fileSearchPath);
            continue;
          }
          classNames = getClassNameByFile(fileSearchPath, packageName);
        } else if (type.equals("jar")) {
          try {
            log.info("type : jar");
            JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();
            JarFile jarFile = jarURLConnection.getJarFile();
            classNames = getClassNameByJar(jarFile, packageName);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else {
          log.info("type : none");
        }
      }
    }

    return classNames;
  }

  /*
   * 获取项目某路径下的所有类
   */
  public static List<String> getClassNameByFile(String fileSearchPath, String packageName) {
    List<String> classNames = new ArrayList<String>();
    String path = packageName.replace(".", "/");

    File file = new File(fileSearchPath);
    File[] childFiles = file.listFiles();
    for(File childFile : childFiles) {
      if(childFile.isDirectory()) {
        classNames.addAll(getClassNameByFile(childFile.getPath(), packageName));
      } else {
        String childFilePath = childFile.getPath();
        if (childFilePath.endsWith(".class")) {
          int packageIndex = childFilePath.indexOf(path);
          if (packageIndex >= 0) {
            String className = childFilePath.substring(packageIndex)
              .replace("/", ".")
              .replaceAll("\\.class$", "");
            classNames.add(className);
          }
        }
      }
    }

    return classNames;
  }

  /*
   * 从jar包中获取某路径下的所有类
   */
  public static List<String> getClassNameByJar(JarFile jarFile, String packageName) {
    String path = packageName.replace(".", "/");
    List<String> classNames = new ArrayList<String>();
    Enumeration<JarEntry> entrys = jarFile.entries();
    while (entrys.hasMoreElements()) {
      JarEntry jarEntry = (JarEntry) entrys.nextElement();
      String entryName = jarEntry.getName();
      if(entryName.endsWith(".class")) {
        int packageIndex = entryName.indexOf(path);
        if (packageIndex >= 0) {
          String className = entryName.substring(packageIndex)
            .replace("/", ".")
            .replaceAll("\\.class$", "");
          classNames.add(className);
        }
        /*String className = entryName.replace("/", ".");
        className = className.substring(0, className.indexOf(".class"));
        classNames.add(className);*/
      }

    }
    return classNames;
  }
}
