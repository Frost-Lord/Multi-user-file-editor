package main;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("File Editor");

        boolean isPrivateSession = JOptionPane.showOptionDialog(window,
                "Would you like to start a private session or join a shared session?",
                "Choose an option",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Private session", "Shared session"},
                "Private session") == JOptionPane.YES_OPTION;

        FileEditorPanel fileEditorPanel = new FileEditorPanel(isPrivateSession);
        window.add(fileEditorPanel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
        window.setSize(1280, 720);
    }
}
