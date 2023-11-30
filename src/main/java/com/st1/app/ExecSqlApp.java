package com.st1.app;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.*;
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
        setIcon();
    }

    private void setIcon() {
        // 標題圖示
        String iconPath = "/sql.png";
        InputStream inputStream = ExecSqlApp.class.getResourceAsStream(iconPath);
        if (inputStream != null) {
            try {
                ImageIcon img = new ImageIcon(ImageIO.read(inputStream));
                frmExecsqlapp.setIconImage(img.getImage());
            } catch (IOException e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                logger.error("Failed to load icon: " + iconPath + " , Exception = " + errors.toString());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    logger.error("inputStream.close Exception = " + errors.toString());
                }
            }
        } else {
            logger.error("Couldn't find file: " + iconPath);
        }
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
                int[] indices = fileList.getSelectedIndices();
                ListModel<String> listModel = fileList.getModel();
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
        doSearch();
        frmExecsqlapp.getContentPane().add(textfieldDir);
        textfieldDir.setColumns(10);

        JButton btnChgDir = new JButton("\u8B8A\u66F4\u76EE\u9304");
        btnChgDir.setBounds(275, 13, 100, 25);
        btnChgDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("ExecSqlApp btnChgDir actionPerformed ...");
                JFileChooser fileChooser = new JFileChooser();
                File tmpFile = new File(dir);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setSelectedFile(tmpFile);
                int returnValue = fileChooser.showOpenDialog(frmExecsqlapp);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
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
        lableTargetEnv.setBounds(389, 16, 113, 19);
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

                int[] indices = envList.getSelectedIndices();
                ListModel<String> listModel = envList.getModel();
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
                if (selectedFiles == null || selectedFiles.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "未選擇欲執行的sql", "錯誤", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (selectedEnv == null || selectedEnv.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "未選擇目標資料庫", "錯誤", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                btnExec.setEnabled(false);
                doExec();
            }
        });
        frmExecsqlapp.getContentPane().add(btnExec);

        JCheckBox checkboxSelectAll = new JCheckBox("\u5168\u9078");
        checkboxSelectAll.setBounds(502, 12, 66, 27);
        checkboxSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logger.trace("ExecSqlApp checkboxSelectAll actionPerformed ...");
                selectedEnv = new ArrayList<>();
                envList.clearSelection();
                if (checkboxSelectAll.isSelected()) {
                    ListModel<String> listModel = envList.getModel();
                    envList.setSelectionInterval(0, listModel.getSize() - 1);
                    for (int i = 0; i < listModel.getSize(); i++) {
                        String envName = listModel.getElementAt(i);
                        selectedEnv.add(envName);
                        System.out.println(envName);
                    }
                }
            }
        });
        frmExecsqlapp.getContentPane().add(checkboxSelectAll);
    }

    private void doSearch() {
        logger.info("ExecSqlApp doSearch ...");
        SearchUtil.listDir(dir);
        allFiles = SearchUtil.getAllFiles();
        allFileNames = SearchUtil.getAllFileNames();
        if (!allFiles.isEmpty()) {
            fileList.setListData(allFileNames.split(","));
        } else {
            fileList.setListData(new String[0]);
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
        execSqlScript = new ExecSqlScript(properties, selectedEnv, selectedFiles);
        fixedThreadPool = Executors.newFixedThreadPool(1);
        fixedThreadPool.submit(execSqlScript);
    }
}
