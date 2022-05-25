package com.st1.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class PropertyUtil {
  private static final Logger logger = LogManager.getLogger();

  private final static String propertiesFilePath = "./ExecSqlApp.properties";

  private static List<Map<String, String>> properties;
  private static String dir;

  private static void init() {
    logger.trace("PropertyUtil init ...");
    dir = "";
    properties = new ArrayList<>();
  }

  public static List<Map<String, String>> getProperties() {
    logger.trace("PropertyUtil getProperties ...");
    readProperties();
    return properties;
  }

  public static String getDir() {
    logger.trace("PropertyUtil getDir ...");
    if (dir == null || dir.isEmpty()) {
      readProperties();
    }
    return dir == null ? "" : dir;
  }

  /**
   * Åª¨ú³]©wÀÉ
   * 
   * @throws IOException ...
   */
  private static void readProperties() {
    logger.trace("PropertyUtil readProperties ...");
    init();

    File fileProperties = new File(propertiesFilePath);

    String jsonString = "";
    try {
      FileInputStream fis;
      fis = new FileInputStream(fileProperties);
      InputStreamReader isw = new InputStreamReader(fis, StandardCharsets.UTF_8);
      BufferedReader br = new BufferedReader(isw);

      while (br.ready()) {
        jsonString += br.readLine();
      }
      br.close();
    } catch (FileNotFoundException e) {
      logger.error("PropertyUtil FileNotFoundException error = " + e.getMessage());
    } catch (IOException e) {
      logger.error("PropertyUtil IOException error = " + e.getMessage());
    }

    JSONObject jsonObj = new JSONObject(jsonString);
    dir = jsonObj.getString("dir");
    JSONArray dbProperties = jsonObj.getJSONArray("dbProperties");

    int size = dbProperties.length();

    for (int i = 0; i < size; i++) {
      JSONObject dbProperty = dbProperties.getJSONObject(i);
      String name = dbProperty.getString("name");
      String ip = dbProperty.getString("ip");
      String port = dbProperty.getString("port");
      String sid = dbProperty.getString("sid");
      String user = dbProperty.getString("user");
      String password = dbProperty.getString("password");
      String timeout = dbProperty.getString("timeout");
      Map<String, String> tmpMap = new HashMap<>();
      tmpMap.put("name", name);
      tmpMap.put("ip", ip);
      tmpMap.put("port", port);
      tmpMap.put("sid", sid);
      tmpMap.put("user", user);
      tmpMap.put("password", password);
      tmpMap.put("timeout", timeout);
      properties.add(tmpMap);
    }
  }
}
