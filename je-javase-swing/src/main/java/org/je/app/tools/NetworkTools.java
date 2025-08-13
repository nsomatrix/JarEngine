package org.je.app.tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.*;
import org.je.util.NetEventBus;
import org.je.util.net.NetConfig;

/**
 * Consolidated Network Tools launcher for the emulator.
 * Provides panels for monitor, traffic shaping, connection testing, DNS overrides,
 * mock server, TLS scenarios, packet capture, offline mode, metrics, and UDP.
 */
public class NetworkTools extends JFrame {
    private static final long serialVersionUID = 1L;

    public NetworkTools(Frame owner) {
        this(owner, 0);
    }

    public NetworkTools(Frame owner, int selectedTabIndex) {
        super("Network Tools");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Monitor", new MonitorPanel());
        tabs.addTab("Traffic", new TrafficPanel());
        tabs.addTab("Tester", new TesterPanel());
        tabs.addTab("DNS", new DnsPanel());
        tabs.addTab("Mock", new MockServerPanel());
        tabs.addTab("TLS", new TlsPanel());
        tabs.addTab("Capture", new CapturePanel());
        tabs.addTab("Offline", new OfflinePanel());
        tabs.addTab("Metrics", new MetricsPanel());
        tabs.addTab("UDP", new UdpPanel());

        add(tabs, BorderLayout.CENTER);
        setSize(800, 540);
        setLocationRelativeTo(owner);
        if (selectedTabIndex >= 0 && selectedTabIndex < tabs.getTabCount()) {
            tabs.setSelectedIndex(selectedTabIndex);
        }
    }

    // ---------- Event Bus scaffolding ----------
    // Remove local bus; use shared org.je.util.NetEventBus

    // Use shared NetConfig managers

    // ---------- Panels ----------
    static class MonitorPanel extends JPanel {
        private final DefaultTableModel model;
        private int lastIndex = 0;
        private javax.swing.Timer timer;
        MonitorPanel() {
            setLayout(new BorderLayout());
            model = new DefaultTableModel(new Object[]{"Time", "Type", "Dir", "Target", "Info"}, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model);
            table.setAutoCreateRowSorter(true);

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton clear = new JButton("Clear");
            top.add(clear);
            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            clear.addActionListener(e -> {
                NetEventBus.clear();
                model.setRowCount(0);
                lastIndex = 0;
            });

            // Prime existing events and start live updates
            appendNewEvents();
            timer = new javax.swing.Timer(500, e -> appendNewEvents());
            timer.start();
        }
        private void appendNewEvents() {
            java.util.List<NetEventBus.NetEvent> all = NetEventBus.snapshot();
            for (int i = lastIndex; i < all.size(); i++) {
                NetEventBus.NetEvent ev = all.get(i);
                model.addRow(new Object[]{ new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date(ev.ts)), ev.type, ev.direction, ev.target, ev.info });
            }
            lastIndex = all.size();
        }
        @Override public void addNotify() { super.addNotify(); if (timer != null) timer.start(); }
        @Override public void removeNotify() { if (timer != null) timer.stop(); super.removeNotify(); }
    }

    static class TrafficPanel extends JPanel {
        TrafficPanel() {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(10,10,10,10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5); gbc.anchor = GridBagConstraints.WEST;

            JSpinner bw = new JSpinner(new SpinnerNumberModel(NetConfig.Traffic.bandwidthKbps, 0, 1_000_000, 16));
            JSpinner lat = new JSpinner(new SpinnerNumberModel(NetConfig.Traffic.latencyMs, 0, 60_000, 10));
            JSpinner jit = new JSpinner(new SpinnerNumberModel(NetConfig.Traffic.jitterMs, 0, 60_000, 5));
            JSpinner loss = new JSpinner(new SpinnerNumberModel(NetConfig.Traffic.packetLossPct, 0, 100, 1));
            JButton apply = new JButton("Apply");

            int r=0;
            add(new JLabel("Bandwidth (kbps, 0=unlimited)"), pos(gbc,0,r)); add(bw, pos(gbc,1,r++));
            add(new JLabel("Latency (ms)"), pos(gbc,0,r)); add(lat, pos(gbc,1,r++));
            add(new JLabel("Jitter (ms)"), pos(gbc,0,r)); add(jit, pos(gbc,1,r++));
            add(new JLabel("Packet Loss (%)"), pos(gbc,0,r)); add(loss, pos(gbc,1,r++));
            add(apply, pos(gbc,0,r,2));

            apply.addActionListener(e -> {
                NetConfig.Traffic.bandwidthKbps = (Integer) bw.getValue();
                NetConfig.Traffic.latencyMs = (Integer) lat.getValue();
                NetConfig.Traffic.jitterMs = (Integer) jit.getValue();
                NetConfig.Traffic.packetLossPct = (Integer) loss.getValue();
                NetConfig.savePreferencesAsync();
                JOptionPane.showMessageDialog(this, "Traffic profile applied (affects future I/O when integrated).", "Traffic", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    static class TesterPanel extends JPanel {
        private final JTextField host = new JTextField("example.com", 20);
        private final JSpinner port = new JSpinner(new SpinnerNumberModel(80, 1, 65535, 1));
        private final JComboBox<String> mode = new JComboBox<>(new String[]{"DNS", "TCP", "TLS"});
        private final JTextArea out = new JTextArea(10, 60);
        TesterPanel() {
            setLayout(new BorderLayout(8,8));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton run = new JButton("Run");
            top.add(new JLabel("Host:")); top.add(host);
            top.add(new JLabel("Port:")); top.add(port);
            top.add(new JLabel("Mode:")); top.add(mode);
            top.add(run);
            add(top, BorderLayout.NORTH);
            out.setEditable(false);
            add(new JScrollPane(out), BorderLayout.CENTER);

            run.addActionListener(this::runTest);
        }
        private void runTest(ActionEvent e) {
            String h = host.getText().trim();
            int p = (Integer) port.getValue();
            String m = Objects.toString(mode.getSelectedItem(), "DNS");
            out.setText("");
            try {
                if ("DNS".equals(m)) doDns(h);
                else if ("TCP".equals(m)) doTcp(h, p);
                else doTls(h, p);
            } catch (Exception ex) {
                out.append("ERROR: "+ ex.getClass().getSimpleName()+": "+ ex.getMessage()+"\n");
            }
        }
        private void doDns(String h) throws UnknownHostException {
            long t0 = System.currentTimeMillis();
            InetAddress[] addrs = InetAddress.getAllByName(h);
            long dt = System.currentTimeMillis()-t0;
            out.append("Resolved in "+dt+" ms\n");
            for (InetAddress a: addrs) out.append("- "+a.getHostAddress()+"\n");
        }
        private void doTcp(String h, int p) throws IOException {
            long t0 = System.currentTimeMillis();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(h, p), 5000);
                long dt = System.currentTimeMillis()-t0;
                out.append("TCP connected in "+dt+" ms\n");
                NetEventBus.publish("TCP","OUT", h+":"+p, "Tester connect");
            }
        }
        private void doTls(String h, int p) throws Exception {
            long t0 = System.currentTimeMillis();
            SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket s = (SSLSocket) fac.createSocket()) {
                s.connect(new InetSocketAddress(h, p), 6000);
                s.startHandshake();
                long dt = System.currentTimeMillis()-t0;
                out.append("TLS handshake in "+dt+" ms\n");
                SSLSession sess = s.getSession();
                out.append("Protocol: "+sess.getProtocol()+"\n");
                out.append("Cipher: "+sess.getCipherSuite()+"\n");
                try {
                    java.security.cert.Certificate cert = sess.getPeerCertificates()[0];
                    out.append("Peer: "+cert.getType()+"\n");
                } catch (Exception ignore) {}
                NetEventBus.publish("TLS","OUT", h+":"+p, "Tester handshake");
            }
        }
    }

    static class DnsPanel extends JPanel {
        private final DefaultTableModel model;
        DnsPanel() {
            setLayout(new BorderLayout(6,6));
            model = new DefaultTableModel(new Object[]{"Host", "IP"}, 0);
            JTable table = new JTable(model);
            reload();
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField host = new JTextField(14); JTextField ip = new JTextField(12);
            JButton add = new JButton("Add"); JButton del = new JButton("Delete"); JButton clear = new JButton("Clear");
            controls.add(new JLabel("Host:")); controls.add(host); controls.add(new JLabel("IP:")); controls.add(ip); controls.add(add); controls.add(del); controls.add(clear);
            add(controls, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            add.addActionListener(e -> {
                String h = host.getText().trim(); String a = ip.getText().trim();
                if (!h.isEmpty() && !a.isEmpty()) { NetConfig.Dns.put(h, a); NetConfig.savePreferencesAsync(); reload(); }
            });
            del.addActionListener(e -> {
                int i = table.getSelectedRow(); if (i>=0) {
                    String h = Objects.toString(model.getValueAt(i,0),""); NetConfig.Dns.remove(h); NetConfig.savePreferencesAsync(); reload();
                }
            });
            clear.addActionListener(e -> { NetConfig.Dns.clear(); NetConfig.savePreferencesAsync(); reload(); });
        }
        private void reload() {
            model.setRowCount(0);
            for (Map.Entry<String,String> e : NetConfig.Dns.snapshot().entrySet()) {
                model.addRow(new Object[]{e.getKey(), e.getValue()});
            }
        }
    }

    static class MockServerPanel extends JPanel {
        private com.sun.net.httpserver.HttpServer server;
        private final JTextField port = new JTextField("8081", 6);
        private final JTextArea response = new JTextArea(6, 60);
        private final JTextField path = new JTextField("/", 10);
        private final JButton start = new JButton("Start");
        private final JButton stop = new JButton("Stop");
        MockServerPanel() {
            setLayout(new BorderLayout(6,6));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(new JLabel("Port:")); top.add(port);
            top.add(new JLabel("Path:")); top.add(path);
            top.add(start); top.add(stop);
            add(top, BorderLayout.NORTH);
            response.setText("Hello from Mock Server\n");
            add(new JScrollPane(response), BorderLayout.CENTER);
            start.addActionListener(this::onStart);
            stop.addActionListener(e -> onStop());
            stop.setEnabled(false);
        }
        private void onStart(ActionEvent e) {
            try {
                int p = Integer.parseInt(port.getText().trim());
                server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
                String route = path.getText().trim();
                server.createContext(route, exchange -> {
                    byte[] body = response.getText().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                });
                server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
                server.start();
                start.setEnabled(false); stop.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Mock server running on 127.0.0.1:"+p, "Mock", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Failed to start: "+ex.getMessage(), "Mock", JOptionPane.ERROR_MESSAGE);
            }
        }
        private void onStop() {
            if (server != null) { server.stop(0); server = null; }
            start.setEnabled(true); stop.setEnabled(false);
        }
    }

    static class TlsPanel extends JPanel {
        TlsPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            JCheckBox trust = new JCheckBox("Trust all certificates (unsafe)", NetConfig.TLS.trustAll);
            JButton apply = new JButton("Apply");
            add(trust); add(apply);
            apply.addActionListener(e -> {
                NetConfig.TLS.trustAll = trust.isSelected();
                NetConfig.TLS.applyDefaultContextIfNeeded();
                NetConfig.savePreferencesAsync();
                JOptionPane.showMessageDialog(this, "TLS settings applied.", "TLS", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    static class CapturePanel extends JPanel {
        CapturePanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            JButton exportHar = new JButton("Export HAR-like log");
            add(exportHar);
            exportHar.addActionListener(e -> doExport());
        }
        private void doExport() {
            java.awt.FileDialog fd = new java.awt.FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Save Log", java.awt.FileDialog.SAVE);
            fd.setFile("network_log.txt"); fd.setVisible(true);
            if (fd.getFile()==null) return;
            java.io.File f = new java.io.File(fd.getDirectory(), fd.getFile());
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(f), StandardCharsets.UTF_8))) {
                for (NetEventBus.NetEvent ev : NetEventBus.snapshot()) {
                    pw.printf("%tF %<tT.%<tL | %s | %s | %s | %s%n", new Date(ev.ts), ev.type, ev.direction, ev.target, ev.info);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: "+ex.getMessage(), "Capture", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Exported.", "Capture", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    static class OfflinePanel extends JPanel {
        OfflinePanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            JCheckBox offline = new JCheckBox("Offline mode (block new connections)", NetConfig.Policy.offline);
            JCheckBox captive = new JCheckBox("Simulate captive portal (redirect)", NetConfig.Policy.captivePortal);
            JSpinner captivePort = new JSpinner(new SpinnerNumberModel(NetConfig.Policy.captivePort, 1, 65535, 1));
            JButton apply = new JButton("Apply");
            add(offline); add(captive); add(new JLabel("Port:")); add(captivePort); add(apply);
            apply.addActionListener(e -> {
                NetConfig.Policy.offline = offline.isSelected();
                NetConfig.Policy.captivePortal = captive.isSelected();
                try { NetConfig.Policy.captivePort = (Integer) captivePort.getValue(); } catch (Throwable ignore) {}
                NetConfig.savePreferencesAsync();
                JOptionPane.showMessageDialog(this, "Network policy applied.", "Offline", JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    static class MetricsPanel extends JPanel {
        private final JLabel count = new JLabel("0 events");
        MetricsPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            JButton refresh = new JButton("Refresh");
            add(new JLabel("Events seen:")); add(count); add(refresh);
            refresh.addActionListener(e -> update());
            update();
        }
    private void update() { count.setText(Integer.toString(NetEventBus.count()) + " events"); }
    }

    static class UdpPanel extends JPanel {
        private final JTextField host = new JTextField("127.0.0.1", 12);
        private final JSpinner port = new JSpinner(new SpinnerNumberModel(9999, 1, 65535, 1));
        private final JTextField message = new JTextField("ping", 20);
        UdpPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            JButton send = new JButton("Send UDP");
            add(new JLabel("Host:")); add(host);
            add(new JLabel("Port:")); add(port);
            add(new JLabel("Data:")); add(message);
            add(send);
            send.addActionListener(e -> doSend());
        }
        private void doSend() {
            try (DatagramSocket ds = new DatagramSocket()) {
                byte[] buf = message.getText().getBytes(StandardCharsets.UTF_8);
                InetAddress ia = InetAddress.getByName(host.getText().trim());
                DatagramPacket p = new DatagramPacket(buf, buf.length, ia, (Integer) port.getValue());
                ds.send(p);
                NetEventBus.publish("UDP","OUT", host.getText().trim()+":"+port.getValue(), "Tester datagram: "+message.getText());
                JOptionPane.showMessageDialog(this, "Sent.", "UDP", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed: "+ex.getMessage(), "UDP", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static GridBagConstraints pos(GridBagConstraints gbc, int x, int y) {
        GridBagConstraints c = (GridBagConstraints) gbc.clone();
        c.gridx = x; c.gridy = y; c.gridwidth = 1; return c;
    }
    private static GridBagConstraints pos(GridBagConstraints gbc, int x, int y, int w) {
        GridBagConstraints c = (GridBagConstraints) gbc.clone();
        c.gridx = x; c.gridy = y; c.gridwidth = w; return c;
    }

    // ---------- Small dialog helpers ----------
    public static JDialog openDialogForPanel(Frame owner, String title, JPanel panel, Dimension size) {
        JDialog d = new JDialog(owner, title, false);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setLayout(new BorderLayout());
        d.add(panel, BorderLayout.CENTER);
        if (size != null) {
            panel.setPreferredSize(size);
            d.pack();
        } else {
            d.pack();
        }
        d.setLocationRelativeTo(owner);
        // Ensure initial layout/paint so it isn't blank until hover
        d.getContentPane().invalidate();
        d.getContentPane().validate();
        d.getContentPane().repaint();
        return d;
    }
    public static JDialog openMonitorDialog(Frame owner) { return openDialogForPanel(owner, "Network Monitor", new MonitorPanel(), null); }
    public static JDialog openTrafficDialog(Frame owner) { return openDialogForPanel(owner, "Traffic Shaping", new TrafficPanel(), null); }
    public static JDialog openTesterDialog(Frame owner) { return openDialogForPanel(owner, "Connection Tester", new TesterPanel(), null); }
    public static JDialog openDnsDialog(Frame owner) { return openDialogForPanel(owner, "DNS Overrides", new DnsPanel(), null); }
    public static JDialog openMockDialog(Frame owner) { return openDialogForPanel(owner, "Mock Server", new MockServerPanel(), null); }
    public static JDialog openTlsDialog(Frame owner) { return openDialogForPanel(owner, "TLS Settings", new TlsPanel(), null); }
    public static JDialog openCaptureDialog(Frame owner) { return openDialogForPanel(owner, "Packet Capture", new CapturePanel(), null); }
    public static JDialog openOfflineDialog(Frame owner) { return openDialogForPanel(owner, "Offline / Captive", new OfflinePanel(), null); }
    public static JDialog openMetricsDialog(Frame owner) { return openDialogForPanel(owner, "Network Metrics", new MetricsPanel(), null); }
    public static JDialog openUdpDialog(Frame owner) { return openDialogForPanel(owner, "UDP Tester", new UdpPanel(), null); }
}
