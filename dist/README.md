# Fusion Query JDBC — Release Package

A JDBC driver that connects any JDBC-compatible SQL client to **Oracle Fusion Cloud** via BI Publisher.

This package contains both install paths. Pick whichever fits your client.

| File                                  | Use for                                                              |
|---------------------------------------|----------------------------------------------------------------------|
| `fusion-query-jdbc-1.0.0.jar`         | Standalone driver (DBeaver, DataGrip, SQL Developer's JDBC tab)      |
| `fusion-sqldev-installer-1.0.0.jar`   | Cross-platform installer for SQL Developer's native connection type  |

Requires **Java 11+**.

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
4. Fill in Username / Password / Hostname (e.g. `fa-xxxx-saasfaprod1.fa.ocs.oraclecloud.com`).

---

## Path B — Oracle SQL Developer (generic JDBC)

1. Open SQL Developer.
2. *Tools → Preferences → Database → Third Party JDBC Drivers → Add Entry…* and pick `fusion-query-jdbc-1.0.0.jar`.
3. **New Connection** → **JDBC** tab.
4. URL: `jdbc:fusion://fa-xxxx-saasfaprod1.fa.ocs.oraclecloud.com`
5. Username / Password → **Test** → **Connect**.

---

## DBeaver

1. *Database → Driver Manager → New*.
2. Driver Name: `Oracle Fusion Cloud (BIP)`, Class Name: `com.fusionquery.jdbc.FusionDriver`, URL Template: `jdbc:fusion://{host}`.
3. **Libraries** tab → **Add File** → pick `fusion-query-jdbc-1.0.0.jar` → **OK**.
4. *Database → New Database Connection → Oracle Fusion Cloud (BIP)*.
5. Host: `fa-xxxx-saasfaprod1.fa.ocs.oraclecloud.com`, User, Password → **Test Connection** → **Finish**.

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

On first connect with no `reportPath`, the driver auto-deploys the proxy report to `/~<user>/FusionQuery/v1/csv.xdo`. OCI/OCS hosts (`.ocs.` in hostname) automatically use SOAP transport.

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
- **OCS host hangs** — driver auto-detects `.ocs.` and falls back to SOAP.

---

Full project source, build instructions, and architecture notes: https://github.com/AleCyriaco/OracleFusionDbeaverSQLDeveloper
