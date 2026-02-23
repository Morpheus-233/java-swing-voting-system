import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class VotingFrame extends JFrame {

    int voterId;
    ButtonGroup group = new ButtonGroup();
    private JPanel candidatesPanel;
    private JButton submitButton;

    public VotingFrame(int voterId) {
        this.voterId = voterId;

        setTitle("Cast Your Vote");
        setSize(450, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel();
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Select a Candidate");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        main.add(title);
        main.add(Box.createVerticalStrut(20));

        candidatesPanel = new JPanel();
        candidatesPanel.setOpaque(false);
        candidatesPanel.setLayout(new BoxLayout(candidatesPanel, BoxLayout.Y_AXIS));
        main.add(candidatesPanel);

        submitButton = new JButton("Submit Vote");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setBackground(new Color(231, 76, 60));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        submitButton.setEnabled(false);

        submitButton.addActionListener(e -> submitVote());

        main.add(Box.createVerticalStrut(20));
        main.add(submitButton);

        add(main);
        setVisible(true);

        loadCandidates();
    }

    private void loadCandidates() {
        candidatesPanel.removeAll();
        JLabel loading = new JLabel("Loading candidates...");
        loading.setAlignmentX(Component.CENTER_ALIGNMENT);
        loading.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        candidatesPanel.add(loading);
        candidatesPanel.revalidate();
        candidatesPanel.repaint();

        SwingWorker<List<Candidate>, Void> worker = new SwingWorker<List<Candidate>, Void>() {
            @Override
            protected List<Candidate> doInBackground() throws Exception {
                List<Candidate> candidates = new ArrayList<Candidate>();

                try (Connection con = DBConnection.getConnection();
                     Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT candidate_id, name, party FROM candidate")) {

                    while (rs.next()) {
                        candidates.add(new Candidate(
                            rs.getInt("candidate_id"),
                            rs.getString("name"),
                            rs.getString("party")
                        ));
                    }
                }

                return candidates;
            }

            @Override
            protected void done() {
                candidatesPanel.removeAll();

                try {
                    List<Candidate> candidates = get();

                    if (candidates.isEmpty()) {
                        JLabel empty = new JLabel("No candidates available.");
                        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                        candidatesPanel.add(empty);
                        submitButton.setEnabled(false);
                    } else {
                        for (Candidate candidate : candidates) {
                            JRadioButton rb = new JRadioButton(candidate.name + " (" + candidate.party + ")");
                            rb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                            rb.setOpaque(false);
                            rb.setActionCommand(String.valueOf(candidate.id));
                            group.add(rb);
                            candidatesPanel.add(rb);
                            candidatesPanel.add(Box.createVerticalStrut(10));
                        }
                        submitButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        VotingFrame.this,
                        "Failed to load candidates.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    submitButton.setEnabled(false);
                }

                candidatesPanel.revalidate();
                candidatesPanel.repaint();
            }
        };

        worker.execute();
    }

    private void submitVote() {
        if (group.getSelection() == null) {
            JOptionPane.showMessageDialog(this, "Select a candidate.");
            return;
        }

        final int candidateId = Integer.parseInt(group.getSelection().getActionCommand());
        submitButton.setEnabled(false);

        SwingWorker<VoteResult, Void> worker = new SwingWorker<VoteResult, Void>() {
            @Override
            protected VoteResult doInBackground() {
                VoteResult result = new VoteResult();

                try (Connection con = DBConnection.getConnection()) {
                    con.setAutoCommit(false);

                    try (PreparedStatement updateVoter = con.prepareStatement(
                             "UPDATE voter SET has_voted=TRUE WHERE voter_id=? AND has_voted=FALSE"
                         )) {

                        updateVoter.setInt(1, voterId);
                        int updated = updateVoter.executeUpdate();

                        if (updated == 0) {
                            con.rollback();
                            result.alreadyVoted = true;
                            return result;
                        }
                    }

                    try (PreparedStatement vote = con.prepareStatement(
                             "INSERT INTO vote (voter_id, candidate_id) VALUES (?,?)"
                         )) {

                        vote.setInt(1, voterId);
                        vote.setInt(2, candidateId);
                        vote.executeUpdate();
                    }

                    con.commit();
                    result.success = true;
                } catch (Exception ex) {
                    result.error = ex;
                }

                return result;
            }

            @Override
            protected void done() {
                try {
                    VoteResult result = get();

                    if (result.success) {
                        JOptionPane.showMessageDialog(
                            VotingFrame.this,
                            "Vote submitted successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        dispose();
                        AdminPanel adminPanel = new AdminPanel();
                        if (adminPanel.results != null) {
                            adminPanel.results.setEditable(false);
                        }
                    } else if (result.alreadyVoted) {
                        JOptionPane.showMessageDialog(
                            VotingFrame.this,
                            "You have already voted.",
                            "Already Voted",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(
                            VotingFrame.this,
                            "Vote submission failed. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        submitButton.setEnabled(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        VotingFrame.this,
                        "Vote submission failed. Please try again.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    submitButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private static class Candidate {
        int id;
        String name;
        String party;

        Candidate(int id, String name, String party) {
            this.id = id;
            this.name = name;
            this.party = party;
        }
    }

    private static class VoteResult {
        boolean success;
        boolean alreadyVoted;
        Exception error;
    }
}
