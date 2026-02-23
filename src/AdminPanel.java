import java.sql.*;
import javax.swing.*;

public class AdminPanel extends JFrame {

    JTextArea results;

    public AdminPanel() {
        setTitle("Election Results");
        setSize(400, 300);

        results = new JTextArea();
        add(new JScrollPane(results));

        loadResults();
        setVisible(true);
    }

    private void loadResults() {
        try (Connection con = DBConnection.getConnection()) {

            String sql = """
                SELECT c.name, c.party, COUNT(v.vote_id) AS total
                FROM candidate c
                LEFT JOIN vote v ON c.candidate_id = v.candidate_id
                GROUP BY c.candidate_id
                """;

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                results.append(
                    rs.getString("name") + " - " +
                    rs.getString("party") + " : " +
                    rs.getInt("total") + " votes\n"
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}