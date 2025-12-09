
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class Client extends Application {

    private static final String SERVER_HOST = "localhost"; // Change to your server IP if needed
    private static final int SERVER_PORT = 44444;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button connectButton;
    private Button disconnectButton;

    private Thread listenerThread;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Chat Client");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Chat display area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(300);
        chatArea.getStyleClass().add("chat-area");

        VBox centerBox = new VBox(chatArea);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // Message input and send button
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        messageField = new TextField();
        messageField.setPromptText("Type your message...");
        messageField.setPrefWidth(400);
        messageField.setDisable(true);
        messageField.getStyleClass().add("message-input");

        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.getStyleClass().add("action-button");
        sendButton.setOnAction(e -> sendMessage());

        messageBox.getChildren().addAll(messageField, sendButton);

        // Connect / Disconnect buttons
        HBox connectionBox = new HBox(15);
        connectionBox.setAlignment(Pos.CENTER);

        connectButton = new Button("Connect");
        connectButton.getStyleClass().add("action-button");
        connectButton.setOnAction(e -> connectToServer());

        disconnectButton = new Button("Disconnect");
        disconnectButton.setDisable(true);
        disconnectButton.getStyleClass().add("action-button");
        disconnectButton.setOnAction(e -> disconnectFromServer());

        connectionBox.getChildren().addAll(connectButton, disconnectButton);

        Label controlLabel = new Label("Client Control");
        controlLabel.getStyleClass().add("label");

        VBox topBox = new VBox(10, controlLabel, connectionBox);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10));

        root.setTop(topBox);
        root.setCenter(centerBox);
        root.setBottom(messageBox);

        // Inline CSS styling
        String inlineCss = """
            .root {
                -fx-background-color: #F8F8F8;
            }
            .label {
                -fx-text-fill: #2c3e50;
                -fx-font-size: 18px;
                -fx-font-weight: bold;
            }
            .chat-area {
                -fx-font-family: 'Consolas', 'Monospaced', monospace;
                -fx-font-size: 13px;
                -fx-text-fill: #1a1a1a;
                -fx-control-inner-background: #ADD8E6;
                -fx-border-color: #34495e;
                -fx-border-width: 1px;
                -fx-background-radius: 5px;
                -fx-padding: 8px;
            }
            .message-input {
                -fx-prompt-text-fill: #7f8c8d;
                -fx-background-radius: 5px;
                -fx-border-color: #34495e;
                -fx-border-radius: 5px;
                -fx-padding: 8px;
            }
            .action-button {
                -fx-background-color: #27ae60;
                -fx-text-fill: white;
                -fx-font-size: 15px;
                -fx-padding: 10px 20px;
                -fx-background-radius: 6px;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);
            }
            .action-button:hover {
                -fx-background-color: #2ecc71;
            }
            .action-button:disabled {
                -fx-background-color: #7f8c8d;
                -fx-opacity: 0.7;
                -fx-effect: none;
                -fx-cursor: default;
            }
            """;

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("data:text/css," + inlineCss);
        root.getStyleClass().add("root");

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            disconnectFromServer();
            Platform.exit();
        });

        primaryStage.show();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            chatArea.clear();
            logMessage("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);

            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            updateControls(true);
        } catch (IOException e) {
            logMessage("Failed to connect: " );
        }
    }

    private void disconnectFromServer() {
        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (out != null) {
                out.println("[CLIENT] Leaving chat...");
                out.flush();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logMessage("Disconnected from server.");
        } catch (IOException e) {
            logMessage("Error during disconnect: ");
        } finally {
            updateControls(false);
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (!Thread.currentThread().isInterrupted() && (message = in.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> logMessage(finalMessage));
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                Platform.runLater(() -> logMessage("Connection lost: " + e.getMessage()));
            }
        } finally {
            Platform.runLater(() -> updateControls(false));
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            logMessage( message);
            messageField.clear();
        }
    }

    private void logMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    private void updateControls(boolean connected) {
        connectButton.setDisable(connected);
        disconnectButton.setDisable(!connected);
        sendButton.setDisable(!connected);
        messageField.setDisable(!connected);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
