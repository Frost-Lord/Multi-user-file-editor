package main;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FileEditorPanel extends JPanel implements Runnable {

    private JTextArea textArea;
    private boolean isPrivateSession;

    public FileEditorPanel(boolean isPrivateSession) {
        this.isPrivateSession = isPrivateSession;

        textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        add(scrollPane);

        if (isPrivateSession) {
            new Thread(new PrivateClient()).start();
        } else {
            new Thread(new SharedClient()).start();
        }
    }

    public void run() {

    }

    private class PrivateClient implements Runnable {
        public void run() {
            try {
                String ipAddress = JOptionPane.showInputDialog(FileEditorPanel.this,
                        "Enter the IP address of the host:");
                try (Socket socket = new Socket(ipAddress, 9000)) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        if ("PING".equals(line)) {
                            System.out.println("Received PING from private session");
                            continue;
                        }
                        String[] parts = line.split(",", 2);
                        int lineNumber = Integer.parseInt(parts[0]);
                        String text = parts[1];
                        SwingUtilities.invokeLater(() -> {
                            try {
                                if (lineNumber < textArea.getLineCount()) {
                                    textArea.replaceRange(text, textArea.getLineStartOffset(lineNumber),
                                            textArea.getLineEndOffset(lineNumber));
                                } else {
                                    textArea.append("\n" + text);
                                }
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SharedClient implements Runnable {
        private Timer timer = new Timer();
        private long lastUpdate = System.currentTimeMillis();
    
        public void run() {
            try {
                Socket socket = null;
                while (socket == null) {
                    try {
                        socket = new Socket("localhost", 9000);
                    } catch (ConnectException e) {
                        // Server not running, start a new one
                        new Thread(new Server(textArea)).start();
                        Thread.sleep(500); // Wait for server to start
                    }
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                int numLines = Integer.parseInt(in.readLine());
                String[] lines = textArea.getText().split("\n");
                for (int i = 0; i < lines.length; i++) {
                    out.println("UPDATE," + i + "," + (i + 1));
                    out.println(lines[i]);
                }
    
                new Thread(() -> {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            String finalLine = line;
                            SwingUtilities.invokeLater(() -> {
                                textArea.append(finalLine + "\n");
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
    
                // Start timer to send updates to server
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            String[] lines = textArea.getText().split("\n");
                            List<Integer> modifiedLines = new ArrayList<>();
                            for (int i = 0; i < lines.length; i++) {
                                if (!lines[i].equals(textArea.getDocument().getText(textArea.getLineStartOffset(i),
                                        textArea.getLineEndOffset(i) - textArea.getLineStartOffset(i)))) {
                                    modifiedLines.add(i);
                                    out.println("UPDATE," + i + "," + (i + 1));
                                    out.println(lines[i]);
                                }
                            }
                            if (!modifiedLines.isEmpty()) {
                                lastUpdate = System.currentTimeMillis();
                            } else if (System.currentTimeMillis() - lastUpdate > 1000) {
                                out.println("PING");
                            }
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000, 1000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}    