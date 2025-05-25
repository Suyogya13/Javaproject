import java.io.*;
import java.util.*;

public class Guvi{

    // === Model Classes ===
    // Layered architecture to perform CRUD operation
    static class Person {
        protected int id;
        protected String name;

        public Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class User extends Person {
        private String password;
        private List<Book> borrowedBooks;

        public User(int id, String name, String password) {
            super(id, name);
            this.password = password;
            this.borrowedBooks = new ArrayList<>();
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public boolean checkPassword(String password) { return this.password.equals(password); }
        public void borrowBook(Book book) { borrowedBooks.add(book); }
        public void returnBook(Book book) { borrowedBooks.remove(book); }
        public List<Book> getBorrowedBooks() { return borrowedBooks; }
    }

    static class Book {
        private int id;
        private String title;
        private String author;
        private boolean isIssued;

        public Book(int id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.isIssued = false;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public boolean isIssued() { return isIssued; }
        public void setIssued(boolean issued) { this.isIssued = issued; }
        public void showInfo() {
            System.out.println(id + ": " + title + " by " + author + (isIssued ? " (Issued)" : ""));
        }
    }

    // === DAO Classes ===
    static class BookDAO {
        private static final String FILE_NAME = "books.txt";

        // Implement IO file connectivity from Java
        // Auto-create files to perform CRUD
        public static List<Book> loadBooks() {
            List<Book> books = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String author = parts[2];
                    boolean isIssued = Boolean.parseBoolean(parts[3]);
                    Book book = new Book(id, title, author);
                    book.setIssued(isIssued);
                    books.add(book);
                }
            } catch (IOException e) {
                System.out.println("No existing book file found. Starting fresh.");
            }
            return books;
        }

        public static void saveBooks(List<Book> books) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
                for (Book book : books) {
                    pw.println(book.getId() + "," + book.getTitle() + "," + book.getAuthor() + "," + book.isIssued());
                }
            } catch (IOException e) {
                System.out.println("Error saving books: " + e.getMessage());
            }
        }
    }

    static class UserDAO {
        private static final String FILE_NAME = "users.txt";

        public static List<User> loadUsers() {
            List<User> users = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String password = parts[2];
                    users.add(new User(id, name, password));
                }
            } catch (IOException e) {
                System.out.println("No existing user file found. Starting fresh.");
            }
            return users;
        }

        public static void saveUsers(List<User> users) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
                for (User user : users) {
                    pw.println(user.getId() + "," + user.getName() + "," + user.password);
                }
            } catch (IOException e) {
                System.out.println("Error saving users: " + e.getMessage());
            }
        }
    }

    // === Core Library System ===
    static class LibrarySystem {
        List<Book> books;
        List<User> users;
        User currentUser;
        int nextBookId = 1;

        public LibrarySystem() {
            books = BookDAO.loadBooks();
            users = UserDAO.loadUsers();
            for (Book b : books) if (b.getId() >= nextBookId) nextBookId = b.getId() + 1;
        }

        public void registerUser(Scanner sc) {
            System.out.print("Enter new user ID: ");
            int newId = sc.nextInt(); sc.nextLine();
            System.out.print("Enter your name: ");
            String name = sc.nextLine();
            System.out.print("Enter a password: ");
            String password = sc.nextLine();

            // Input validation
            for (User user : users) if (user.getId() == newId) {
                System.out.println("User ID already exists."); return; }

            users.add(new User(newId, name, password));
            UserDAO.saveUsers(users);
            System.out.println("Registration successful.");
        }

        public void login(Scanner sc) {
            System.out.print("Enter user ID: ");
            int userId = sc.nextInt(); sc.nextLine();
            System.out.print("Enter password: ");
            String pwd = sc.nextLine();
            for (User user : users) if (user.getId() == userId && user.checkPassword(pwd)) {
                currentUser = user;
                System.out.println("Login successful. Welcome, " + user.getName());
                return;
            }
            System.out.println("Invalid credentials."); // Error messages and feedback
        }

        public void searchBook(Scanner sc) {
            sc.nextLine();
            System.out.print("Enter book title or author to search: ");
            String query = sc.nextLine().toLowerCase();
            boolean found = false;
            for (Book book : books) {
                if (book.getTitle().toLowerCase().contains(query) ||
                    book.getAuthor().toLowerCase().contains(query)) {
                    book.showInfo(); found = true;
                }
            }
            if (!found) System.out.println("No book found.");
        }

        public void issueBook(Scanner sc) {
            if (currentUser == null) {
                System.out.println("Login first."); return;
            }
            sc.nextLine();
            System.out.print("Enter book title to issue: ");
            String title = sc.nextLine();
            for (Book book : books) if (book.getTitle().equalsIgnoreCase(title)) {
                if (!book.isIssued()) {
                    book.setIssued(true);
                    currentUser.borrowBook(book);
                    BookDAO.saveBooks(books);
                    System.out.println("Book issued.");
                } else System.out.println("Book is already issued.");
                return;
            }
            System.out.print("Book not found. Add and issue it? (yes/no): ");
            if (sc.nextLine().equalsIgnoreCase("yes")) {
                System.out.print("Enter author: ");
                String author = sc.nextLine();
                Book newBook = new Book(nextBookId++, title, author);
                newBook.setIssued(true);
                books.add(newBook);
                currentUser.borrowBook(newBook);
                BookDAO.saveBooks(books);
                System.out.println("Book added and issued.");
            }
        }

        public void returnBook(Scanner sc) {
            if (currentUser == null) {
                System.out.println("Login first."); return;
            }
            sc.nextLine();
            System.out.print("Enter book title to return: ");
            String title = sc.nextLine();
            for (Book book : currentUser.getBorrowedBooks()) {
                if (book.getTitle().equalsIgnoreCase(title)) {
                    book.setIssued(false);
                    currentUser.returnBook(book);
                    BookDAO.saveBooks(books);
                    System.out.println("Book returned.");
                    return;
                }
            }
            System.out.println("Book not in your list.");
        }

        public void generateReport() {
            if (currentUser == null) {
                System.out.println("Login first."); return;
            }

            // Accuracy of Output
            System.out.println("=== Report for " + currentUser.getName() + " ===");
            if (!currentUser.getBorrowedBooks().isEmpty()) {
                for (Book b : currentUser.getBorrowedBooks()) System.out.println("- " + b.getTitle());
            } else System.out.println("No books borrowed.");
        }

        public void addBook(Scanner sc) {
            if (currentUser == null) {
                System.out.println("Login first."); return;
            }
            sc.nextLine();
            System.out.print("Enter book title: ");
            String title = sc.nextLine();
            System.out.print("Enter author: ");
            String author = sc.nextLine();
            books.add(new Book(nextBookId++, title, author));
            BookDAO.saveBooks(books);
            System.out.println("Book added.");
        }
    }

    // === Main Method ===
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        LibrarySystem lib = new LibrarySystem();
        int choice = -1;

        System.out.println("=== GUVI Library Management System ===");

        do {
            System.out.println("\nMenu:");
            System.out.println("0. Register");
            System.out.println("1. Login");
            System.out.println("2. Search Book");
            System.out.println("3. Issue Book");
            System.out.println("4. Return Book");
            System.out.println("5. Generate Report");
            System.out.println("6. Add Book");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                System.out.println("Invalid input.");
                scanner.next(); // Consume bad input
                continue;
            }

            switch (choice) {
                case 0 -> lib.registerUser(scanner);
                case 1 -> lib.login(scanner);
                case 2 -> lib.searchBook(scanner);
                case 3 -> lib.issueBook(scanner);
                case 4 -> lib.returnBook(scanner);
                case 5 -> lib.generateReport();
                case 6 -> lib.addBook(scanner);
                case 7 -> {
                    BookDAO.saveBooks(lib.books);
                    UserDAO.saveUsers(lib.users);
                    System.out.println("Exiting...");
                }
                default -> System.out.println("Invalid option.");
            }

        } while (choice != 7);

        scanner.close();
    }
}
