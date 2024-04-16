import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//Message class
class Message {
    private String sender;
    private List<String> recipients;
    private String content;
    private LocalDateTime timestamp;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Message(String sender, List<String> recipients, String content) {
        this.sender = sender;
        this.recipients = recipients;
        this.content = content;
        this.timestamp = LocalDateTime.now(); // Use LocalDateTime for local time
    }
    public String getSender() {
        return sender;
    }
    public List<String> getRecipients() {
        return recipients;
    }
    public String getContent() {
        return content;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    // Format timestamp to a human-readable format
    public String getFormattedTimestamp() {
        return timestamp.format(formatter);
    }
}

class User implements IterableByUser {
    private String username;
    private ChatServer chatServer;
    private ChatHistory chatHistory;

    public User(String username, ChatServer chatServer) {
        this.username = username;
        this.chatServer = chatServer;
        this.chatHistory = new ChatHistory();
        chatServer.registerUser(this);
    }
    public void sendMessage(List<String> recipients, String content) {
        chatServer.sendMessage(this, recipients, content);
    }
    public void receiveMessage(Message message) {
        chatHistory.addMessage(message);
        System.out.println(username + " received message from " + message.getSender() + " at " +
                message.getTimestamp() + ": " + message.getContent());
    }
    public void undoLastMessage() {
        Message lastMessage = chatHistory.getLastMessage();
        if (lastMessage != null) {
            chatServer.undoMessage(this, lastMessage);
            chatHistory.removeLastMessage();
            System.out.println(username + " undid the last message.");
        } else {
            System.out.println(username + " has no message to undo.");
        }
    }
    public void blockUser(String username) {
        chatServer.blockUser(this, username);
        System.out.println(username + " blocked messages from " + username);
    }
    public String getUsername() {
        return username;
    }
    @Override
    public Iterator iterator(User userToSearchWith) {
        return chatHistory.iterator(userToSearchWith);
    }
}

class ChatServer {
    private Map<String, User> users = new HashMap<>();
    private Map<String, List<String>> blockedUsers = new HashMap<>();

    public void registerUser(User user) {
        users.put(user.getUsername(), user);
    }
    public void sendMessage(User sender, List<String> recipients, String content) {
        Message message = new Message(sender.getUsername(), recipients, content);
        for (String recipient : recipients) {
            if (!isBlocked(recipient, sender.getUsername())) {
                users.get(recipient).receiveMessage(message);
            }
        }
    }
    public void undoMessage(User user, Message message) {
        List<String> recipients = new ArrayList<>(message.getRecipients());
        recipients.remove(user.getUsername());
        sendMessage(user, recipients, "Undo: " + message.getContent());
    }
    public void blockUser(User user, String username) {
        if (!blockedUsers.containsKey(user.getUsername())) {
            blockedUsers.put(user.getUsername(), new ArrayList<>());
        }
        blockedUsers.get(user.getUsername()).add(username);
    }
    private boolean isBlocked(String recipient, String sender) {
        return blockedUsers.containsKey(recipient) && blockedUsers.get(recipient).contains(sender);
    }
}

// ChatHistory class
class ChatHistory implements IterableByUser {
    private List<Message> messages = new ArrayList<>();

    public void addMessage(Message message) {
        messages.add(message);
    }
    public Message getLastMessage() {
        if (!messages.isEmpty()) {
            return messages.get(messages.size() - 1);
        }
        return null;
    }
    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            messages.remove(messages.size() - 1);
        }
    }
    @Override
    public Iterator iterator(User userToSearchWith) {
        return new SearchMessagesByUserIterator(userToSearchWith, messages);
    }
}

interface IterableByUser {
    Iterator iterator(User userToSearchWith);
}

class SearchMessagesByUserIterator implements Iterator<Message> {
    private User userToSearchWith;
    private List<Message> messages;
    private int currentIndex;
    public SearchMessagesByUserIterator(User userToSearchWith, List<Message> messages) {
        this.userToSearchWith = userToSearchWith;
        this.messages = messages;
        this.currentIndex = 0;
    }
    @Override
    public boolean hasNext() {
        while (currentIndex < messages.size()) {
            Message message = messages.get(currentIndex);
            if (message.getSender().equals(userToSearchWith.getUsername()) || message.getRecipients().contains(userToSearchWith.getUsername())) {
                return true;
            }
            currentIndex++;
        }
        return false;
    }
    @Override
    public Message next() {
        return messages.get(currentIndex++);
    }
}

public class Main {
    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        User user1 = new User("Alice", chatServer);
        User user2 = new User("Bob", chatServer);
        User user3 = new User("Charlie", chatServer);

        //Sending messages
        user1.sendMessage(List.of("Bob", "Charlie"), "Hello Bob and Charlie!");
        user2.sendMessage(List.of("Alice"), "Hi Alice!");
        user3.sendMessage(List.of("Bob"), "Hey Bob!");

        //deletes messages
        user1.undoLastMessage();

        //blocking
        user2.blockUser("Alice");

        //message but blocked
        user1.sendMessage(List.of("Bob"), "This message won't reach Bob!");

        //chat history
        System.out.println("Charlie's chat history:");
        for (Iterator<Message> iterator = user3.iterator(user1); iterator.hasNext();) {
            Message message = iterator.next();
            System.out.println("From: " + message.getSender() + ", Content: " + message.getContent());
        }
    }
}
