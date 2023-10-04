package com.st1.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecordUtil {
    private static final Logger logger = LogManager.getLogger();

    private final static String recordFilePath = "./ExecSqlApp.rec";

    private static Map<String, String> record;

    public static Map<String, String> getRecord() {
        logger.trace("RecordUtil getRecord ...");
        loadRecord();
        logger.trace("RecordUtil getRecord return record.");
        return record;
    }

    private static void init() {
        logger.trace("RecordUtil init ...");
        record = new HashMap<>();
    }

    private static void loadRecord() {
        logger.info("RecordUtil loadRecord ...");
        init();

        File recordFile = new File(recordFilePath);

        List<String> everyLine = new ArrayList<>();

        try {
            FileInputStream fis;
            fis = new FileInputStream(recordFile);
            InputStreamReader isw = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isw);

            while (br.ready()) {
                everyLine.add(br.readLine());
            }
            br.close();
        } catch (FileNotFoundException e) {
            logger.error("RecordUtil FileNotFoundException error = " + e.getMessage());
        } catch (IOException e) {
            logger.error("RecordUtil IOException error = " + e.getMessage());
        }

        for (String line : everyLine) {
            if (line.contains("=")) {
                String[] s = line.split("=");
                if (s.length >= 2) {
                    record.put(s[0], s[1]);
                    logger.info("RecordUtil loadRecord " + s[0] + " , " + s[1]);
                }
            }
        }
    }

    public static void saveRecord(Map<String, String> newRecord) {
        logger.info("RecordUtil saveRecord ...");
        File outputLogFile = new File(recordFilePath);

        try {
            FileOutputStream fos = new FileOutputStream(outputLogFile, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);

            if (record != null && !record.isEmpty()) {
                for (Map.Entry<String, String> entry : record.entrySet()) {
                    bw.write(entry.getKey() + "=" + entry.getValue() + "\r\n");
                    logger.info("RecordUtil saveRecord " + entry.getKey() + " , " + entry.getValue());
                }
            }

            bw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            logger.error("RecordUtil saveRecord error = " + e.getMessage());
        }
    }
}
