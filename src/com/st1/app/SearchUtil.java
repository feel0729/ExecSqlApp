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
   * 在"起始目錄"搜尋"目標名稱",根據"是否完全符合"進行比對.
   * 
   * @param fileFrom 起始目錄
   */
  public static void listDir(String fileFrom) {
    logger.trace("SearchUtil listDir ...");
    init();
    File dir = new File(fileFrom);
    // 若是一個目錄
    if (dir.isDirectory()) {
      // 列出目錄中的全部內容
      File[] nextDir = dir.listFiles();
      if (nextDir != null) {
        for (int i = 0; i < nextDir.length; i++) {
          File tmpFile = nextDir[i];
          if (tmpFile.isFile()) {
            // 路徑
            String filePath = tmpFile.getAbsolutePath();
            String fileName = tmpFile.getName();
            String subFileName = fileName.substring(fileName.lastIndexOf("."));
            // 副檔名限制為.sql
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
