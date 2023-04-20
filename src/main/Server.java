package main;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public class Server implements Runnable {

    private final Map<Socket, ClientData> clients = Collections.synchronizedMap(new HashMap<>());
    private final JTextArea textArea;

    private static class ClientData {
        private PrintWriter out;
        private List<Integer> modifiedLines;
    
        public ClientData(PrintWriter out) {
            this.out = out;
            this.modifiedLines = new ArrayList<>();
        }
    }

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
                System.out.println("User " + clientSocket.getInetAddress().getHostAddress() + " has joined the session");
                clients.put(clientSocket, new ClientData(new PrintWriter(clientSocket.getOutputStream(), true)));
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final PrintWriter out;
    
        public ClientHandler(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            ClientData clientData = clients.get(clientSocket);
            this.out = clientData.out;
        }
    
        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("UPDATE")) {
                        String[] updateInfo = line.split(",");
                        System.out.println("Received UPDATE command: " + line); // Add this line to print the received UPDATE command
                        if (updateInfo.length >= 4) {
                            int startLine = Integer.parseInt(updateInfo[1]);
                            int endLine = Integer.parseInt(updateInfo[2]);
                            boolean isInsert = Boolean.parseBoolean(updateInfo[3]);
                            String updatedText = in.readLine();
        
                            synchronized (textArea) {
                                int startOffset = textArea.getLineStartOffset(startLine);
                                int endOffset = isInsert ? startOffset : textArea.getLineEndOffset(endLine) - 1;
                                textArea.replaceRange(updatedText, startOffset, endOffset);
                            }
        
                            synchronized (clients) {
                                for (Map.Entry<Socket, ClientData> entry : clients.entrySet()) {
                                    if (entry.getKey() != clientSocket) {
                                        PrintWriter otherOut = entry.getValue().out;
                                        otherOut.println(line);
                                        otherOut.println(updatedText);
                                    }
                                }
                            }
                        } else {
                            System.err.println("Invalid UPDATE command received.");
                        }
                    }
                }
            } catch (IOException | BadLocationException e) {
                e.printStackTrace();
            } finally {
                clients.remove(clientSocket);
                System.out.println("User " + clientSocket.getInetAddress().getHostAddress() + " has left the session");
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }        
    }    
}
