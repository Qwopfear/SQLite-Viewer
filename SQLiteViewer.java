package viewer;

import org.sqlite.SQLiteDataSource;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SQLiteViewer extends JFrame {

    String url = "";

    private JPanel connectionPanel;
    private JPanel dataBaseManagerPanel;
    private JTable queryResultManagePanel;

    private Map<String, Component> dataBaseManagerPanelItems;


    public SQLiteViewer() {
        setTitle("SQLite Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 900);
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);


        connectionPanel = initConnectionPanel();
        add(connectionPanel);

        dataBaseManagerPanel = initDataBaseManagerPanel();
        add(dataBaseManagerPanel);


        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(700, 300));
        panel.setBackground(new Color(215, 11, 11));


        queryResultManagePanel = new JTable();
        queryResultManagePanel.setName("Table");
        queryResultManagePanel.setVisible(true);
        panel.add(queryResultManagePanel);


        JScrollPane sp = new JScrollPane(queryResultManagePanel);
        sp.setPreferredSize(new Dimension(700, 500));
        this.add(sp);


    }

    private JPanel initDataBaseManagerPanel() {
        dataBaseManagerPanelItems = new HashMap<>();

        JPanel dataBaseManagerPanel = new JPanel();
        dataBaseManagerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 40, 10));
        dataBaseManagerPanel.setVisible(true);
        dataBaseManagerPanel.setPreferredSize(new Dimension(700, 300));

        JTextArea queryTextArea = new JTextArea();
        queryTextArea.setName("QueryTextArea");
        queryTextArea.setPreferredSize(new Dimension(500, 200));
        queryTextArea.setVisible(true);
        queryTextArea.setEnabled(false);


        JScrollPane sc = new JScrollPane(queryTextArea);
        sc.setVisible(true);


        JComboBox<String> tablesBox = new JComboBox<>();
        tablesBox.setVisible(true);
        tablesBox.setPreferredSize(new Dimension(600, 30));
        tablesBox.setName("TablesComboBox");
        tablesBox.addItemListener(e -> {
            queryTextArea.setText("SELECT * FROM " + tablesBox.getSelectedItem() + ";");
        });


        JButton executeQueryButton = new JButton("Execute");
        executeQueryButton.setVisible(true);
        executeQueryButton.setName("ExecuteQueryButton");
        executeQueryButton.addActionListener(e -> {
            executeCustomQuery(queryTextArea.getText());
        });
        executeQueryButton.setEnabled(false);

        dataBaseManagerPanel.add(tablesBox);
        dataBaseManagerPanel.add(sc);
        dataBaseManagerPanel.add(executeQueryButton);

        dataBaseManagerPanelItems.put(tablesBox.getName(), tablesBox);
        dataBaseManagerPanelItems.put(queryTextArea.getName(), queryTextArea);
        dataBaseManagerPanelItems.put(executeQueryButton.getName(), executeQueryButton);

        return dataBaseManagerPanel;

    }

    private JPanel initConnectionPanel() {
        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 40, 10));

        connectionPanel.setVisible(true);
        connectionPanel.setPreferredSize(new Dimension(700, 100));

        JTextField dataBaseField = new JTextField();
        dataBaseField.setPreferredSize(new Dimension(500, 20));
        dataBaseField.setName("FileNameTextField");

        JTextField localPath = new JTextField();
        localPath.setPreferredSize(new Dimension(500, 20));
        localPath.setName("LocalPath");


        JButton openButton = new JButton("Open");
        openButton.setName("OpenFileButton");
        openButton.addActionListener(e -> {
            url = "jdbc:sqlite:" + localPath.getText() + dataBaseField.getText();
            connectToDb(url);
        });



        connectionPanel.add(new JLabel("DB name"));
        connectionPanel.add(dataBaseField);

        connectionPanel.add(new JLabel("Local Path"));
        connectionPanel.add(localPath);
        connectionPanel.add(openButton);

        return connectionPanel;
    }

    private void connectToDb(String url) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);
        JComboBox<String> tablesBox = ((JComboBox<String>) dataBaseManagerPanelItems.get("TablesComboBox"));
        tablesBox.removeAllItems();

        try (Connection c = dataSource.getConnection()) {

            if (c.isValid(5)) {
                Statement s = c.createStatement();
                ResultSet set = s.executeQuery("SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%';");

                while (set.next()) {
                    tablesBox.addItem(set.getString("name"));
                }

                if (tablesBox.getItemCount() == 0) {
                    JOptionPane.showMessageDialog(new JFrame(),"Wrong file name!");
                    dataBaseManagerPanelItems.get("QueryTextArea").setEnabled(false);
                    dataBaseManagerPanelItems.get("ExecuteQueryButton").setEnabled(false);
                } else {
                    dataBaseManagerPanelItems.get("QueryTextArea").setEnabled(true);
                    dataBaseManagerPanelItems.get("ExecuteQueryButton").setEnabled(true);
                }

            }


        } catch (SQLException e) {
            System.out.println("Wrong path");
        }
}

    private void executeCustomQuery(String queryText) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection connection = dataSource.getConnection()) {
            Statement s = connection.createStatement();
            ResultSet resultSet = s.executeQuery(queryText);
            int columnsCount = resultSet.getMetaData().getColumnCount();
            String[] columns = new String[columnsCount];

            for (int i = 0; i < columnsCount; i++) {
                columns[i] = resultSet.getMetaData().getColumnName(i + 1);
            }

            System.out.println(Arrays.toString(columns));


            ArrayList<Object[]> data = new ArrayList<>();
            while (resultSet.next()) {
                Object[] row = new Object[columnsCount];
                for (int i = 0; i < columnsCount; i++) {
                    row[i] = resultSet.getString(i + 1);
                }
                System.out.println(Arrays.toString(row));
                data.add(row);
            }

            System.out.println(data);

            System.out.println("- - - - - - - - - - - - -Data to Array - - - - - - -- - - -");

            for (Object[] objects : data.toArray(new Object[0][])) {
                System.out.println(Arrays.toString(objects));
            }


            QueryTableModel tableModel = new QueryTableModel(columns, data.toArray(new Object[0][]));

            queryResultManagePanel.setModel(tableModel);


        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(new JFrame(), ex.getMessage());
        }
    }


}

class QueryTableModel extends AbstractTableModel {

    String[] columns;
    Object[][] data;

    public QueryTableModel(String[] columns, Object[][] data) {
        this.columns = columns;
        this.data = data;
    }


    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }
}



