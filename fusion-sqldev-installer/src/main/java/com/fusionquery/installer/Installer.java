package com.fusionquery.installer;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Installer {

    public static void main(String[] args) {
        boolean cli = false;
        boolean uninstall = false;
        Path overrideUserDir = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equalsIgnoreCase("--cli") || a.equalsIgnoreCase("-c")) cli = true;
            else if (a.equalsIgnoreCase("--uninstall") || a.equalsIgnoreCase("-u")) uninstall = true;
            else if (a.equalsIgnoreCase("--userdir") && i + 1 < args.length) overrideUserDir = Paths.get(args[++i]);
            else if (a.equalsIgnoreCase("--help") || a.equalsIgnoreCase("-h")) { printHelp(); return; }
        }

        Platform platform = Platform.detect();
        Path userDir = overrideUserDir != null ? overrideUserDir : platform.userDir();
        List<SqlDevDetector.Detection> detections = SqlDevDetector.findVersions(userDir);

        if (cli || GraphicsEnvironment.isHeadless()) {
            runCli(platform, userDir, detections, uninstall);
        } else {
            final Path initialUserDir = userDir;
            final List<SqlDevDetector.Detection> initialDetections = detections;
            SwingUtilities.invokeLater(() ->
                new InstallerGui(platform, initialUserDir, initialDetections).setVisible(true));
        }
    }

    private static void printHelp() {
        System.out.println("Fusion Query JDBC Installer for Oracle SQL Developer");
        System.out.println();
        System.out.println("Usage: java -jar fusion-sqldev-installer-1.0.0.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --cli              Run in headless mode (no GUI).");
        System.out.println("  --uninstall        Reverse the installation. Use with --cli.");
        System.out.println("  --userdir <path>   Use a custom SQL Developer user directory.");
        System.out.println("                     The path should contain '<version>/product.conf'");
        System.out.println("                     and (after first launch) 'system<version>/'.");
        System.out.println("                     Defaults to %APPDATA%\\sqldeveloper (Windows) or ~/.sqldeveloper.");
        System.out.println("  --help             Show this message.");
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
        private Path userDir;
        private List<SqlDevDetector.Detection> detections;
        private final JTextArea logArea = new JTextArea(16, 80);
        private final JLabel userDirLabel = new JLabel();
        private final JButton browseBtn = new JButton("Choose user directory…");
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
            north.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            JLabel title = new JLabel("Fusion Query JDBC — install Oracle Fusion Cloud (BIP) connection in SQL Developer");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            north.add(title, BorderLayout.NORTH);

            JPanel pickRow = new JPanel(new BorderLayout(8, 0));
            pickRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            userDirLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            pickRow.add(userDirLabel, BorderLayout.CENTER);
            pickRow.add(browseBtn, BorderLayout.EAST);
            north.add(pickRow, BorderLayout.SOUTH);

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
            browseBtn.addActionListener(e -> chooseUserDir());

            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { dispose(); }
            });
            pack();
            setLocationRelativeTo(null);
        }

        private void chooseUserDir() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select SQL Developer user directory or install folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(Files.isDirectory(userDir) ? userDir.toFile()
                    : new File(System.getProperty("user.home")));

            int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;

            Path chosen = chooser.getSelectedFile().toPath();
            Path resolved = resolveUserDir(chosen);
            if (resolved == null) {
                JOptionPane.showMessageDialog(this,
                    "No SQL Developer version directories were found under:\n" + chosen
                    + "\n\nPick the folder that contains '<version>\\product.conf', e.g.:\n"
                    + "  %APPDATA%\\sqldeveloper\n"
                    + "  <sqldeveloper-install>\\sqldeveloper\\bin (auto-detected)\n"
                    + "  <portable-folder>\\.sqldeveloper",
                    "Folder not recognized",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            this.userDir = resolved;
            this.detections = SqlDevDetector.findVersions(resolved);
            logArea.setText("");
            populateSummary();
        }

        /**
         * If the user picked the SQL Developer user directory itself, use it directly.
         * Otherwise try common sibling locations relative to a SQL Developer install folder
         * (e.g. they picked C:\Users\X\Desktop\sqldeveloper but the user dir is
         * %APPDATA%\sqldeveloper, or a portable layout where it sits at <root>\.sqldeveloper).
         */
        private Path resolveUserDir(Path chosen) {
            if (containsVersionDir(chosen)) return chosen;
            Path[] candidates = {
                chosen.resolve(".sqldeveloper"),
                chosen.resolve("sqldeveloper"),
                chosen.getParent() != null ? chosen.getParent().resolve(".sqldeveloper") : null
            };
            for (Path c : candidates) {
                if (c != null && containsVersionDir(c)) return c;
            }
            return null;
        }

        private boolean containsVersionDir(Path dir) {
            return !SqlDevDetector.findVersions(dir).isEmpty();
        }

        private void populateSummary() {
            userDirLabel.setText("User dir: " + userDir);
            log("Platform: " + platform);
            log("User dir: " + userDir);
            if (detections.isEmpty()) {
                log("No SQL Developer installation detected (no version dirs in " + userDir + ").");
                log("If SQL Developer is installed in a custom location, click 'Choose user directory…'");
                log("and point at the folder that contains '<version>\\product.conf'.");
            } else {
                log("Detected SQL Developer versions:");
                for (SqlDevDetector.Detection d : detections) {
                    log("  - " + d + "   product.conf=" + d.productConf
                        + (d.systemCache() != null ? "   cache=" + d.systemCache() : "   (no system cache yet)"));
                }
            }
            log("");
        }

        private void runTask(boolean uninstall) {
            installBtn.setEnabled(false);
            uninstallBtn.setEnabled(false);
            browseBtn.setEnabled(false);
            final Path udir = userDir;
            final List<SqlDevDetector.Detection> dets = detections;
            new SwingWorker<Void, String>() {
                @Override protected Void doInBackground() {
                    InstallTask task = new InstallTask(platform, udir, dets, this::publish);
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
                    browseBtn.setEnabled(true);
                }
            }.execute();
        }

        private void log(String msg) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}
