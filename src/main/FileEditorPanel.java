package main;

import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FileEditorPanel extends JPanel {

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
        private PrintWriter out;
        public void run() {
            try {
                textArea.getDocument().addDocumentListener(new TextAreaDocumentListener());
    
                Socket socket = null;
                while (socket == null) {
                    try {
                        socket = new Socket("localhost", 9000);
                    } catch (ConnectException e) {
                        new Thread(new Server(textArea)).start();
                        Thread.sleep(500);
                    }
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true); 
    
                new Thread(() -> {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            if (line.startsWith("UPDATE")) {
                                String[] updateInfo = line.split(",");
                                int startLine = Integer.parseInt(updateInfo[1]);
                                boolean isInsert = Boolean.parseBoolean(updateInfo[2]);
                                String updatedText = in.readLine();
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        int startOffset = textArea.getLineStartOffset(startLine);
                                        int endOffset = isInsert ? startOffset : textArea.getLineEndOffset(startLine);
                                        textArea.getDocument().removeDocumentListener(new TextAreaDocumentListener());
                                        textArea.replaceRange(updatedText, startOffset, endOffset);
                                        textArea.getDocument().addDocumentListener(new TextAreaDocumentListener());
                                    } catch (BadLocationException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private class TextAreaDocumentListener implements DocumentListener {

            @Override
            public void insertUpdate(DocumentEvent e) {
                sendUpdate(e.getOffset(), e.getLength(), true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sendUpdate(e.getOffset(), e.getLength(), false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void sendUpdate(int offset, int length, boolean isInsert) {
                try {
                    int startLine = textArea.getLineOfOffset(offset);
                    String updatedText = isInsert ? textArea.getDocument().getText(offset, length) : "";
                    System.out.println("UPDATE," + startLine + "," + isInsert);
                    System.out.println(updatedText);
                    out.println("UPDATE," + startLine + "," + isInsert);
                    out.println(updatedText);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    private class Server implements Runnable {
        JTextArea textArea;

        public Server(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void run() {
            // Your Server implementation
        }
    }

}