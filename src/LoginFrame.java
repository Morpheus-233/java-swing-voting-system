import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class LoginFrame extends JFrame {

    JTextField username;
    JPasswordField password;
    private JButton loginButton;
    private JButton registerButton;

    public LoginFrame() {
        setTitle("Online Voting System");
        setSize(420, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        Font TITLE = new Font("Segoe UI", Font.BOLD, 20);
        Font NORMAL = new Font("Segoe UI", Font.PLAIN, 14);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(245, 247, 250));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Login");
        title.setFont(TITLE);
        title.setForeground(new Color(44, 62, 80));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        card.add(title, c);

        c.gridwidth = 1;

        JLabel uLabel = new JLabel("Username");
        uLabel.setFont(NORMAL);
        c.gridy = 1;
        c.gridx = 0;
        card.add(uLabel, c);

        username = new JTextField();
        c.gridx = 1;
        card.add(username, c);

        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(NORMAL);
        c.gridy = 2;
        c.gridx = 0;
        card.add(pLabel, c);

        password = new JPasswordField();
        c.gridx = 1;
        card.add(password, c);

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        styleButton(loginButton);
        styleButton(registerButton);

        c.gridy = 3;
        c.gridx = 0;
        card.add(loginButton, c);
        c.gridx = 1;
        card.add(registerButton, c);

        loginButton.addActionListener(e -> authenticate());
        registerButton.addActionListener(e -> {
            new RegisterFrame();
            dispose();
        });

        root.add(card);
        add(root);
        setVisible(true);
    }

    private void styleButton(JButton btn) {
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void authenticate() {
        final String user = username.getText().trim();
        final String pass = new String(password.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter username and password.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);

        SwingWorker<LoginResult, Void> worker = new SwingWorker<LoginResult, Void>() {
            @Override
            protected LoginResult doInBackground() {
                LoginResult result = new LoginResult();

                try (Connection con = DBConnection.getConnection()) {
                    if (con == null) {
                        result.errorMessage = "Cannot connect to database. Check MySQL server and DB credentials.";
                        return result;
                    }

                    try (PreparedStatement ps = con.prepareStatement(
                             "SELECT voter_id, has_voted FROM voter WHERE username=? AND password=?"
                         )) {

                        ps.setString(1, user);
                        ps.setString(2, pass);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                result.valid = true;
                                result.voterId = rs.getInt("voter_id");
                                result.hasVoted = rs.getBoolean("has_voted");
                            }
                        }
                    }
                } catch (Exception ex) {
                    result.errorMessage = ex.getMessage();
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    LoginResult result = get();

                    if (result.errorMessage != null) {
                        JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Login failed: " + result.errorMessage,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        loginButton.setEnabled(true);
                        registerButton.setEnabled(true);
                        return;
                    }

                    if (!result.valid) {
                        JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Invalid credentials.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                        loginButton.setEnabled(true);
                        registerButton.setEnabled(true);
                        return;
                    }

                    if (result.hasVoted) {
                        JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "You have already voted.",
                            "Already Voted",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        loginButton.setEnabled(true);
                        registerButton.setEnabled(true);
                    } else {
                        dispose();
                        new VotingFrame(result.voterId);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        LoginFrame.this,
                        "Login failed. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private static class LoginResult {
        boolean valid;
        boolean hasVoted;
        int voterId;
        String errorMessage;
    }
}
