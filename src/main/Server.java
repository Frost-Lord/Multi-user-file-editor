package main;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JTextArea;

public class Server implements Runnable {

    private Map<Socket, List<Integer>> modifiedLines = new HashMap<>();
    private JTextArea textArea;

    public Server(JTextArea textArea) {
        this.textArea = textArea;
    }

    public void run() {
        try {
            ServerSocket serverSocket = null;
            while (serverSocket == null) {
                try {
                    serverSocket = new ServerSocket(9000);
                } catch (BindException e) {
                    System.out.println("Port 9000 is already in use. Trying the next port...");
                    continue;
                }
            }
            System.out.println("Server started on port " + serverSocket.getLocalPort());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                modifiedLines.put(clientSocket, new ArrayList<>());
                System.out.println("User " + clientSocket.getInetAddress().getHostAddress() + " has joined the session");
                new Thread(new ClientHandler(clientSocket, textArea.getLineCount())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private int numLines;
    
        public ClientHandler(Socket clientSocket, int numLines) {
            this.clientSocket = clientSocket;
            this.numLines = numLines;
        }
    
        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(numLines);
    
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("UPDATE")) {
                        int startLine = Integer.parseInt(line.split(",")[1]);
                        int endLine = Integer.parseInt(line.split(",")[2]);
                        String[] lines = textArea.getText().split("\n");
                        StringBuilder sb = new StringBuilder();
                        for (int i = startLine; i < endLine && i < lines.length; i++) {
                            sb.append(lines[i]).append("\n");
                        }
                        out.println(sb.toString());
                    } else if ("PING".equals(line)) {
                        System.out.println("Received PING from " + clientSocket.getInetAddress().getHostAddress());
                    } else {
                        synchronized (textArea) {
                            textArea.append(line + "\n");
                            numLines++;
                        }
                        for (Socket client : modifiedLines.keySet()) {
                            if (client != clientSocket) {
                                PrintWriter out2 = new PrintWriter(client.getOutputStream(), true);
                                out2.println(line);
                            }
                        }
                    }
                }
                modifiedLines.remove(clientSocket);
                System.out.println("User " + clientSocket.getInetAddress().getHostAddress() + " has left the session");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }    
}
