package com.student;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {

    static class Book {

        String title;
        String author;
        String url;

        Book(String title, String author, String url) {
            this.title = title;
            this.author = author;
            this.url = url;
        }
    }

    static final int MAX_BOOKS = 20;
    static final String TOP_URL = "https://www.gutenberg.org/browse/scores/top";
    static final String SEARCH_URL = "https://www.gutenberg.org/ebooks/search/?query=";

    static Book[] readingList = new Book[MAX_BOOKS];
    static int readingCount = 0;

    static Book[] currentResults = new Book[MAX_BOOKS];
    static int resultCount = 0;

    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice: ");

            if (choice == 1) {
                exploreBooks();
            } else if (choice == 2) {
                System.out.print("Enter title: ");
                String title = scanner.nextLine().trim();
                searchBook(title);
            } else if (choice == 3) {
                System.out.print("Enter author: ");
                String author = scanner.nextLine().trim();
                searchBook(author, true);
            } else if (choice == 4) {
                addBook();
            } else if (choice == 5) {
                viewReadingList();
            } else if (choice == 6) {
                removeBook();
            } else if (choice == 7) {
                displayBookDetails();
            } else if (choice == 0) {
                System.out.println("Goodbye! Happy reading.");
            } else {
                System.out.println("Invalid choice. Please enter a number from 0 to 7.");
            }
        } while (choice != 0);

        scanner.close();
    }

    static void printMenu() {
        System.out.println();
        System.out.println("===== GUTENBERG BOOK EXPLORER =====");
        System.out.println("1. Explore Books");
        System.out.println("2. Search by Title");
        System.out.println("3. Search by Author");
        System.out.println("4. Add Book to Reading List");
        System.out.println("5. View Reading List");
        System.out.println("6. Remove Book");
        System.out.println("7. Display Book Details");
        System.out.println("0. Exit");
    }

    static void exploreBooks() {
        try {
            System.out.println("Loading top books from Project Gutenberg...");
            Document doc = Jsoup.connect(TOP_URL).userAgent("Mozilla/5.0").timeout(15000).get();
            Elements links = doc.select("a[href^=/ebooks/]");

            resultCount = 0;
            for (Element a : links) {
                if (resultCount == MAX_BOOKS) {
                    break;
                }

                String href = a.attr("href");
                if (!href.matches("/ebooks/\\d+")) {
                    continue;
                }
                String url = a.absUrl("href");
                if (isInResults(url)) {
                    continue;
                }
                String text = a.text().trim().replaceAll("\\s*\\(\\d+\\)$", "");
                String title = text;
                String author = "Unknown";
                int idx = text.lastIndexOf(" by ");
                if (idx > 0) {
                    title = text.substring(0, idx);
                    author = text.substring(idx + 4);
                }
                currentResults[resultCount++] = new Book(title, author, url);
            }
            showResults("TOP BOOKS");
        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    static void searchBook(String title) {
        searchBook(title, false);
    }

    static void searchBook(String keyword, boolean searchByAuthor) {
        if (keyword.isEmpty()) {
            System.out.println("Search text cannot be empty.");
            return;
        }
        try {
            String query = (searchByAuthor ? "a." : "t.") + keyword;
            String url = SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8);
            System.out.println("Searching...");
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            Elements items = doc.select("li.booklink");

            resultCount = 0;
            for (Element item : items) {
                if (resultCount == MAX_BOOKS) {
                    break;
                }

                Element link = item.selectFirst("a.link");
                Element titleEl = item.selectFirst("span.title");
                Element authorEl = item.selectFirst("span.subtitle");
                if (link == null || titleEl == null) {
                    continue;
                }

                String bookUrl = link.absUrl("href");
                String author = (authorEl != null) ? authorEl.text().trim() : "Unknown";
                currentResults[resultCount++] = new Book(titleEl.text().trim(), author, bookUrl);
            }

            if (resultCount == 0) {
                System.out.println("No results found for \"" + keyword + "\".");
            } else {
                showResults("SEARCH RESULTS");
            }
        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    static void addBook() {
        if (resultCount == 0) {
            System.out.println("No books to choose from. Use Explore (1) or Search (2/3) first.");
            return;
        }
        if (readingCount == MAX_BOOKS) {
            System.out.println("Your reading list is full (" + MAX_BOOKS + " books). Remove one first.");
            return;
        }
        showResults("CHOOSE A BOOK");
        int n = readInt("Enter the book number to add (0 to cancel): ");
        if (n == 0) {
            return;
        }
        if (n < 1 || n > resultCount) {
            System.out.println("Invalid number.");
            return;
        }
        Book chosen = currentResults[n - 1];

        for (int i = 0; i < readingCount; i++) {
            if (readingList[i].url.equals(chosen.url)) {
                System.out.println("This book is already in your reading list.");
                return;
            }
        }
        readingList[readingCount++] = chosen;
        System.out.println("Added: " + chosen.title);
    }

    static void viewReadingList() {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }
        System.out.println("--- YOUR READING LIST (" + readingCount + "/" + MAX_BOOKS + ") ---");
        for (int i = 0; i < readingCount; i++) {
            System.out.println((i + 1) + ". " + readingList[i].title + " - " + readingList[i].author);
        }
    }

    static void removeBook() {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty. Nothing to remove.");
            return;
        }
        viewReadingList();
        int n = readInt("Enter the book number to remove (0 to cancel): ");
        if (n == 0) {
            return;
        }
        if (n < 1 || n > readingCount) {
            System.out.println("Invalid number.");
            return;
        }
        String removed = readingList[n - 1].title;
        for (int i = n - 1; i < readingCount - 1; i++) {
            readingList[i] = readingList[i + 1];
        }
        readingList[--readingCount] = null;
        System.out.println("Removed: " + removed);
    }

    static void displayBookDetails() {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty. Add a book first.");
            return;
        }
        viewReadingList();
        int n = readInt("Enter the book number to view (0 to cancel): ");
        if (n == 0) {
            return;
        }
        if (n < 1 || n > readingCount) {
            System.out.println("Invalid number.");
            return;
        }
        Book b = readingList[n - 1];
        System.out.println("--------------------------------");
        System.out.println("Title : " + b.title);
        System.out.println("Author: " + b.author);
        System.out.println("URL   : " + b.url);
        System.out.println("--------------------------------");
    }

    static void showResults(String header) {
        System.out.println("--- " + header + " ---");
        for (int i = 0; i < resultCount; i++) {
            System.out.println((i + 1) + ". " + currentResults[i].title
                    + " - " + currentResults[i].author);
        }
    }

    static boolean isInResults(String url) {
        for (int i = 0; i < resultCount; i++) {
            if (currentResults[i].url.equals(url)) {
                return true;
            }
        }
        return false;
    }

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
