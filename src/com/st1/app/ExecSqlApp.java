package com.st1.app;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExecSqlApp {
  private static final Logger logger = LogManager.getLogger();

  public static JFrame frmExecsqlapp;
  private JTextField textfieldDir;

  private String dir;

  private JList<String> fileList;
  private JList<String> envList;

  private Map<String, String> allFiles;
  private String allFileNames;
  private Map<String, String> selectedFiles;
  private List<String> selectedEnv;

  private List<Map<String, String>> properties;

  private ExecutorService fixedThreadPool;

  private ExecSqlScript execSqlScript;

  public static JButton btnExec;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        logger.trace("ExecSqlApp main run ...");
        try {
          new ExecSqlApp();
          ExecSqlApp.frmExecsqlapp.setVisible(true);
        } catch (Exception e) {
          logger.error("ExecSqlApp error = " + e.getMessage());
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public ExecSqlApp() {
    init();
    initialize();
    doSearch();
    setEnvList();
  }

  private void init() {
    logger.trace("ExecSqlApp init ...");
    allFiles = new HashMap<>();
    dir = "";
    properties = PropertyUtil.getProperties();
    dir = PropertyUtil.getDir();
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    logger.info("ExecSqlApp initialize ...");
    frmExecsqlapp = new JFrame();
    frmExecsqlapp.setTitle("ExecSqlApp");
    frmExecsqlapp.setBounds(100, 100, 600, 400);
    frmExecsqlapp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frmExecsqlapp.getContentPane().setLayout(null);

    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setBounds(14, 51, 360, 289);
    frmExecsqlapp.getContentPane().add(scrollPane);

    fileList = new JList<>();
    fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    fileList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        logger.trace("ExecSqlApp fileList valueChanged ...");
        selectedFiles = new HashMap<>();

        // 獲取所有被選中的選項索引
        int[] indices = fileList.getSelectedIndices();
        // 獲取選項資料的 ListModel
        ListModel<String> listModel = fileList.getModel();
        // 輸出選中的選項
        for (int index : indices) {
          String fileName = listModel.getElementAt(index);

          String filePath = allFiles.get(fileName);

          selectedFiles.put(fileName, filePath);
        }
      }
    });
    scrollPane.setViewportView(fileList);

    textfieldDir = new JTextField();
    textfieldDir.setBounds(14, 13, 240, 25);
    textfieldDir.setText(dir);
    frmExecsqlapp.getContentPane().add(textfieldDir);
    textfieldDir.setColumns(10);

    JButton btnChgDir = new JButton("\u8B8A\u66F4\u76EE\u9304");
    btnChgDir.setBounds(275, 13, 100, 25);
    btnChgDir.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        logger.trace("ExecSqlApp btnChgDir actionPerformed ...");
        // 宣告fileChooser
        JFileChooser fileChooser = new JFileChooser();

        // 預設檔案
        File tmpFile = new File(dir);

        // 設定fileChooser為只選擇檔案
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // 設定fileChooser指向預設檔案
        fileChooser.setSelectedFile(tmpFile);

        // 叫出fileChooser
        int returnValue = fileChooser.showOpenDialog(frmExecsqlapp);

        // 判斷是否選擇檔案
        if (returnValue == JFileChooser.APPROVE_OPTION) {

          // 指派給File
          File selectedFile = fileChooser.getSelectedFile();

          dir = selectedFile.getAbsolutePath();

          textfieldDir.setText(dir);

          doSearch();
        }
      }
    });
    frmExecsqlapp.getContentPane().add(btnChgDir);

    JLabel lableTargetEnv = new JLabel("\u76EE\u6A19\u74B0\u5883");
    lableTargetEnv.setHorizontalAlignment(SwingConstants.CENTER);
    lableTargetEnv.setBounds(389, 16, 179, 19);
    frmExecsqlapp.getContentPane().add(lableTargetEnv);

    JScrollPane scrollPaneTargetEnv = new JScrollPane();
    scrollPaneTargetEnv.setBounds(388, 51, 180, 245);
    frmExecsqlapp.getContentPane().add(scrollPaneTargetEnv);

    envList = new JList<>();
    envList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    envList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        logger.trace("ExecSqlApp envList valueChanged ...");
        selectedEnv = new ArrayList<>();

        // 獲取所有被選中的選項索引
        int[] indices = envList.getSelectedIndices();
        // 獲取選項資料的 ListModel
        ListModel<String> listModel = envList.getModel();
        // 輸出選中的選項
        for (int index : indices) {
          String envName = listModel.getElementAt(index);

          selectedEnv.add(envName);
        }
      }
    });
    scrollPaneTargetEnv.setViewportView(envList);

    btnExec = new JButton("\u57F7\u884C");
    btnExec.setBounds(388, 313, 180, 27);
    btnExec.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        logger.trace("ExecSqlApp btnExec actionPerformed ...");
        // 執行Sql到已選的各個環境

        // 檢查1:是否有已選擇的sql檔案
        if (selectedFiles == null || selectedFiles.isEmpty()) {
          // 沒有已選擇的sql檔案
          // 提示錯誤並結束
          JOptionPane.showMessageDialog(null, "沒有已選擇的sql檔案", "提示", JOptionPane.INFORMATION_MESSAGE);
          return;
        }

        // 檢查2:是否有已選擇的目標環境
        if (selectedEnv == null || selectedEnv.isEmpty()) {
          // 沒有已選擇的目標環境
          // 提示錯誤並結束
          JOptionPane.showMessageDialog(null, "沒有已選擇的目標環境", "提示", JOptionPane.INFORMATION_MESSAGE);
          return;
        }

        btnExec.setEnabled(false);

        // 執行
        doExec();
      }
    });
    frmExecsqlapp.getContentPane().add(btnExec);
  }

  private void doSearch() {
    logger.info("ExecSqlApp doSearch ...");

    SearchUtil.listDir(dir);

    allFiles = SearchUtil.getAllFiles();

    allFileNames = SearchUtil.getAllFileNames();

    if (!allFiles.isEmpty()) {
      fileList.setListData(allFileNames.split(","));
    }
  }

  private void setEnvList() {
    logger.info("ExecSqlApp setEnvList ...");
    String allEnvNames = "";
    if (properties != null && !properties.isEmpty()) {
      for (Map<String, String> property : properties) {
        String envName = property.get("name");
        allEnvNames += envName + ",";
      }
      if (!allEnvNames.isEmpty()) {
        envList.setListData(allEnvNames.split(","));
      }
    }
  }

  private void doExec() {
    logger.info("ExecSqlApp doExec ...");
    // 執行Sql到已選的各個環境

    // 實際執行Sql
    execSqlScript = new ExecSqlScript(properties, selectedEnv, selectedFiles);

    fixedThreadPool = Executors.newFixedThreadPool(1);

    fixedThreadPool.submit(execSqlScript);
  }
}
