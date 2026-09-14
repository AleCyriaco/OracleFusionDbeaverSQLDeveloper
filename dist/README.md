# Fusion Query JDBC — Release Package

A JDBC driver that connects any JDBC-compatible SQL client to **Oracle Fusion Cloud** via BI Publisher.

This package contains both install paths. Pick whichever fits your client.

| File                                  | Use for                                                              |
|---------------------------------------|----------------------------------------------------------------------|
| `fusion-query-jdbc-1.0.0.jar`         | Standalone driver (DBeaver, DataGrip, SQL Developer's JDBC tab)      |
| `fusion-sqldev-installer-1.0.0.exe`   | Windows installer — double-click to run                              |
| `fusion-sqldev-installer-1.0.0.jar`   | Cross-platform installer (`java -jar ...`)                           |

Requires **Java 8+**.

The installer drops both JARs into a canonical folder for cross-tool use:
- Windows: `%USERPROFILE%\Oracle\fusion-query-jdbc-1.0.0\`
- macOS / Linux: `~/Oracle/fusion-query-jdbc-1.0.0/`

---

## Path A — Oracle SQL Developer (native connection type)

Adds **"Oracle Fusion Cloud (BIP)"** as a dedicated entry in SQL Developer's *Database Type* dropdown.

1. Run the installer:
   ```bash
   java -jar fusion-sqldev-installer-1.0.0.jar
   ```
   GUI opens. Click **Install**.
   Headless: `java -jar fusion-sqldev-installer-1.0.0.jar --cli`
   Uninstall: `java -jar fusion-sqldev-installer-1.0.0.jar --cli --uninstall`
2. **Fully quit** SQL Developer (Cmd+Q / File → Exit) and reopen.
3. *File → New Database Connection*. In **Database Type**, pick **Oracle Fusion Cloud (BIP)**.
4. Fill in Username / Password / Hostname (e.g. `fa-xxxx-saasabcd.fa.ocs.oraclecloud.com`).

---

## Path B — Oracle SQL Developer (generic JDBC)

1. Open SQL Developer.
2. *Tools → Preferences → Database → Third Party JDBC Drivers → Add Entry…* and pick `fusion-query-jdbc-1.0.0.jar`.
3. **New Connection** → **JDBC** tab.
4. URL: `jdbc:fusion://fa-xxxx-saasabcd.fa.ocs.oraclecloud.com`
5. Username / Password → **Test** → **Connect**.

---

## DBeaver

1. *Database → Driver Manager → New*.
2. Driver Name: `Oracle Fusion Cloud (BIP)`, Class Name: `com.fusionquery.jdbc.FusionDriver`, URL Template: `jdbc:fusion://{host}`.
3. **Libraries** tab → **Add File** → pick `fusion-query-jdbc-1.0.0.jar` → **OK**.
4. *Database → New Database Connection → Oracle Fusion Cloud (BIP)*.
5. Host: `fa-xxxx-saasabcd.fa.ocs.oraclecloud.com`, User, Password → **Test Connection** → **Finish**.

---

## JetBrains DataGrip / IntelliJ

1. Wrench icon → **DataSource → Driver** → **+**.
2. Driver Files: add `fusion-query-jdbc-1.0.0.jar`. Class: `com.fusionquery.jdbc.FusionDriver`. URL template: `jdbc:fusion://{host}`.
3. **+ → Data Source → Oracle Fusion Cloud (BIP)**.
4. Host / User / Password → **Test Connection**.

---

## URL syntax

```
jdbc:fusion://<host>
jdbc:fusion://<host>?reportPath=...&timeout=120
jdbc:fusion://<user>:<password>@<host>
```

| Property      | Required | Default                          |
|---------------|----------|----------------------------------|
| `user`        | yes      | —                                |
| `password`    | yes      | —                                |
| `reportPath`  | no       | auto-deployed on first connect   |
| `timeout`     | no       | `120` seconds                    |

On **Test Connection** or a normal JDBC connection with a blank `reportPath`, the driver creates missing folders, the data model `dm.xdm` and the report `csv.xdo` under `/~<user>/FusionQuery/v1/`. It reuses existing objects and completes interrupted installations without overwriting them. Catalog setup and connection validation use SOAP on all Fusion hosts.

The connection succeeds only after the report returns the expected result from `SELECT 1 FROM DUAL`. Setup or execution failures report the path and server reason immediately. The Fusion user needs permission to create Publisher folders, data models and reports in My Folders and access the model's data source. The driver does not grant permissions or fall back to the shared `/Custom/FusionQuery/Proxy/v1/csv.xdo` report.

If `reportPath` is explicitly set, that existing report is validated without creating catalog objects. **Clear an old `reportPath` value to enable automatic setup.**

When upgrading DBeaver, replace the old JAR in **Driver Settings → Libraries**, keep **Use legacy JDBC instantiation** enabled and reconnect. Restart DBeaver if it still holds the old driver in memory.

---

## Errors

Translated Oracle codes:

- `ORA-00904` → invalid identifier (suggests `all_tab_columns` catalog query)
- `ORA-00942` → table/view not found (suggests `all_views` catalog query)
- `ORA-01017` → invalid username/password
- `ORA-01722`, `ORA-00907`, `ORA-00933`, `ORA-00936` → translated with hints

---

## Troubleshooting

- **Path A: "Oracle Fusion Cloud (BIP)" missing from dropdown** — Cmd+Q (full quit), then reopen. First boot after install takes ~10s longer (rebuilding cache).
- **Path A: "creator is null" NPE** — extension didn't load. Re-run installer; ensure full quit; verify `<userdir>/user_extensions/fusion-sqldev-extension-1.0.0.jar` exists.
- **Driver class not found** — class is exactly `com.fusionquery.jdbc.FusionDriver`.
- **Setup permission denied** — the connection error identifies the failed catalog operation. Ask the Fusion administrator to verify creation rights in My Folders and data source access.

---

Full project source, build instructions, and architecture notes: https://github.com/AleCyriaco/OracleFusionDbeaverSQLDeveloper
