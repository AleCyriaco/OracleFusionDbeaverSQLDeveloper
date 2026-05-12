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
        Path overrideInstallDir = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equalsIgnoreCase("--cli") || a.equalsIgnoreCase("-c")) cli = true;
            else if (a.equalsIgnoreCase("--uninstall") || a.equalsIgnoreCase("-u")) uninstall = true;
            else if (a.equalsIgnoreCase("--userdir") && i + 1 < args.length) overrideUserDir = Paths.get(args[++i]);
            else if (a.equalsIgnoreCase("--installdir") && i + 1 < args.length) overrideInstallDir = Paths.get(args[++i]);
            else if (a.equalsIgnoreCase("--help") || a.equalsIgnoreCase("-h")) { printHelp(); return; }
        }

        Platform platform = Platform.detect();
        Path userDir = overrideUserDir != null ? overrideUserDir : platform.userDir();
        List<SqlDevDetector.Detection> detections = SqlDevDetector.findVersions(userDir);
        Path installDir = overrideInstallDir != null ? overrideInstallDir : platform.findInstallDir();

        if (cli || GraphicsEnvironment.isHeadless()) {
            runCli(platform, userDir, detections, installDir, uninstall);
        } else {
            final Path initialUserDir = userDir;
            final Path initialInstallDir = installDir;
            final List<SqlDevDetector.Detection> initialDetections = detections;
            SwingUtilities.invokeLater(() ->
                new InstallerGui(platform, initialUserDir, initialDetections, initialInstallDir).setVisible(true));
        }
    }

    private static void printHelp() {
        System.out.println("Fusion Query JDBC Installer for Oracle SQL Developer");
        System.out.println();
        System.out.println("Usage: java -jar fusion-sqldev-installer-1.0.0.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --cli                Run in headless mode (no GUI).");
        System.out.println("  --uninstall          Reverse the installation. Use with --cli.");
        System.out.println("  --userdir <path>     Use a custom SQL Developer user directory.");
        System.out.println("                       The path should contain '<version>/product.conf'");
        System.out.println("                       and (after first launch) 'system<version>/'.");
        System.out.println("                       Defaults to %APPDATA%\\SQL Developer (Windows) or ~/.sqldeveloper.");
        System.out.println("  --installdir <path>  Path to the SQL Developer install root (the folder that");
        System.out.println("                       contains sqldeveloper.exe). The installer patches");
        System.out.println("                       <install>/sqldeveloper/bin/sqldeveloper.conf so the");
        System.out.println("                       extension is picked up on any launcher variant, including");
        System.out.println("                       portable installs. Auto-detected from common locations.");
        System.out.println("  --help               Show this message.");
    }

    private static void runCli(Platform platform, Path userDir,
                               List<SqlDevDetector.Detection> detections,
                               Path installDir,
                               boolean uninstall) {
        InstallTask task = new InstallTask(platform, userDir, detections, installDir, System.out::println);
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
        private Path installDir;
        private final JTextArea logArea = new JTextArea(16, 80);
        private final JLabel userDirLabel = new JLabel();
        private final JLabel installDirLabel = new JLabel();
        private final JButton browseUserBtn = new JButton("Choose user directory…");
        private final JButton browseInstallBtn = new JButton("Choose SQL Developer install folder…");
        private final JButton installBtn = new JButton("Install");
        private final JButton uninstallBtn = new JButton("Uninstall");
        private final JButton closeBtn = new JButton("Close");

        InstallerGui(Platform platform, Path userDir, List<SqlDevDetector.Detection> detections, Path installDir) {
            super("Fusion Query JDBC Installer for SQL Developer");
            this.platform = platform;
            this.userDir = userDir;
            this.detections = detections;
            this.installDir = installDir;
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

            JPanel pickRows = new JPanel();
            pickRows.setLayout(new BoxLayout(pickRows, BoxLayout.Y_AXIS));
            pickRows.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

            JPanel userRow = new JPanel(new BorderLayout(8, 0));
            userDirLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            userRow.add(userDirLabel, BorderLayout.CENTER);
            userRow.add(browseUserBtn, BorderLayout.EAST);
            pickRows.add(userRow);

            JPanel installRow = new JPanel(new BorderLayout(8, 0));
            installRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
            installDirLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            installRow.add(installDirLabel, BorderLayout.CENTER);
            installRow.add(browseInstallBtn, BorderLayout.EAST);
            pickRows.add(installRow);

            north.add(pickRows, BorderLayout.SOUTH);

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
            browseUserBtn.addActionListener(e -> chooseUserDir());
            browseInstallBtn.addActionListener(e -> chooseInstallDir());

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

        private void chooseInstallDir() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select SQL Developer install folder (contains sqldeveloper.exe)");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(installDir != null && Files.isDirectory(installDir)
                    ? installDir.toFile() : new File(System.getProperty("user.home")));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

            Path chosen = chooser.getSelectedFile().toPath();
            if (!Platform.looksLikeInstallDir(chosen)) {
                JOptionPane.showMessageDialog(this,
                    "That folder doesn't look like a SQL Developer install — expected to find\n"
                    + "sqldeveloper.exe / sqldeveloper.sh at the top, or\n"
                    + "sqldeveloper/bin/sqldeveloper.conf inside.",
                    "Folder not recognized", JOptionPane.WARNING_MESSAGE);
                return;
            }
            this.installDir = chosen;
            logArea.setText("");
            populateSummary();
        }

        private void populateSummary() {
            userDirLabel.setText("User dir:    " + userDir);
            installDirLabel.setText("Install dir: " + (installDir != null ? installDir : "(not detected — click Choose…)"));
            log("Platform: " + platform);
            log("User dir: " + userDir);
            log("Install dir: " + (installDir != null ? installDir : "(not detected)"));
            if (detections.isEmpty()) {
                log("No SQL Developer user-dir versions detected (no version dirs in " + userDir + ").");
                log("That's fine if you're on a portable install — the launcher conf patch is the");
                log("primary mechanism. Make sure Install dir above points at the right place.");
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
            browseUserBtn.setEnabled(false);
            browseInstallBtn.setEnabled(false);
            final Path udir = userDir;
            final List<SqlDevDetector.Detection> dets = detections;
            final Path idir = installDir;
            new SwingWorker<Void, String>() {
                @Override protected Void doInBackground() {
                    InstallTask task = new InstallTask(platform, udir, dets, idir, this::publish);
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
                    browseUserBtn.setEnabled(true);
                    browseInstallBtn.setEnabled(true);
                }
            }.execute();
        }

        private void log(String msg) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}
