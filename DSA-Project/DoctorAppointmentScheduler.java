import java.util.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DoctorAppointmentScheduler {

    static class Appointment {
        String id;
        String patient;
        LocalTime time;

        Appointment(String id, String patient, LocalTime time) {
            this.id = id;
            this.patient = patient;
            this.time = time;
        }
    }

    static class Node {
        Appointment appointment;
        Node left, right;

        Node(Appointment appointment) {
            this.appointment = appointment;
        }
    }

    static class BookingRequest {
        String patient;
        LocalTime time;

        BookingRequest(String patient, LocalTime time) {
            this.patient = patient;
            this.time = time;
        }
    }

    static Node root;
    static Queue<BookingRequest> pendingQueue = new LinkedList<>();
    static int nextId = 101;

    static final String FILE_NAME = "appointments.txt";
    static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    static Node insert(Node node, Appointment appointment) {
        if (node == null)
            return new Node(appointment);

        if (appointment.time.isBefore(node.appointment.time))
            node.left = insert(node.left, appointment);
        else if (appointment.time.isAfter(node.appointment.time))
            node.right = insert(node.right, appointment);

        return node;
    }

    static Node searchTime(Node node, LocalTime time) {
        if (node == null || node.appointment.time.equals(time))
            return node;

        if (time.isBefore(node.appointment.time))
            return searchTime(node.left, time);

        return searchTime(node.right, time);
    }

    static boolean hasOverlap(LocalTime time) {
        Node current = root;

        while (current != null) {
            LocalTime existing = current.appointment.time;

            if (time.isBefore(existing.plusMinutes(30))
                    && existing.isBefore(time.plusMinutes(30)))
                return true;

            if (time.isBefore(existing))
                current = current.left;
            else
                current = current.right;
        }

        return false;
    }

    static LocalTime earliestSlot(LocalTime start) {
        LocalTime slot = start;

        while (hasOverlap(slot)) {
            slot = slot.plusMinutes(30);
        }

        return slot;
    }

    static String generateId() {
        return "A" + nextId++;
    }

    static void bookAppointment(String patient, LocalTime time) {
        if (patient == null || patient.trim().isEmpty()) {
            System.out.println("Invalid patient name.");
            return;
        }

        if (time == null) {
            System.out.println("Invalid appointment time.");
            return;
        }

        if (hasOverlap(time)) {
            System.out.println("Time slot unavailable. Appointment overlaps.");
            return;
        }

        Appointment appointment =
                new Appointment(generateId(), patient.trim(), time);

        root = insert(root, appointment);

        System.out.println("Appointment booked: "
                + appointment.id + " | "
                + appointment.patient + " | "
                + appointment.time.format(FORMAT));
    }

    static Appointment searchId(Node node, String id) {
        if (node == null)
            return null;

        if (node.appointment.id.equalsIgnoreCase(id))
            return node.appointment;

        Appointment found = searchId(node.left, id);

        if (found != null)
            return found;

        return searchId(node.right, id);
    }

    static Node findMinimum(Node node) {
        while (node.left != null)
            node = node.left;

        return node;
    }

    static Node delete(Node node, LocalTime time) {
        if (node == null)
            return null;

        if (time.isBefore(node.appointment.time)) {
            node.left = delete(node.left, time);
        } else if (time.isAfter(node.appointment.time)) {
            node.right = delete(node.right, time);
        } else {
            if (node.left == null)
                return node.right;

            if (node.right == null)
                return node.left;

            Node min = findMinimum(node.right);
            node.appointment = min.appointment;
            node.right = delete(node.right, min.appointment.time);
        }

        return node;
    }

    static void display(Node node) {
        if (node == null)
            return;

        display(node.left);

        System.out.println(node.appointment.id + " | "
                + node.appointment.patient + " | "
                + node.appointment.time.format(FORMAT));

        display(node.right);
    }

    static void saveToFile(Node node, PrintWriter writer) {
        if (node == null)
            return;

        saveToFile(node.left, writer);

        writer.println(node.appointment.id + ","
                + node.appointment.patient + ","
                + node.appointment.time);

        saveToFile(node.right, writer);
    }

    static void saveAppointments() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            saveToFile(root, writer);
        } catch (IOException e) {
            System.out.println("Error saving appointments.");
        }
    }

    static void loadAppointments() {
        File file = new File(FILE_NAME);

        if (!file.exists())
            return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", 3);

                if (data.length != 3)
                    continue;

                Appointment appointment = new Appointment(
                        data[0],
                        data[1],
                        LocalTime.parse(data[2])
                );

                if (!hasOverlap(appointment.time)) {
                    root = insert(root, appointment);

                    try {
                        nextId = Math.max(nextId,
                                Integer.parseInt(
                                        appointment.id.substring(1)) + 1);
                    } catch (NumberFormatException e) {
                        // Keep the current ID counter.
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading appointment file.");
        }
    }

    static LocalTime readTime(Scanner sc) {
        String input = sc.nextLine().trim();

        if (input.isEmpty())
            return null;

        try {
            return LocalTime.parse(input,
                    DateTimeFormatter.ofPattern("h:mm a",
                            Locale.ENGLISH));
        } catch (Exception e) {
            try {
                return LocalTime.parse(input);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    static void cancelAppointment(String id) {
        Appointment appointment = searchId(root, id);

        if (appointment == null) {
            System.out.println("Appointment " + id + " not found.");
            return;
        }

        root = delete(root, appointment.time);
        System.out.println("Appointment cancelled: " + id);
    }

    static void processQueue() {
        if (pendingQueue.isEmpty()) {
            System.out.println("No pending requests.");
            return;
        }

        while (!pendingQueue.isEmpty()) {
            BookingRequest request = pendingQueue.poll();
            LocalTime slot = earliestSlot(request.time);
            bookAppointment(request.patient, slot);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        loadAppointments();

        while (true) {
            System.out.println("\n===== DOCTOR APPOINTMENT SCHEDULER =====");
            System.out.println("1. Book Appointment");
            System.out.println("2. Add Pending Request");
            System.out.println("3. Process Pending Requests");
            System.out.println("4. Search Appointment");
            System.out.println("5. Cancel Appointment");
            System.out.println("6. Display Appointments");
            System.out.println("7. Find Earliest Available Slot");
            System.out.println("8. Exit");
            System.out.print("Enter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter patient name: ");
                    String name = sc.nextLine();

                    System.out.print("Enter appointment time (10:00 AM): ");
                    LocalTime time = readTime(sc);

                    if (time == null) {
                        System.out.println("Invalid time.");
                    } else {
                        bookAppointment(name, time);
                    }
                    break;

                case "2":
                    System.out.print("Enter patient name: ");
                    String patient = sc.nextLine();

                    System.out.print("Enter requested time (09:00 AM): ");
                    LocalTime requestedTime = readTime(sc);

                    if (requestedTime == null || patient.trim().isEmpty()) {
                        System.out.println("Invalid request.");
                    } else {
                        pendingQueue.add(
                                new BookingRequest(patient, requestedTime));
                        System.out.println("Request added to queue.");
                    }
                    break;

                case "3":
                    processQueue();
                    break;

                case "4":
                    System.out.print("Enter appointment ID: ");
                    String id = sc.nextLine().trim();

                    Appointment result = searchId(root, id);

                    if (result == null) {
                        System.out.println("Appointment " + id + " not found.");
                    } else {
                        System.out.println("Appointment found: "
                                + result.id + " | "
                                + result.patient + " | "
                                + result.time.format(FORMAT));
                    }
                    break;

                case "5":
                    System.out.print("Enter appointment ID: ");
                    cancelAppointment(sc.nextLine().trim());
                    break;

                case "6":
                    if (root == null) {
                        System.out.println("No appointments available.");
                    } else {
                        System.out.println("\nScheduled Appointments:");
                        display(root);
                    }
                    break;

                case "7":
                    System.out.print(
                            "Enter starting time (press Enter for 09:00 AM): ");
                    LocalTime start = readTime(sc);

                    if (start == null && !hasOverlap(LocalTime.of(9, 0))) {
                        System.out.println("Earliest available slot: 09:00 AM");
                    } else if (start == null) {
                        LocalTime slot = earliestSlot(LocalTime.of(9, 0));
                        System.out.println("Earliest available slot: "
                                + slot.format(FORMAT));
                    } else {
                        LocalTime slot = earliestSlot(start);
                        System.out.println("Earliest available slot: "
                                + slot.format(FORMAT));
                    }
                    break;

                case "8":
                    saveAppointments();
                    System.out.println("Appointments saved successfully.");
                    System.out.println("Exiting scheduler. Goodbye!");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }
}