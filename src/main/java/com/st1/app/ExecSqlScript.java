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
import java.util.*;
import java.util.Map.Entry;

public class ExecSqlScript implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    private List<Map<String, String>> dbProperties;

    private List<String> envList;

    private Map<String, String> sqlScriptFiles;

    private Statement statment;

    private String appStartDate;
    private String appStartTime;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private final DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private boolean userChoice = true;

    private List<Map<String, String>> execResultList;

    private Map<Map<String, String>, List<String>> errorData;
    private Map<String, String> errorKey;

    public ExecSqlScript(List<Map<String, String>> properties, List<String> selectedEnv, Map<String, String> selectedFiles) {
        dbProperties = properties;
        envList = selectedEnv;
        sqlScriptFiles = selectedFiles;
        statment = null;
    }

    private void init() {
        logger.trace("ExecSqlScript init ...");

        execResultList = new ArrayList<>();

        errorData = new HashMap<>();

        Date appStart = new Date();

        appStartDate = dateFormat.format(appStart);
        appStartTime = timeFormat.format(appStart);

        logger.trace("ExecSqlScript init appStartTime = " + appStartTime);

        if (dbProperties == null || dbProperties.isEmpty()) {
            logger.error("ExecSqlScript run dbProperties is null.");
            return;
        }
        if (envList == null || envList.isEmpty()) {
            logger.error("ExecSqlScript run envList is null.");
            return;
        }
        if (sqlScriptFiles == null || sqlScriptFiles.isEmpty()) {
            logger.error("ExecSqlScript run sqlScriptFiles is null.");
            return;
        }
    }

    @Override
    public void run() {
        logger.trace("ExecSqlScript run ...");

        init();
        boolean isOK = true;
        for (Map.Entry<String, String> entry : sqlScriptFiles.entrySet()) {
            userChoice = true;
            String fileName = entry.getKey();
            String filePath = entry.getValue();
            logger.trace("ExecSqlScript run fileName = " + fileName);
            boolean isTfUsp = false;
            if (fileName.toUpperCase().startsWith("USP_TF_")) {
                isTfUsp = true;
            }
            List<String> sqlList = null;
            try {
                sqlList = getSqlScript(filePath);
            } catch (IOException e) {
                logger.error("ExecSqlScript run getSqlScript error = " + e.getMessage());
            }
            if (sqlList == null || sqlList.isEmpty()) {
                logger.trace("ExecSqlScript run sqlList is null. fileName = " + fileName);
            } else {
                for (String sql : sqlList) {
                    isOK = execSql(isTfUsp, fileName, sql);
                    if (!isOK) {
                        break;
                    }
                }
            }
            if (!isOK) {
                break;
            }
        }

        if (execResultList != null && !execResultList.isEmpty()) {
            for (Map<String, String> execResult : execResultList) {
                logger.info("================================================");
                logger.info("fileName     = " + execResult.get("fileName"));
                logger.info("targetEnv    = " + execResult.get("targetEnv"));
                logger.info("isOK         = " + execResult.get("isOK"));
                logger.info("userChoice   = " + execResult.get("userChoice"));
                logger.info("appStartTime = " + execResult.get("appStartTime"));
                logger.info("startTime    = " + execResult.get("startTime"));
                logger.info("endTime      = " + execResult.get("endTime"));
            }
        }

        boolean isError = errorData != null && !errorData.isEmpty();

        JOptionPane.showMessageDialog(null, "執行完畢" + (isError ? ",但有錯誤" : ""), "提示",
                JOptionPane.INFORMATION_MESSAGE);

        if (isError) {
            String allErrorMessage = "";
            Map<String, String> tempErrorKey;
            List<String> errorMessageList;
            String tempTargetEnv;
            String tempFileName;
            int errorCount = 0;
            for (Entry<Map<String, String>, List<String>> entry : errorData.entrySet()) {
                tempErrorKey = entry.getKey();
                errorMessageList = entry.getValue();
                tempTargetEnv = tempErrorKey.get("targetEnv");
                tempFileName = tempErrorKey.get("fileName");
                allErrorMessage += "環境 : " + tempTargetEnv + "\r\n執行 : " + tempFileName + " 時，發生錯誤\r\n";
                for (String errorMessage : errorMessageList) {
                    if (errorCount >= 10) {
                        allErrorMessage += "錯誤太多,未全部顯示在此視窗,請至log查看完整錯誤說明";
                        break;
                    }
                    allErrorMessage += errorMessage + "\r\n";
                    errorCount++;
                }
                if (errorCount >= 10) {
                    break;
                }
            }
            logger.error(allErrorMessage);
            JOptionPane.showMessageDialog(null, allErrorMessage, "執行錯誤", JOptionPane.ERROR_MESSAGE);
        }

        ExecSqlApp.btnExec.setEnabled(true);
    }

    private boolean execSql(boolean isTfUsp, String fileName, String sql) {
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
                logger.error("ExecSqlScript execSql tmpConnection is null.");
                continue;
            }
            try {
                tmpConnection.setAutoCommit(false);
            } catch (SQLException e) {
                logger.error("ExecSqlScript execSql tmpConnection setAutoCommit error = " + e.getMessage());
            }
            String startTime = timeFormat.format(new Date());
            try {
                statment = tmpConnection.createStatement();
            } catch (SQLException e) {
                logger.error("ExecSqlScript execSql dbConnection createStatement error = " + e.getMessage());
            }
            String endTime = timeFormat.format(new Date());
            logger.info("ExecSqlScript execSql targetEnv = " + targetEnv);
            try {
                statment.executeLargeUpdate(sql);
            } catch (SQLException e) {
                logger.error("ExecSqlScript execSql statment executeLargeUpdate error = " + e.getMessage());
                logger.error("ExecSqlScript execSql error sql = \n" + sql);
                JOptionPane.showMessageDialog(null
                        , "執行錯誤("
                                + e.getMessage()
                                + "),sql:"
                                + sql
                        , "錯誤"
                        , JOptionPane.ERROR_MESSAGE
                );
                errorKey = new HashMap<>();
                errorKey.put("targetEnv", targetEnv);
                errorKey.put("fileName", fileName);
                List<String> errorList;
                if (errorData.containsKey(errorKey)) {
                    errorList = errorData.get(errorKey);
                } else {
                    errorList = new ArrayList<>();
                }
                errorList.add(e.getMessage());
                errorData.put(errorKey, errorList);
                isOK = false;
            }
            if (statment != null) {
                try {
                    statment.close();
                } catch (SQLException e) {
                    logger.error("ExecSqlScript execSql statment close error = " + e.getMessage());
                }
            }
            Map<String, String> execResult = new HashMap<>();
            execResult.put("fileName", fileName);
            execResult.put("targetEnv", targetEnv);
            execResult.put("isOK", isOK ? "OK" : "");
            execResult.put("userChoice", userChoice ? "" : "don't continue");
            execResult.put("appStartTime", appStartTime);
            execResult.put("startTime", startTime);
            execResult.put("endTime", endTime);
            execResultList.add(execResult);
            try {
                tmpConnection.commit();
            } catch (SQLException e) {
                logger.error("ExecSqlScript execSql tmpConnection commit error = " + e.getMessage());
            }
            try {
                tmpConnection.close();
            } catch (SQLException e) {
                logger.error("ExecSqlScript execSql tmpConnection close error = " + e.getMessage());
            }
            if (!isOK) {
                break;
            }
        }
        return isOK;
    }

    private List<String> getSqlScript(String filePath) throws IOException {
        logger.trace("ExecSqlScript getSqlScript filePath = " + filePath);
        File sqlFile = new File(filePath);

        String fileName = sqlFile.getName();
        logger.info("ExecSqlScript getSqlScript fileName = " + fileName);

        String fileDate = dateFormat.format(new Date(sqlFile.lastModified()));
        logger.info("ExecSqlScript getSqlScript fileDate = " + fileDate);

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
        logger.trace("ExecSqlScript getSqlScript isDDL = " + isDDL);
        if (isDDL) {
            return getSqlScriptIsDDL(everyLineInFile);
        }
        return getSqlScriptNotDDL(everyLineInFile, fileName, fileDate);
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

    private List<String> getSqlScriptNotDDL(List<String> everyLineInFile, String fileName, String fileDate) {
        List<String> result = new ArrayList<>();
        String tmpLine = "";
        for (String thisLine : everyLineInFile) {
            if (thisLine.contains(";")) {
                tmpLine += thisLine.substring(0, thisLine.indexOf(";"));
                result.add(tmpLine);
                tmpLine = "";
            } else {
                tmpLine += thisLine;
            }
        }
        if (tmpLine != null && !tmpLine.isEmpty()) {
            result.add(tmpLine);
        }
        return result;
    }
}
