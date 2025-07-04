import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int failedAttempts = 0;
            int lockMultiplier = 0;
            String subject = null;
                System.out.println("+----------------------------------------+");
                System.out.println("|         TEACHER LOGIN PORTAL          |");
                System.out.println("+----------------------------------------+");

            // Login loop with retry and incremental lockout
            while (true) {
                
                System.out.print("Enter teacher username: ");
                String username = scanner.nextLine().trim();
                while (username.isEmpty()) {
                System.out.print("Username cannot be empty. Please enter again: ");
                username = scanner.nextLine().trim();
                }

                System.out.print("Enter password: ");
                String password = scanner.nextLine().trim();
                while (password.isEmpty()) {
                 System.out.print("Password cannot be empty. Please enter again: ");
                password = scanner.nextLine().trim();
                }


                try {
                    subject = TeacherAuth.login(username, password);

                    if (subject != null) {
                        System.out.println("\n+-----------------------------+");
                        System.out.println("|    Login successful!        |");
                        System.out.println("+-----------------------------+");
                        System.out.println("Subject: " + subject);
                        break;
                    } else {
                        failedAttempts++;
                        System.out.println("\nInvalid login attempt.");

                        if (failedAttempts % 3 == 0) {
                            lockMultiplier++;
                            int lockTimeSeconds = lockMultiplier * 60;

                            for (int i = lockTimeSeconds; i > 0; i--) {
                                System.out.print("\rAccount locked. Try again after: " + i + " seconds");
                                System.out.flush();
                                Thread.sleep(1000);
                            }
                            System.out.print("\r                                      \r"); // Clear the line
                        }
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Post-login: dashboard loop
            while (true) {
                System.out.println("\n===============================================");
                System.out.println("|               TEACHER DASHBOARD             |");
                System.out.println("===============================================");

                System.out.println("Welcome to the Teacher Dashboard for subject: " + subject);
                System.out.println("+-----------------------------------------------+");
                System.out.println("| 1. View Class Attendance History             |");
                System.out.println("| 2. Start Attendance                          |");
                System.out.println("| 3. Logout                                    |");
                System.out.println("+-----------------------------------------------+");
                System.out.print("Enter choice (1, 2 or 3): ");
                String choice = scanner.nextLine();

                if (choice.equals("1")) {
                    TeacherDashboard.showDashboard(subject); // returns here after viewing
                } else if (choice.equals("2")) {
                    String enrollmentFileName = "student_" + subject.toLowerCase() + ".txt";
                    File enrollmentFile = new File(enrollmentFileName);

                    if (!enrollmentFile.exists()) {
                        try {
                            if (enrollmentFile.createNewFile()) {
                                System.out.println("Created enrollment file: " + enrollmentFileName);
                            }
                            System.out.println("No students enrolled for " + subject + ". Please enroll students to start attendance.");
                        } catch (IOException e) {
                            System.out.println("Failed to create enrollment file: " + enrollmentFileName);
                            e.printStackTrace();
                        }
                        continue;
                    }

                    if (enrollmentFile.length() == 0) {
                        System.out.println(enrollmentFileName + " is empty. Please enroll students before starting attendance.");
                        continue;
                    }

                    System.out.println("\n===============================================");
                    System.out.println("|        QR ATTENDANCE SCANNER ACTIVE        |");
                    System.out.println("===============================================");
                    System.out.println("Waiting for QR code...");

                    try {
                        Map<String, String> studentMap = AttendanceMarker.loadStudents();
                        long lastScanTime = System.currentTimeMillis();
                        long timeout = 2 * 60 * 1000; // 2 minutes

                        while (true) {
                            if (System.currentTimeMillis() - lastScanTime > timeout) {
                                System.out.println("Timeout: No QR code detected for 2 minutes. Returning to main menu...");
                                break;
                            }

                            BufferedImage frame = WebcamReader.captureFrame();

                            if (frame != null) {
                                String qrData = QRDecoder.decode(frame);

                                if (qrData != null && !qrData.isEmpty()) {
                                    lastScanTime = System.currentTimeMillis();

                                    String studentId = extractStudentId(qrData);
                                    if (studentId == null || !studentMap.containsKey(studentId)) {
                                        System.out.println("Student not found in student records.");
                                        Thread.sleep(2000);
                                        continue;
                                    }

                                    boolean enrolled = AttendanceMarker.isStudentEnrolledInSubject(studentId, subject);
                                    if (!enrolled) {
                                        System.out.println("Student is not enrolled in your subject.");
                                        Thread.sleep(2000);
                                        continue;
                                    }

                                    String studentName = studentMap.get(studentId);
                                    AttendanceMarker.markSubjectAttendance(studentId, studentName, subject);
                                    System.out.println(studentName + " (" + studentId + ") marked present for " + subject);
                                    Thread.sleep(3000);
                                }
                            }

                            Thread.sleep(500);
                        }
                    } catch (Exception e) {
                        System.out.println("An error occurred during attendance: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        WebcamReader.releaseCamera();
                        System.out.println("Camera released.");
                    }
                } else if (choice.equals("3")) {
                    System.out.println("\n+-----------------------+");
                    System.out.println("| Logging out. Goodbye!|");
                    System.out.println("+-----------------------+");
                    break;
                } else {
                    System.out.println("Invalid input. Please try again.");
                }
            }
        }
    }

    private static String extractStudentId(String qrData) {
        try {
            if (qrData != null && qrData.startsWith("MECARD:N:")) {
                qrData = qrData.substring(9, qrData.length() - 2).trim();
                return qrData;
            }
            return qrData.trim();
        } catch (Exception e) {
            System.out.println("Failed to parse student ID from QR data.");
        }
        return null;
    }
}
