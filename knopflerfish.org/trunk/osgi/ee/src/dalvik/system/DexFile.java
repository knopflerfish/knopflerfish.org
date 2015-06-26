package dalvik.system;

import java.io.File;
import java.util.Enumeration;

public class DexFile {

  public DexFile(File file) {}

  public DexFile(String fileName) {}


  public void close() {}

  public Enumeration<String> entries() {
    return null;
  } 

  protected  void finalize() {}

  public String getName() {
    return null;
  }

  public static boolean isDexOptNeeded(String fileName) {
    return false;
  }

  public Class loadClass(String name, ClassLoader loader) {
    return null;
  }

  public static DexFile loadDex(String sourcePathName, String outputPathName, int flags) {
    return null;
  }

}
