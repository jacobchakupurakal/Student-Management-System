import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;
import javax.imageio.ImageIO;

public class StudentManagementSystemGUI {
    private JFrame frame;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JTextField addressField;
    private JTextField studentIdField;
    private JTable studentTable;
    private Connection connection;
    private JLabel statusLabel;

    public StudentManagementSystemGUI() {
        // Optional nicer LAF
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}

        frame = new JFrame("Student Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(950, 600));

        // Background panel that paints an image
        BackgroundPanel bg = new BackgroundPanel("/bg.jpg"); // put bg.jpg on classpath, or beside class (fallback handled)
        bg.setLayout(new BorderLayout());
        frame.setContentPane(bg);

        // Glass (semi-transparent) container for content
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        bg.add(content, BorderLayout.CENTER);

        // ===== Top: Title Bar =====
        content.add(buildTitleBar(), BorderLayout.NORTH);

        // ===== Center: Split (Form | Table) =====
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildFormPanel(), buildTablePanel());
        split.setResizeWeight(0.38);
        split.setOpaque(false);
        content.add(split, BorderLayout.CENTER);

        // ===== Bottom: Buttons + Status =====
        content.add(buildBottomBar(), BorderLayout.SOUTH);

        // Connect to DB
        connect();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JComponent buildTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);

        JLabel title = new JLabel("Student Management System", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(245, 245, 245));

        JLabel subtitle = new JLabel("Add • View • Update • Delete", SwingConstants.LEFT);
        subtitle.setForeground(new Color(230, 230, 230));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitle);

        titleBar.add(left, BorderLayout.WEST);
        return titleBar;
    }

    private JComponent buildFormPanel() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(false);

        JPanel card = new RoundedPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        studentIdField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        addressField = new JTextField();

        int r = 0;
        addLabeledField(card, gc, r++, "Student ID", studentIdField);
        addLabeledField(card, gc, r++, "Name",      nameField);
        addLabeledField(card, gc, r++, "Email",     emailField);
        addLabeledField(card, gc, r++, "Phone",     phoneField);
        addLabeledField(card, gc, r++, "Address",   addressField);

        shell.add(card, BorderLayout.CENTER);
        return shell;
    }

    private void addLabeledField(JPanel panel, GridBagConstraints gc, int row, String label, JTextField field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; gc.gridwidth = 1;
        JLabel l = new JLabel(label + ":");
        l.setForeground(Color.WHITE);
        panel.add(l, gc);

        gc.gridx = 1; gc.gridy = row; gc.weightx = 1; gc.gridwidth = 1;
        panel.add(field, gc); // directly add the field (no wrapper, no placeholder)
    }

    private JComponent buildTablePanel() {
        JPanel container = new RoundedPanel();
        container.setLayout(new BorderLayout());
        container.setBorder(new EmptyBorder(8, 8, 8, 8));
        container.setOpaque(false);

        // --- Table (plain look) ---
        studentTable = new JTable();
        studentTable.setAutoCreateRowSorter(true);
        studentTable.setFillsViewportHeight(true);
        studentTable.setRowHeight(24);
        studentTable.setBackground(Color.WHITE);
        studentTable.setGridColor(new Color(230, 230, 230));

        // Row click -> populate form (optional)
        studentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = studentTable.getSelectedRow();
                if (row >= 0) {
                    int m = studentTable.convertRowIndexToModel(row);
                    Object id    = studentTable.getModel().getValueAt(m, findColumn("student_id"));
                    Object name  = studentTable.getModel().getValueAt(m, findColumn("name"));
                    Object email = studentTable.getModel().getValueAt(m, findColumn("email"));
                    Object phone = studentTable.getModel().getValueAt(m, findColumn("phone"));
                    Object addr  = studentTable.getModel().getValueAt(m, findColumn("address"));
                    studentIdField.setText(id == null ? "" : String.valueOf(id));
                    nameField.setText(name == null ? "" : String.valueOf(name));
                    emailField.setText(email == null ? "" : String.valueOf(email));
                    phoneField.setText(phone == null ? "" : String.valueOf(phone));
                    addressField.setText(addr == null ? "" : String.valueOf(addr));
                }
            }
        });

        // --- Title (bold; small optional icon) ---
        JLabel tableTitle = new JLabel("  Students", loadIcon("/icons/students.png", 24), SwingConstants.LEFT);
        Font base = tableTitle.getFont();
        tableTitle.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 2f));
        tableTitle.setForeground(Color.WHITE);
        tableTitle.setIconTextGap(8);
        tableTitle.setBorder(new EmptyBorder(6, 8, 6, 8));

        // --- Scroll ---
        JScrollPane scroll = new JScrollPane(studentTable);

        // --- Layout ---
        container.add(tableTitle, BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);

        return container;
    }

    private int findColumn(String name) {
        for (int i = 0; i < studentTable.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(studentTable.getColumnName(i))) return i;
        }
        return 0;
    }

    private JComponent buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 12));
        bar.setOpaque(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);

        JButton addButton = makeButton("Add Student", e -> addStudent());
        JButton viewButton = makeButton("View Students", e -> viewStudents());
        JButton updateButton = makeButton("Update Student", e -> updateStudent());
        JButton deleteButton = makeButton("Delete Student", e -> deleteStudent());

        buttons.add(addButton);
        buttons.add(viewButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);

        statusLabel = new JLabel(" Ready");
        statusLabel.setForeground(Color.WHITE);

        bar.add(buttons, BorderLayout.WEST);
        bar.add(statusLabel, BorderLayout.EAST);
        return bar;
    }

    private JButton makeButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.addActionListener(al);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
        return b;
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/student_db", "root", "Root1234"
            );
            setStatus("Connected to database.");
        } catch (ClassNotFoundException e) {
            error("MySQL driver not found. Add mysql-connector-j to classpath.", e);
        } catch (SQLException e) {
            error("Could not connect to MySQL. Check URL/user/password & server.", e);
        }
    }

    private void addStudent() {
        if (!validateFields(false)) return;
        String sql = "INSERT INTO students (name, email, phone, address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nameField.getText().trim());
            ps.setString(2, emailField.getText().trim());
            ps.setString(3, phoneField.getText().trim());
            ps.setString(4, addressField.getText().trim());
            ps.executeUpdate();
            setStatus("Student added.");
            viewStudents();
            clearNonIdFields();
        } catch (SQLException e) { error("Add failed", e); }
    }

    private void viewStudents() {
        String sql = "SELECT * FROM students ORDER BY student_id DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            studentTable.setModel(resultSetToTableModel(rs));  // no DbUtils needed
            setStatus("Loaded " + studentTable.getRowCount() + " record(s).");
        } catch (SQLException e) { error("Load failed", e); }
    }

    private void updateStudent() {
        if (!validateFields(true)) return;
        String sql = "UPDATE students SET name=?, email=?, phone=?, address=? WHERE student_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nameField.getText().trim());
            ps.setString(2, emailField.getText().trim());
            ps.setString(3, phoneField.getText().trim());
            ps.setString(4, addressField.getText().trim());
            ps.setInt(5, Integer.parseInt(studentIdField.getText().trim()));
            int count = ps.executeUpdate();
            setStatus(count == 0 ? "No record updated." : "Student updated.");
            viewStudents();
        } catch (SQLException e) { error("Update failed", e); }
    }

    private void deleteStudent() {
        String idText = studentIdField.getText().trim();
        if (idText.isEmpty()) { warn("Enter Student ID to delete."); return; }

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Delete student with ID " + idText + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM students WHERE student_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idText));
            int count = ps.executeUpdate();
            setStatus(count == 0 ? "No record deleted." : "Student deleted.");
            viewStudents();
            clearAllFields();
        } catch (SQLException e) { error("Delete failed", e); }
    }

    private boolean validateFields(boolean needId) {
        if (needId && studentIdField.getText().trim().isEmpty()) {
            warn("Student ID is required for Update/Delete."); return false;
        }
        if (nameField.getText().trim().isEmpty()) { warn("Name is required."); return false; }
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            warn("Email looks invalid."); return false;
        }
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("\\d{7,15}")) {
            warn("Phone must be 7–15 digits."); return false;
        }
        return true;
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(frame, msg, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(String title, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(frame, title + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        setStatus("Error: " + e.getMessage());
    }
    private void setStatus(String s) { statusLabel.setText(" " + s); }

    private void clearAllFields() {
        studentIdField.setText(""); clearNonIdFields();
    }
    private void clearNonIdFields() {
        nameField.setText(""); emailField.setText(""); phoneField.setText(""); addressField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StudentManagementSystemGUI::new);
    }

    /* ---------- Helpers ---------- */

    /** Convert a ResultSet to a TableModel (no external libs). */
    private static javax.swing.table.TableModel resultSetToTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();

        Vector<String> columnNames = new Vector<>();
        for (int i = 1; i <= columns; i++) columnNames.add(meta.getColumnName(i));

        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= columns; i++) row.add(rs.getObject(i));
            data.add(row);
        }
        return new javax.swing.table.DefaultTableModel(data, columnNames) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Paints a scaled background image that covers the window (classpath first, then file path fallback). */
    static class BackgroundPanel extends JPanel {
        private BufferedImage image;
        BackgroundPanel(String resourcePath) {
            setPreferredSize(new Dimension(1100, 700));
            try {
                // Try classpath
                var url = getClass().getResource(resourcePath);
                if (url != null) {
                    image = ImageIO.read(url);
                } else {
                    // Fallback: plain file name (e.g., "bg.jpg")
                    File f = new File(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
                    if (f.exists()) image = ImageIO.read(f);
                }
            } catch (IOException ignored) {}
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();

            if (image != null) {
                double imgW = image.getWidth(), imgH = image.getHeight();
                double scale = Math.max(w / imgW, h / imgH);
                int drawW = (int) (imgW * scale);
                int drawH = (int) (imgH * scale);
                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;
                g2.drawImage(image, x, y, drawW, drawH, null);
                g2.setColor(new Color(0, 0, 0, 90)); // darken for contrast
                g2.fillRect(0, 0, w, h);
            } else {
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 40, 60), w, h, new Color(10, 10, 20));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }

    /** Rounded translucent card panel. */
    static class RoundedPanel extends JPanel {
        RoundedPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 24;
            g2.setColor(new Color(30, 30, 30, 120)); // dark glass fill
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(new Color(50, 50, 50, 150)); // border
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Load an optional small PNG icon from classpath; returns null if missing. */
    private static Icon loadIcon(String classpath, int size) {
        try {
            var url = StudentManagementSystemGUI.class.getResource(classpath);
            if (url == null) return null;
            Image img = ImageIO.read(url);
            if (img == null) return null;
            Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            return null;
        }
    }
}
