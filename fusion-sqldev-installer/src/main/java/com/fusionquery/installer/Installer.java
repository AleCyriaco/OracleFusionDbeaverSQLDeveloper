package com.fusionquery.installer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;

public class Installer {

    public static void main(String[] args) {
        boolean cli = args.length > 0 && (args[0].equalsIgnoreCase("--cli") || args[0].equalsIgnoreCase("-c"));
        boolean uninstall = false;
        for (String a : args) {
            if (a.equalsIgnoreCase("--uninstall") || a.equalsIgnoreCase("-u")) uninstall = true;
        }

        Platform platform = Platform.detect();
        Path userDir = platform.userDir();
        List<SqlDevDetector.Detection> detections = SqlDevDetector.findVersions(userDir);

        if (cli || GraphicsEnvironment.isHeadless()) {
            runCli(platform, userDir, detections, uninstall);
        } else {
            SwingUtilities.invokeLater(() -> new InstallerGui(platform, userDir, detections).setVisible(true));
        }
    }

    private static void runCli(Platform platform, Path userDir,
                               List<SqlDevDetector.Detection> detections,
                               boolean uninstall) {
        InstallTask task = new InstallTask(platform, userDir, detections, System.out::println);
        try {
            if (uninstall) task.uninstall(); else task.install();
        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static class InstallerGui extends JFrame {
        private final Platform platform;
        private final Path userDir;
        private final List<SqlDevDetector.Detection> detections;
        private final JTextArea logArea = new JTextArea(16, 70);
        private final JButton installBtn = new JButton("Install");
        private final JButton uninstallBtn = new JButton("Uninstall");
        private final JButton closeBtn = new JButton("Close");

        InstallerGui(Platform platform, Path userDir, List<SqlDevDetector.Detection> detections) {
            super("Fusion Query JDBC Installer for SQL Developer");
            this.platform = platform;
            this.userDir = userDir;
            this.detections = detections;
            buildUi();
            populateSummary();
        }

        private void buildUi() {
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(8, 8));

            JPanel north = new JPanel(new BorderLayout(8, 8));
            north.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
            JLabel title = new JLabel("Fusion Query JDBC — install Oracle Fusion Cloud (BIP) connection in SQL Developer");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            north.add(title, BorderLayout.NORTH);

            logArea.setEditable(false);
            logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane scroll = new JScrollPane(logArea);
            scroll.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            add(north, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            south.add(uninstallBtn);
            south.add(installBtn);
            south.add(closeBtn);
            add(south, BorderLayout.SOUTH);

            installBtn.addActionListener(e -> runTask(false));
            uninstallBtn.addActionListener(e -> runTask(true));
            closeBtn.addActionListener(e -> dispose());

            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { dispose(); }
            });
            pack();
            setLocationRelativeTo(null);
        }

        private void populateSummary() {
            log("Platform: " + platform);
            log("User dir: " + userDir);
            if (detections.isEmpty()) {
                log("No SQL Developer installation detected (no version dirs in " + userDir + ").");
                log("Launch SQL Developer once to create its config, then re-run this installer.");
            } else {
                log("Detected SQL Developer versions:");
                for (SqlDevDetector.Detection d : detections) {
                    log("  - " + d + "   product.conf=" + d.productConf
                        + (d.systemCache() != null ? "   cache=" + d.systemCache() : ""));
                }
            }
            log("");
        }

        private void runTask(boolean uninstall) {
            installBtn.setEnabled(false);
            uninstallBtn.setEnabled(false);
            new SwingWorker<Void, String>() {
                @Override protected Void doInBackground() {
                    InstallTask task = new InstallTask(platform, userDir, detections, this::publish);
                    try { if (uninstall) task.uninstall(); else task.install(); }
                    catch (Exception ex) { publish("ERROR: " + ex.getMessage()); }
                    return null;
                }
                @Override protected void process(java.util.List<String> chunks) {
                    for (String c : chunks) log(c);
                }
                @Override protected void done() {
                    installBtn.setEnabled(true);
                    uninstallBtn.setEnabled(true);
                }
            }.execute();
        }

        private void log(String msg) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}
