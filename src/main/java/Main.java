import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Main {
    private JPanel jPanel;
    private JTextField pathTF;
    private JLabel pathLabel;
    private JTextArea logArea;
    private JButton chooseFileButton;
    private JTextField fromTF;
    private JTextField toTF;
    private JLabel timeLabel;
    private JButton startButton;
    private JTextField urlTF;

    private static String path;
    private static String url;
    private static long timeFrom;
    private static long timeTo;
    private static long timeInit;
    private volatile long time;
    private volatile long counter;
    private volatile boolean started = false;
    private static Properties property;
    private static JFrame jFrame;

    public Main() {
        FileInputStream fis;
        property = new Properties();
        try {
            fis = new FileInputStream("app.config");
            property.load(fis);
            path = property.getProperty("path");
            url = property.getProperty("url");
            timeFrom = Long.parseLong((String) property.get("timeFrom"));
            timeTo = Long.parseLong((String) property.get("timeTo"));
            timeInit = Long.parseLong((String) property.get("timeInit"));
            pathTF.setText(path);
            urlTF.setText(url);
            fromTF.setText(String.valueOf(timeFrom));
            toTF.setText(String.valueOf(timeTo));
        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }

        toTF.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!toTF.getText().equals("")) {
                    timeTo = Long.parseLong(toTF.getText());
                    property.setProperty("timeTo", String.valueOf(timeTo));
                }
            }
        });
        fromTF.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!fromTF.getText().equals("")) {
                    timeFrom = Long.parseLong(fromTF.getText());
                    property.setProperty("timeFrom", String.valueOf(timeFrom));
                }
            }
        });
        chooseFileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                jFrame.setAlwaysOnTop(false);
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                int returnValue = jfc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = jfc.getSelectedFile();
                    pathTF.setText(selectedFile.getAbsolutePath());
                    path = Paths.get(FileSystems.getDefault().getPath(pathTF.getText().trim()).toString()).toString();
                    property.setProperty("path", path);
                }
                jFrame.setAlwaysOnTop(true);
            }
        });
        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                property.setProperty("path", path);
                property.setProperty("url", url);
                property.setProperty("timeFrom", String.valueOf(timeFrom));
                property.setProperty("timeTo", String.valueOf(timeTo));
                try {
                    property.store(new FileOutputStream("app.config"), null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                if (!started && startButton.getText().equals("Остановка...")) {
                    log("Остановка цикла в процессе. Дождитесь окончания предыдущего запуска.");
                    return;
                } else if (started && startButton.getText().equals("Остановить")) {
                    started = false;
                    SwingUtilities.invokeLater(() -> {
                        startButton.setText("Остановка...");
                        log("Остановка цикла.");
                        jPanel.revalidate();
                    });
                    return;
                }

                if (path == null || path.equals("")) {
                    log("Путь до приложения не задан.");
                }
                if (path == null || path.equals("")) {
                    log("Адрес сайта не задан.");
                }

                started = true;
                startButton.setText("Остановить");
                jPanel.revalidate();

                new Thread(() -> {
                    log("Старт цикла посещений.");
                    counter = 0;
                    while (started) {
                        counter++;
                        time = (long) (((timeTo - timeFrom) * Math.random()) + timeFrom) * 1000;
                        log("Запуск " + counter + ". Планируемое время: инициализация - " + timeInit + " сек."
                                + ", посещение - " + time / 1000 + " сек.");
                        jPanel.revalidate();
                        logArea.revalidate();
                        try {
                            Process first = Runtime.getRuntime().exec(path + " -url " + "http://ya.ru");
                            Thread.sleep(timeInit * 1000);
                            Process second = Runtime.getRuntime().exec(path + " -url " + url);
                            Thread.sleep(time);
                            first.destroy();
                            second.destroy();
                            if (started) {
                                log("Конец итерации " + counter + ". Следующий запуск через 5 сек.");
                            }
                            Thread.sleep(5000);
                        } catch (InterruptedException | IOException ex) {
                            log("Произошла ошибка во время работы программы. Следующее сообщение содержит техническую информацию об ошибке:");
                            log(ex.getMessage());
                            ex.printStackTrace();
                            counter = 0;
                            started = false;
                            startButton.setText("Запустить");
                        }
                    }
                    startButton.setText("Запустить");
                    log("Цикл посещений остановлен.");
                    counter = 0;
                }).start();
            }
        });

        pathTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                path = Paths.get(FileSystems.getDefault().getPath(pathTF.getText().trim()).toString()).toString();
                property.setProperty("path", path);
            }
        });
        urlTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                url = urlTF.getText();
                property.setProperty("url", url);
            }
        });
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format(
                            "%s : %s \n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                            s
                    )
            );
            jPanel.revalidate();
        });
    }

    public static void main(String[] args) {
        jFrame = new JFrame("Main");
        jFrame.setContentPane(new Main().jPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    jFrame.requestFocus();
                    property.setProperty("path", path);
                    property.setProperty("url", url);
                    property.setProperty("timeFrom", String.valueOf(timeFrom));
                    property.setProperty("timeTo", String.valueOf(timeTo));
                    property.store(new FileOutputStream("app.config"), null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        jFrame.setAlwaysOnTop(true);
        jFrame.pack();
        jFrame.setMinimumSize(new Dimension(500, 600));
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        jFrame.setLocation((dim.width - jFrame.getSize().width) / 2,
                (dim.height - jFrame.getSize().height) / 2);
        jFrame.setVisible(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jPanel = new JPanel();
        jPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 15, new Insets(15, 15, 15, 15), 10, 10));
        jPanel.setMinimumSize(new Dimension(500, 200));
        pathLabel = new JLabel();
        pathLabel.setText("Путь до приложения:");
        jPanel.add(pathLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 15, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTHWEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFileButton = new JButton();
        chooseFileButton.setText("Выбрать");
        jPanel.add(chooseFileButton, new com.intellij.uiDesigner.core.GridConstraints(1, 11, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, 25), new Dimension(120, 25), new Dimension(120, 25), 1, false));
        timeLabel = new JLabel();
        timeLabel.setText("Время посещения(сек):");
        jPanel.add(timeLabel, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 10, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pathTF = new JTextField();
        jPanel.add(pathTF, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 11, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), new Dimension(-1, 28), new Dimension(-1, 28), 1, false));
        final JLabel label1 = new JLabel();
        label1.setText("Журнал работы:");
        jPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 10, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Запустить");
        jPanel.add(startButton, new com.intellij.uiDesigner.core.GridConstraints(6, 11, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, 35), new Dimension(120, 35), new Dimension(120, 35), 1, false));
        final JLabel label2 = new JLabel();
        label2.setText("URL назначения:");
        jPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 10, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlTF = new JTextField();
        urlTF.setForeground(new Color(-16777216));
        jPanel.add(urlTF, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 11, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 28), new Dimension(-1, 28), new Dimension(-1, 28), 1, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        jPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 12, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logArea = new JTextArea();
        logArea.setEditable(false);
        scrollPane1.setViewportView(logArea);
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        jPanel.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 9, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(280, 28), new Dimension(80, 28), new Dimension(80, 28), 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 0, 0));
        jPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(100, 25), null, null, 0, false));
        fromTF = new JTextField();
        panel1.add(fromTF, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, 28), new Dimension(80, 28), new Dimension(80, 28), 1, false));
        toTF = new JTextField();
        panel1.add(toTF, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, 28), new Dimension(80, 28), new Dimension(80, 28), 1, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jPanel;
    }

}
