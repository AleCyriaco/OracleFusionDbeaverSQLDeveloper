package com.fusionquery.sqldev;

import oracle.dbtools.raptor.connections.IConnectionPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class FusionConnectionPanel extends IConnectionPanel {

    private JTextField hostField;
    private JTextField reportPathField;
    private JTextField timeoutField;

    public FusionConnectionPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Hostname row
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        add(new JLabel("Hostname:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        hostField = new JTextField(35);
        hostField.setToolTipText("e.g. fa-xxxx-saasabcd.fa.ocs.oraclecloud.com");
        add(hostField, gbc);

        // Report Path row (optional)
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        add(new JLabel("Report Path (optional):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        reportPathField = new JTextField(35);
        reportPathField.setToolTipText("Leave blank for auto-deploy. Example: /Custom/FusionQuery/Proxy/v1/csv.xdo");
        add(reportPathField, gbc);

        // Timeout row (optional)
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        add(new JLabel("Timeout seconds (optional):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        timeoutField = new JTextField(10);
        timeoutField.setToolTipText("Default: 120");
        add(timeoutField, gbc);

        // Vertical filler
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    @Override
    protected void clear() {
        hostField.setText("");
        reportPathField.setText("");
        timeoutField.setText("");
    }

    @Override
    protected Properties getInput(Properties props) {
        if (props == null) props = new Properties();
        props.put("subtype", FusionConnectionCreator.SUBTYPE);
        props.put("RaptorConnectionType", FusionConnectionCreator.SUBTYPE);
        props.put("driver", FusionConnectionCreator.DRIVER_CLASS);
        String host = hostField.getText().trim();
        if (!host.isEmpty()) props.put(FusionConnectionCreator.PROP_HOST, host);
        String rp = reportPathField.getText().trim();
        if (!rp.isEmpty()) props.put(FusionConnectionCreator.PROP_REPORT_PATH, rp);
        String to = timeoutField.getText().trim();
        if (!to.isEmpty()) props.put(FusionConnectionCreator.PROP_TIMEOUT, to);
        return props;
    }

    @Override
    public String getConnectionType() {
        return FusionConnectionCreator.SUBTYPE;
    }

    @Override
    public String getDriver() {
        return FusionConnectionCreator.DRIVER_CLASS;
    }

    @Override
    public void setDefaultPrompts() {}

    @Override
    public void updatePrompts(Properties props) {
        if (props == null) return;
        hostField.setText(props.getProperty(FusionConnectionCreator.PROP_HOST, ""));
        reportPathField.setText(props.getProperty(FusionConnectionCreator.PROP_REPORT_PATH, ""));
        timeoutField.setText(props.getProperty(FusionConnectionCreator.PROP_TIMEOUT, ""));
    }
}
