# Fusion Query JDBC

A JDBC driver that connects any JDBC-compatible SQL client to **Oracle Fusion Cloud** through BI Publisher's REST/SOAP APIs. Run real SQL against Fusion Cloud's database from **Oracle SQL Developer**, **DBeaver**, **JetBrains DataGrip**, **IntelliJ Database Tools**, or any other JDBC client — no VPN, no admin SQL access required.

The driver tunnels SQL queries through a BI Publisher data model proxy that the driver auto-deploys to your Fusion environment on first connect.

---

## Download

Pre-built artifacts are in [`dist/`](dist/). Grab the release ZIP:

[**fusion-query-jdbc-1.0.0.zip**](dist/fusion-query-jdbc-1.0.0.zip) — contains:

| File                                                          | Use for                                                                          |
|---------------------------------------------------------------|----------------------------------------------------------------------------------|
| `fusion-query-jdbc-1.0.0.jar`                                 | Standalone JDBC driver (DBeaver, DataGrip, SQL Developer JDBC tab)               |
| `fusion-sqldev-installer-1.0.0-windows-bundle.zip` (~39 MB)   | Windows installer **with bundled JRE** — no Java required                        |
| `fusion-sqldev-installer-1.0.0.exe` (~750 KB)                 | Windows installer (lean — needs an existing Java 8+ JRE/JDK)                     |
| `fusion-sqldev-installer-1.0.0.jar` (~685 KB)                 | Cross-platform installer (`java -jar ...`, needs Java 8+ installed)              |
| `README.md`                                                   | Bundled quick-start                                                              |

Java requirement:
- *Lean* installers need an existing **Java 8+** runtime.
- The **windows-bundle.zip** ships its own JRE 8 — extract and double-click, nothing else to install.

---

## Quick start

Pick the workflow that matches your client.

| Client                                                  | Use                                                      |
|---------------------------------------------------------|----------------------------------------------------------|
| Oracle SQL Developer — first-class connection type      | [Path A](#path-a--oracle-sql-developer-native)           |
| Oracle SQL Developer — generic JDBC tab (MySQL-style)   | [Path B](#path-b--generic-jdbc-mysql-style)              |
| DBeaver Community / Enterprise                          | [DBeaver](#dbeaver)                                      |
| JetBrains DataGrip / IntelliJ Database Tools            | [DataGrip / IntelliJ](#jetbrains-datagrip--intellij)     |

---

## Path A — Oracle SQL Developer (native)

Adds **"Oracle Fusion Cloud (BIP)"** as a dedicated entry in SQL Developer's *Database Type* dropdown, with a custom panel (Hostname, Report Path, Timeout).

1. Download the installer for your OS:
   - **Windows (no Java installed)**: [`fusion-sqldev-installer-1.0.0-windows-bundle.zip`](dist/fusion-sqldev-installer-1.0.0-windows-bundle.zip) — extract anywhere, double-click `fusion-sqldev-installer-1.0.0.exe` inside. Bundles its own JRE.
   - **Windows (Java already installed)**: [`fusion-sqldev-installer-1.0.0.exe`](dist/fusion-sqldev-installer-1.0.0.exe) — double-click to run, uses the system JRE.
   - **macOS / Linux**: [`fusion-sqldev-installer-1.0.0.jar`](dist/fusion-sqldev-installer-1.0.0.jar) — `java -jar fusion-sqldev-installer-1.0.0.jar`
2. A small GUI opens. Click **Install**.

   Headless / CI mode: `java -jar fusion-sqldev-installer-1.0.0.jar --cli` (or use the EXE with the same flag on Windows)
   Uninstall: append `--uninstall`
3. **Fully quit** SQL Developer (Cmd+Q on macOS, File → Exit on Windows/Linux — not just close window) and reopen.
4. *File → New → Database Connection*. In **Database Type**, pick **Oracle Fusion Cloud (BIP)**.
5. Fill in:
   - **Username** / **Password**: your Fusion Cloud credentials.
   - **Hostname**: your Fusion host, e.g. `fa-xxxx-saasabcd.fa.ocs.oraclecloud.com` (no `https://`).
   - **Report Path** (optional): leave blank to auto-deploy.
   - **Timeout seconds** (optional): defaults to 120.
6. Click **Test** → **Connect**.

**What the installer does** (transparent, all reversible):

- Creates a canonical folder for the JARs: `%USERPROFILE%\Oracle\fusion-query-jdbc-1.0.0\` on Windows, `~/Oracle/fusion-query-jdbc-1.0.0/` on macOS/Linux. Both the driver and extension JARs land here — useful as a stable path for DBeaver / DataGrip / IntelliJ.
- Copies the extension JAR to `<userdir>/user_extensions/` (where SQL Developer scans for extensions).
- Adds a marked block in `<userdir>/<version>/product.conf` so SQL Developer loads the extension at boot.
- Registers the driver JAR (from the canonical folder above) in SQL Developer's *Third Party JDBC Drivers* list automatically — no need to do *Tools → Preferences → Database → Third Party JDBC Drivers → Add Entry* manually.
- Clears SQL Developer's module cache so the new bundle is picked up.

The uninstall flag reverses every step.

SQL Developer user directory:

| OS      | Path                                                  |
|---------|-------------------------------------------------------|
| macOS   | `~/.sqldeveloper`                                     |
| Linux   | `~/.sqldeveloper`                                     |
| Windows | `%APPDATA%\sqldeveloper` (modern) or `%APPDATA%\SQL Developer` (older versions) |

---

## Path B — Generic JDBC (MySQL-style)

The same workflow you'd use with MySQL Connector/J: register the driver JAR, then create a connection through the generic JDBC tab. **No extension installed, nothing modified on SQL Developer itself.**

1. Download [`fusion-query-jdbc-1.0.0.jar`](dist/fusion-query-jdbc-1.0.0.jar).
2. Open SQL Developer.
3. *Tools → Preferences → Database → Third Party JDBC Drivers → Add Entry…* and select the JAR.
4. Click **OK** to save.
5. Click **New Connection** (green plus). In the connection dialog, choose the **JDBC** tab.
6. Fill in:
   - **Username** / **Password**: your Fusion Cloud credentials.
   - **JDBC URL**: `jdbc:fusion://fa-xxxx-saasabcd.fa.ocs.oraclecloud.com`
     Optional URL parameters:
     `jdbc:fusion://host?reportPath=/Custom/FusionQuery/Proxy/v1/csv.xdo&timeout=120`
7. **Test** → **Connect**.

---

## DBeaver

1. Download [`fusion-query-jdbc-1.0.0.jar`](dist/fusion-query-jdbc-1.0.0.jar).
2. Open DBeaver.
3. *Database → Driver Manager → New*.
4. Fill the driver definition:
   - **Driver Name**: `Oracle Fusion Cloud (BIP)`
   - **Driver Type**: `Generic`
   - **Class Name**: `com.fusionquery.jdbc.FusionDriver`
   - **URL Template**: `jdbc:fusion://{host}`
   - **Default Port**: leave blank
   - **Default Database**: leave blank
5. On the **Libraries** tab, click **Add File** and select the `fusion-query-jdbc-1.0.0.jar`.
6. Click **OK** to save the driver.
7. *Database → New Database Connection → select "Oracle Fusion Cloud (BIP)"*.
8. Fill in:
   - **Host**: your Fusion host, e.g. `fa-xxxx-saasabcd.fa.ocs.oraclecloud.com`
   - **User** / **Password**: your Fusion credentials.
   - Optional: switch to the **Driver properties** tab and add `reportPath` or `timeout`.
9. **Test Connection** → **Finish**.

---

## JetBrains DataGrip / IntelliJ

1. Download [`fusion-query-jdbc-1.0.0.jar`](dist/fusion-query-jdbc-1.0.0.jar).
2. Open DataGrip (or IntelliJ's Database tool window).
3. Click the wrench icon → **DataSource → Driver**.
4. Click **+** to add a new driver:
   - **Name**: `Oracle Fusion Cloud (BIP)`
   - **Driver Files**: click **+** and add the JAR.
   - **Class**: `com.fusionquery.jdbc.FusionDriver`
   - **URL templates**: add `jdbc:fusion://{host}`
5. **OK** to save.
6. **+ → Data Source → Oracle Fusion Cloud (BIP)**.
7. Fill in Host, User, Password. Optional `reportPath` / `timeout` go under **Advanced**.
8. **Test Connection** → **OK**.

---

## URL syntax (all clients)

```
jdbc:fusion://<host>
jdbc:fusion://<host>?<param>=<value>&...
jdbc:fusion://<user>:<password>@<host>          # credentials inline (URL-encoded)
```

| Property      | Required | Default                          | Notes                                            |
|---------------|----------|----------------------------------|--------------------------------------------------|
| `user`        | yes      | —                                | Fusion Cloud username                            |
| `password`    | yes      | —                                | Fusion Cloud password                            |
| `reportPath`  | no       | auto-deployed on first connect   | BI Publisher proxy report path                   |
| `timeout`     | no       | `120`                            | HTTP timeout (seconds)                           |

On Test Connection or a normal JDBC connection with no `reportPath` (or a blank value), the driver creates missing folders and objects under `/~<user>/FusionQuery/v1/`: `dm.xdm` and `csv.xdo`. It uses the documented SOAP v2 CatalogService on all Fusion hosts, uploads the model before the report, and fixes the report's model reference and archive metadata for the current user. Existing objects are reused; an interrupted installation is completed without overwriting existing objects. There is no automatic fallback to `/Custom/FusionQuery/Proxy/v1/csv.xdo`.

Before returning a connection, the driver executes `SELECT 1 AS FUSION_QUERY_CHECK FROM DUAL` through the report over SOAP and checks the result. Catalog or execution failures are raised during connection setup with SQLState `08001`, including the path and the server's reason. The user must have permission to create Publisher folders, models and reports in My Folders and use the configured data source (`ApplicationDB_FSCM` in the bundled model).

An explicit nonblank `reportPath` selects an existing proxy report. It is validated without creating or replacing catalog objects. Clear any old `reportPath` value in DBeaver to enable automatic setup.

For DBeaver, keep **Use legacy JDBC instantiation** enabled with class `com.fusionquery.jdbc.FusionDriver`. When replacing the JAR, remove the old library entry, add the new JAR and reconnect (restart DBeaver if its driver classloader still holds the old version).

The automatic setup tests use a local simulated Publisher server; they do not require Oracle credentials. Run `mvn -f fusion-query-jdbc/pom.xml test`. Tests cover initial setup, reconnect, incomplete installations, upload object types, patched archives, explicit paths, SOAP faults, missing objects after upload and report execution validation.

API reference: [Oracle CatalogService](https://docs.oracle.com/middleware/12212/bip/BIPDV/catalogservice.htm).

---

## SQL examples

Once connected, the driver translates plain SQL into BI Publisher data model queries against Fusion's database. Standard SELECT against Fusion views works out of the box:

```sql
-- Inspect available views
SELECT view_name FROM all_views WHERE owner = 'FUSION' ORDER BY view_name;

-- Query an HCM view
SELECT person_number, full_name FROM per_all_people_f WHERE ROWNUM <= 10;

-- Catalog query helper
SELECT column_name, data_type FROM all_tab_columns WHERE table_name = 'PER_ALL_PEOPLE_F';
```

The driver supports `SELECT` and BI Publisher catalog queries. DML/DDL is intentionally not supported — Fusion Cloud's BIP layer is read-only.

---

## Errors

Oracle errors are translated to friendlier messages:

| Oracle code | Meaning                       | Driver hint                                                   |
|-------------|-------------------------------|---------------------------------------------------------------|
| `ORA-00904` | invalid identifier            | suggests `all_tab_columns` catalog query                      |
| `ORA-00942` | table/view not found          | suggests `all_views` catalog query                            |
| `ORA-01017` | invalid username/password     | clear authentication message                                  |
| `ORA-01722` | invalid number                | translated                                                    |
| `ORA-00907` | missing right parenthesis     | translated                                                    |
| `ORA-00933` | SQL command not properly ended| translated                                                    |
| `ORA-00936` | missing expression            | translated                                                    |

---

## Troubleshooting

- **Path A: dropdown doesn't show "Oracle Fusion Cloud (BIP)"**
  Make sure SQL Developer was *fully* quit (Cmd+Q on macOS, not just close window) before reopening. The installer clears the module cache; the first boot after install takes ~10s longer than usual.

- **Path A: "creator is null" NPE on Test or Connect**
  Indicates the extension didn't load. Re-run the installer, ensure SQL Developer was *fully* quit, and check that `<userdir>/user_extensions/fusion-sqldev-extension-1.0.0.jar` exists.

- **Path B / DBeaver / DataGrip: driver class not found**
  Confirm the driver class name is **exactly** `com.fusionquery.jdbc.FusionDriver` and that the JAR was added to the driver's library/classpath.

- **`reportPath` shown as unknown**
  Leave the field blank on first connect — the driver auto-deploys the proxy report to your personal folder under BI Publisher.

- **OCI Cloud Service instance hangs on REST**
  The driver auto-detects OCS hostnames (anything with `.ocs.`) and falls back to SOAP. If you see a hang on an `.ocs.` host, file an issue with the host pattern.

- **DBeaver: empty result on first query**
  DBeaver caches the driver classloader. After updating the JAR, *Edit Driver → Libraries → remove old JAR → add new JAR → OK*, then re-test.

---

## Build from source

Requirements:

- JDK 17+ (needed to read the SQL Developer 24.3.1 APIs while building the extension; the driver and installer target Java 8 bytecode for maximum runtime compatibility)
- Maven 3.6+
- Oracle SQL Developer 24.3.1 installed locally (the build pulls JARs from the SQL Developer installation for compile-time `provided` dependencies)

```bash
# Build everything (driver + extension + installer + dist ZIP)
./build-dist.sh
```

Per-module builds:

```bash
( cd fusion-query-jdbc        && mvn install )   # produces target/fusion-query-jdbc-1.0.0.jar
( cd fusion-sqldev-extension  && mvn package )   # produces target/fusion-sqldev-extension-1.0.0.jar (embeds driver)
( cd fusion-sqldev-installer  && mvn package )   # produces target/fusion-sqldev-installer-1.0.0.jar (embeds extension)
```

---

## Project layout

```
.
├── fusion-query-jdbc/          # The JDBC driver (Type 3, talks to BI Publisher)
├── fusion-sqldev-extension/    # SQL Developer OSGi extension (registers the native Database Type)
├── fusion-sqldev-installer/    # Cross-platform Swing/CLI installer for Path A
├── dist/                       # Pre-built release artifacts (JARs + ZIP + README)
└── build-dist.sh               # One-shot build script
```

### How the SQL Developer extension works

- `FusionAddin` extends `AbstractThirdPartyAddin`, registers `subtype = "fusionCloud"` and a `FusionConnectionCreator` in `DatabaseProvider.s_creators`.
- `fusion.xml` (a `sqldev-navigator-hook` descriptor) makes `"Oracle Fusion Cloud (BIP)"` appear in the Database Type dropdown by registering a navigator entry with `connectionPanelClass` + `connectionTabName`.
- `FusionConnectionPanel` (extends `IConnectionPanel`) renders the custom fields (Hostname / Report Path / Timeout) and writes `subtype`, `RaptorConnectionType`, `driver`, and the field values into the saved properties.
- `FusionConnectionCreator` (extends `AbstractConnectionCreator`) builds the `jdbc:fusion://...` URL from the saved properties and returns the driver class.
- The extension's MANIFEST `Require-Bundle` lists `oracle.sqldeveloper.utils-nodeps` (which exports the `oracle.dbtools.connections.db` package containing `DatabaseProvider` and `AbstractConnectionCreator`), plus the standard `oracle.ide`, `oracle.javatools`, etc.

### How the installer works

- Detects the SQL Developer user directory per OS (`~/.sqldeveloper` on macOS/Linux, `%APPDATA%\SQL Developer` on Windows).
- Detects installed SQL Developer versions inside the user dir.
- Copies the extension JAR (which already embeds the driver) to `<userdir>/user_extensions/`.
- Edits `<userdir>/<version>/product.conf` to set `ide.bundle.search.path` and `ide.extension.search.path` JVM options pointing at `user_extensions/`, inside a managed `# >>> Fusion Query JDBC extension (managed) ... # <<<` block (so uninstall is exact).
- Clears the module cache directory so the new bundle is picked up on the next boot.

---

## License

MIT — see [LICENSE](LICENSE).
