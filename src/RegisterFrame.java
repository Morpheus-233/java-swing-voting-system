import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class RegisterFrame extends JFrame {

    JTextField name, username;
    JPasswordField password;
    private JButton register;
    private JButton back;

    public RegisterFrame() {
        setTitle("Voter Registration");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        card.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Register");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        card.add(title, c);

        c.gridwidth = 1;

        c.gridy++;
        c.gridx = 0;
        card.add(new JLabel("Name"), c);
        name = new JTextField();
        c.gridx = 1;
        card.add(name, c);

        c.gridy++;
        c.gridx = 0;
        card.add(new JLabel("Username"), c);
        username = new JTextField();
        c.gridx = 1;
        card.add(username, c);

        c.gridy++;
        c.gridx = 0;
        card.add(new JLabel("Password"), c);
        password = new JPasswordField();
        c.gridx = 1;
        card.add(password, c);

        register = new JButton("Create Account");
        register.setBackground(new Color(46, 204, 113));
        register.setForeground(Color.WHITE);
        register.setFont(new Font("Segoe UI", Font.BOLD, 14));

        back = new JButton("Back");
        back.setBackground(new Color(149, 165, 166));
        back.setForeground(Color.WHITE);
        back.setFont(new Font("Segoe UI", Font.BOLD, 14));

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        card.add(back, c);

        c.gridx = 1;
        card.add(register, c);

        back.addActionListener(e -> goBackToLogin());
        register.addActionListener(e -> registerVoter());

        add(card);
        setVisible(true);
    }

    private void goBackToLogin() {
        dispose();
        new LoginFrame();
    }

    private void registerVoter() {
        final String fullName = name.getText().trim();
        final String user = username.getText().trim();
        final String pass = new String(password.getPassword()).trim();

        if (fullName.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter name, username, and password.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        register.setEnabled(false);
        back.setEnabled(false);

        SwingWorker<RegisterResult, Void> worker = new SwingWorker<RegisterResult, Void>() {
            @Override
            protected RegisterResult doInBackground() {
                RegisterResult result = new RegisterResult();

                try (Connection con = DBConnection.getConnection()) {
                    if (con == null) {
                        result.errorMessage = "Cannot connect to database. Check MySQL server and DB credentials.";
                        return result;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                             "INSERT INTO voter (name, username, password, has_voted) VALUES (?,?,?,FALSE)"
                         )) {

                        ps.setString(1, fullName);
                        ps.setString(2, user);
                        ps.setString(3, pass);
                        ps.executeUpdate();
                        result.success = true;
                    }
                } catch (SQLIntegrityConstraintViolationException ex) {
                    result.duplicateUsername = true;
                } catch (SQLException ex) {
                    String state = ex.getSQLState();
                    if ("23000".equals(state)) {
                        result.duplicateUsername = true;
                    } else {
                        result.errorMessage = ex.getMessage();
                    }
                } catch (Exception ex) {
                    result.errorMessage = ex.getMessage();
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    RegisterResult result = get();

                    if (result.success) {
                        JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            "Registration successful. Please log in to vote.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        dispose();
                        new LoginFrame();
                        return;
                    }

                    if (result.duplicateUsername) {
                        JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            "Username already exists. Please choose another username.",
                            "Duplicate Username",
                            JOptionPane.ERROR_MESSAGE
                        );
                    } else {
                        String message = result.errorMessage == null
                            ? "Registration failed. Please try again."
                            : "Registration failed: " + result.errorMessage;

                        JOptionPane.showMessageDialog(
                            RegisterFrame.this,
                            message,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }

                    register.setEnabled(true);
                    back.setEnabled(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        RegisterFrame.this,
                        "Registration failed. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    register.setEnabled(true);
                    back.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private static class RegisterResult {
        boolean success;
        boolean duplicateUsername;
        String errorMessage;
    }
}
