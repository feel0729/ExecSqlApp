package com.st1.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExecSqlScript implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private List<Map<String, String>> dbProperties;

    private List<String> envList;

    private Map<String, String> sqlScriptFiles;

    private Statement statement;

    private String appStartDate;
    private String appStartTime;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private final DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ExecSqlScript(List<Map<String, String>> properties, List<String> selectedEnv, Map<String, String> selectedFiles) {
        dbProperties = properties;
        envList = selectedEnv;
        sqlScriptFiles = selectedFiles;
        statement = null;
    }

    private void init() {
        logger.trace("init ...");
        Date appStart = new Date();
        appStartDate = dateFormat.format(appStart);
        appStartTime = timeFormat.format(appStart);

        logger.trace("appStartTime = " + appStartTime);

        if (dbProperties == null || dbProperties.isEmpty()) {
            logger.error("dbProperties is null.");
            return;
        }
        if (envList == null || envList.isEmpty()) {
            logger.error("envList is null.");
            return;
        }
        if (sqlScriptFiles == null || sqlScriptFiles.isEmpty()) {
            logger.error("sqlScriptFiles is null.");
            return;
        }
    }

    @Override
    public void run() {
        logger.trace("run ...");
        init();
        boolean isOK = true;
        for (Map.Entry<String, String> entry : sqlScriptFiles.entrySet()) {
            String fileName = entry.getKey();
            String filePath = entry.getValue();
            logger.trace("fileName = " + fileName);
            boolean isTfUsp = false;
            if (fileName.toUpperCase().startsWith("USP_TF_")) {
                isTfUsp = true;
            }
            List<String> sqlList = null;
            try {
                sqlList = getSqlScript(filePath);
            } catch (IOException e) {
                logger.error("getSqlScript error = " + e.getMessage());
            }
            if (sqlList == null || sqlList.isEmpty()) {
                logger.trace("sqlList is null. fileName = " + fileName);
            } else {
                isOK = execSql(isTfUsp, fileName, sqlList);
                if (!isOK) {
                    continue;
                }
            }
            logger.trace("fileName {} ok", fileName);
        }
        JOptionPane.showMessageDialog(
                null,
                "執行完畢",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);
        ExecSqlApp.btnExec.setEnabled(true);
    }

    private boolean execSql(boolean isTfUsp, String fileName, List<String> sqlList) {
        boolean isOK = true;
        for (String targetEnv : envList) {
            if (isTfUsp && !targetEnv.toUpperCase().startsWith("TF")) {
                continue;
            }
            Connection tmpConnection = null;
            for (Map<String, String> property : dbProperties) {
                if (property.get("name").equals(targetEnv)) {
                    DbConnect.settingDbProperty(property);
                    tmpConnection = DbConnect.getConnection();
                    break;
                }
            }
            if (tmpConnection == null) {
                logger.error("tmpConnection is null.");
                continue;
            }
            try {
                tmpConnection.setAutoCommit(false);
            } catch (SQLException e) {
                logger.error("tmpConnection setAutoCommit error = " + e.getMessage());
            }
            String startTime = timeFormat.format(new Date());
            try {
                statement = tmpConnection.createStatement();
                statement.setEscapeProcessing(false);
            } catch (SQLException e) {
                logger.error("createStatement error = " + e.getMessage());
            }
            String endTime = timeFormat.format(new Date());
            logger.info("execSql targetEnv = " + targetEnv);
            int cnt = 0;
            logger.info("executeLargeUpdate sqlList size = {} ", sqlList.size());
            for (String sql : sqlList) {
                try {
                    statement.executeLargeUpdate(sql);
                    cnt++;
                } catch (SQLException e) {
                    logger.error("error = " + e.getMessage());
                    logger.error("error sql = " + sql);
                    isOK = false;
                }
                if (cnt % 100 == 0 || cnt == sqlList.size()) {
                    logger.info("cnt = {}", cnt);
                    try {
                        doCommit(tmpConnection);
                    } catch (SQLException e) {
                        logger.error("doCommit error = " + e.getMessage());
                    }
                }
            }
            logger.info("executeLargeUpdate sqlList ok");
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.error("statment close error = " + e.getMessage());
                }
            }
            try {
                tmpConnection.close();
            } catch (SQLException e) {
                logger.error("tmpConnection close error = " + e.getMessage());
            }
        }
        return isOK;
    }

    private void doCommit(Connection tmpConnection) throws SQLException {
        tmpConnection.commit();
        logger.info("doCommit ok ");
    }

    private List<String> getSqlScript(String filePath) throws IOException {
        logger.trace("getSqlScript filePath = " + filePath);
        File sqlFile = new File(filePath);

        String fileName = sqlFile.getName();
        logger.info("fileName = " + fileName);

        String fileDate = dateFormat.format(new Date(sqlFile.lastModified()));
        logger.info("fileDate = " + fileDate);

        FileInputStream fis = new FileInputStream(sqlFile);
        InputStreamReader isw = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isw);
        List<String> everyLineInFile = new ArrayList<>();

        boolean isDDL = false;
        while (br.ready()) {
            String thisLine = br.readLine() + " \n";
            if (isDDL) {
            } else if (thisLine.toUpperCase().contains("PROCEDURE") || thisLine.toUpperCase().contains("FUNCTION")) {
                isDDL = true;
            }
            everyLineInFile.add(thisLine);
        }
        br.close();

        if (everyLineInFile == null || everyLineInFile.isEmpty()) {
            return everyLineInFile;
        }
        logger.trace("getSqlScript isDDL = " + isDDL);
        if (isDDL) {
            return getSqlScriptIsDDL(everyLineInFile);
        }
        return getSqlScriptNotDDL(everyLineInFile);
    }

    private List<String> getSqlScriptIsDDL(List<String> everyLineInFile) {
        List<String> result = new ArrayList<>();
        String line = "";
        boolean isSkip = true;
        for (String tmpLine : everyLineInFile) {
            if (isSkip) {
                if (tmpLine.trim().startsWith("--")) {
                    logger.info("skip this line : " + tmpLine);
                    continue;
                }
                if (tmpLine.toUpperCase().contains("DROP") && (tmpLine.toUpperCase().contains("PROCEDURE") || tmpLine.toUpperCase().contains("FUNCTION"))) {
                    // DROP ... PROCEDURE
                    // DROP ... FUNCTION
                    result.add(tmpLine);
                } else if (tmpLine.toUpperCase().contains("CREATE") && (tmpLine.toUpperCase().contains("PROCEDURE") || tmpLine.toUpperCase().contains("FUNCTION"))) {
                    // CREATE ... PROCEDURE
                    // CREATE ... FUNCTION
                    line += tmpLine;
                    line += "-- create by ExecSqlApp at " + appStartTime + " \n";
                    isSkip = false;
                } else {
                    logger.info("skip this line : " + tmpLine);
                }
            } else {
                line += tmpLine;
            }
        }
        line = line.substring(0, line.lastIndexOf(";") + 1);
        result.add(line);
        return result;
    }

    private List<String> getSqlScriptNotDDL(List<String> everyLineInFile) {
        List<String> result = new ArrayList<>();

        StringBuilder tmpLine = new StringBuilder();
        boolean isInString = false; // 用來標記是否處於字符串中

        for (String thisLine : everyLineInFile) {
            for (char ch : thisLine.toCharArray()) {
                if (ch == '\'') { // 切換 isInString 狀態
                    isInString = !isInString;
                }
                if (ch == ';' && !isInString) { // 只有當不在字符串中遇到分號時，才考慮結束語句
                    result.add(tmpLine.toString());
                    tmpLine = new StringBuilder();
                    continue;
                }
                tmpLine.append(ch);
            }
        }

        if (tmpLine.length() > 0) {
            result.add(tmpLine.toString());
        }
        return result;
    }
}
