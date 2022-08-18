
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import java.awt.*;
import java.awt.event.*;

public class App extends JFrame {
    String filename = "";
    Socket socket;
    static String id = "";
    PrintWriter out;
    JPanel panel;

    public App() {
        initUI();
    }

    private void initUI() {

        JButton quitButton = new JButton("Quit");
        JButton reqbutton = new JButton("Select");
        JLabel namelabel = new JLabel("File name: ");
        JTextField name = new JTextField(20);
        JLabel sizelabel = new JLabel("File Size: ");
        JTextField reqid = new JTextField(20);
        JLabel reqlabel = new JLabel("Request To: ");
        JTextField size = new JTextField(20);
        JTabbedPane tabbedPane = new JTabbedPane();

        panel = new JPanel(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipady = 10;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);

        panel.setLayout(new GridBagLayout());
        getContentPane().add(panel);

        panel.add(namelabel, gbc);
        gbc.gridx++;

        panel.add(name, gbc);
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;

        panel.add(sizelabel, gbc);
        gbc.gridx++;

        panel.add(size, gbc);
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;

        panel.add(reqlabel, gbc);
        gbc.gridx++;

        panel.add(reqid, gbc);
        gbc.gridy++;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridy++;

        panel.add(reqbutton, gbc);
        gbc.gridx++;

        panel.add(quitButton, gbc);

        File dir;
        dir = new File("C:\\Users\\educa\\Desktop\\Files");
        FileTableModel model = new FileTableModel(dir);

        JPanel panel2 = new JPanel(false);
        JTable table = new JTable(model);
        panel2.add(new JScrollPane(table), "Center");

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    JTable target = (JTable) me.getSource();
                    int row = target.getSelectedRow();
                    String msg = "Node: " + id + "\nName: " + table.getValueAt(row, 0).toString() + "\nSize: "
                            + table.getValueAt(row, 1).toString();

                    StringSelection stringSelection = new StringSelection(msg);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                }
            }
        });

        File dir2;
        dir2 = new File("C:\\Users\\educa\\Desktop\\Downloaded_Files");
        FileTableModel model2 = new FileTableModel(dir2);

        JPanel panel3 = new JPanel(false);
        JTable table2 = new JTable(model2);

        panel3.add(new JScrollPane(table2), "Center");

        tabbedPane.addTab("Request", panel);
        tabbedPane.addTab("Files", panel2);
        tabbedPane.addTab("Downloaded Files", panel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tabbedPane));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(tabbedPane));

        pack();
        Runnable helloRunnable = new Runnable() {
            public void run() {

                table.setModel(new FileTableModel(dir));
                table2.setModel(new FileTableModel(dir2));
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(helloRunnable, 0, 5, TimeUnit.SECONDS);
        setTitle("App");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try {
            socket = new Socket("localhost", 1981);
            out = new PrintWriter(socket.getOutputStream(), true);

            quitButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent event) {
                    System.exit(0);
                }
            });

            reqbutton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    out.println("requestfile");
                    out.println(name.getText());
                    out.println(size.getText());
                    out.println(reqid.getText());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        id = args[0];
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                App ex = new App();
                ex.setVisible(true);
            }
        });
    }

}

class FileTableModel extends AbstractTableModel {
    protected File dir;
    protected String[] filenames;

    protected String[] columnNames = new String[] { "name", "size", "last modified" };

    protected Class[] columnClasses = new Class[] { String.class, Long.class, Date.class, Boolean.class, Boolean.class,
            Boolean.class };

    public FileTableModel(File dir) {
        this.dir = dir;
        this.filenames = dir.list(); 
    }

    public int getColumnCount() {
        return 3;
    } 

    public int getRowCount() {
        return filenames.length;
    } 

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Class getColumnClass(int col) {
        return columnClasses[col];
    }

    public Object getValueAt(int row, int col) {
        File f = new File(dir, filenames[row]);
        switch (col) {
        case 0:
            return filenames[row];
        case 1:
            return f.length();
        case 2:
            return new Date(f.lastModified());
        default:
            return null;
        }
    }
}
