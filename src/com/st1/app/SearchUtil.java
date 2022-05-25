package com.st1.app;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchUtil {
  private static final Logger logger = LogManager.getLogger();

  private static Map<String, String> allFiles;
  private static String allFileNames;

  private static void init() {
    logger.trace("SearchUtil init ...");
    allFiles = new HashMap<>();
    allFileNames = "";
  }

  /**
   * �b"�_�l�ؿ�"�j�M"�ؼЦW��",�ھ�"�O�_�����ŦX"�i����.
   * 
   * @param fileFrom �_�l�ؿ�
   */
  public static void listDir(String fileFrom) {
    logger.trace("SearchUtil listDir ...");
    init();
    File dir = new File(fileFrom);
    // �Y�O�@�ӥؿ�
    if (dir.isDirectory()) {
      // �C�X�ؿ������������e
      File[] nextDir = dir.listFiles();
      if (nextDir != null) {
        for (int i = 0; i < nextDir.length; i++) {
          File tmpFile = nextDir[i];
          if (tmpFile.isFile()) {
            // ���|
            String filePath = tmpFile.getAbsolutePath();
            String fileName = tmpFile.getName();
            String subFileName = fileName.substring(fileName.lastIndexOf("."));
            // ���ɦW���.sql
            if (subFileName.toUpperCase().equals(".SQL")) {
              allFiles.put(fileName, filePath);
              allFileNames += fileName + ",";
            }
          }
        }
        if (!allFiles.isEmpty()) {
          allFileNames = allFileNames.substring(0, allFileNames.lastIndexOf(","));
        }
      }
    }
  }

  /**
   * @return the allFiles
   */
  public static Map<String, String> getAllFiles() {
    logger.trace("SearchUtil getAllFiles ...");
    return allFiles;
  }

  /**
   * @return the allFileNames
   */
  public static String getAllFileNames() {
    logger.trace("SearchUtil getAllFileNames ...");
    return allFileNames;
  }
}
