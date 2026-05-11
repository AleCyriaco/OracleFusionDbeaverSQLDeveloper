# Fusion Query JDBC — SQL Developer Installer

Cross-platform Java installer that registers the **Fusion Query JDBC driver** as a native connection type in Oracle SQL Developer. After installation, "Oracle Fusion Cloud (BIP)" appears in the **Database Type** dropdown of the New Database Connection dialog.

## Requirements

- Java 11+ (any JDK/JRE)
- Oracle SQL Developer 24.x (other versions may work but are not tested)

## What it does

1. Detects your OS (macOS / Linux / Windows) and SQL Developer user directory
2. Copies the bundled extension JAR (with the embedded JDBC driver) to `<userdir>/user_extensions/`
3. Adds two VM options to each detected `product.conf`:
   - `ide.bundle.search.path` — so OSGi finds the bundle
   - `ide.extension.search.path` — so the extension framework processes its hooks
4. Clears the SQL Developer module cache to force re-scan on next launch

The managed block in `product.conf` is delimited by markers, so re-running the installer or running `--uninstall` cleanly applies/reverts changes.

## Install

```bash
# GUI (default)
java -jar fusion-sqldev-installer-1.0.0.jar

# Headless CLI
java -jar fusion-sqldev-installer-1.0.0.jar --cli

# Remove
java -jar fusion-sqldev-installer-1.0.0.jar --cli --uninstall
```

If SQL Developer has never been launched on the machine, run it once so it creates `~/.sqldeveloper/<version>/product.conf`, then re-run the installer.

After installation, **fully quit SQL Developer** (Cmd+Q on macOS, File → Exit elsewhere) and reopen it. Open *New Database Connection* and pick **Oracle Fusion Cloud (BIP)** in the Database Type dropdown.

## Per-OS user directories

| OS      | User directory                    |
|---------|-----------------------------------|
| macOS   | `~/.sqldeveloper`                 |
| Linux   | `~/.sqldeveloper`                 |
| Windows | `%APPDATA%\SQL Developer` (falls back to `%USERPROFILE%\.sqldeveloper`) |

## Connection fields

- **Hostname**: e.g. `fa-xxxx-saasfaprod1.fa.ocs.oraclecloud.com`
- **Username / Password**: your Fusion Cloud user
- **Report Path** (optional): leave blank to auto-deploy. Example: `/Custom/FusionQuery/Proxy/v1/csv.xdo`
- **Timeout** (optional): default 120s

## Build from source

```bash
# 1) Build the JDBC driver
cd fusion-query-jdbc && mvn install

# 2) Build the SQL Developer extension (depends on the driver)
cd ../fusion-sqldev-extension && mvn package

# 3) Build the installer (bundles the extension JAR)
cd ../fusion-sqldev-installer && mvn package

# Produces target/fusion-sqldev-installer-1.0.0.jar (~350 KB, fully self-contained)
```
