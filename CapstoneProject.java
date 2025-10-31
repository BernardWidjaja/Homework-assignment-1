import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

// ========================
// LogManager Class
// ========================

// Exception Handling
class InvalidPathException extends Exception {
    public InvalidPathException(String message) {
        super(message);
    }

    public InvalidPathException(String message, Throwable cause) {
        super(message, cause);
    }
}

class LogManager {
    private BufferedWriter writer;
    private File currentLogFile;
    private String baseFolder;             // For dynamic folder path

    public LogManager(String folderName) {
        this.baseFolder = folderName.trim();
    }

    // Initialize and create folder structure for logs
    public void initializeLog() throws InvalidPathException {
        try {

            if (baseFolder == null || baseFolder.isEmpty()) {
                throw new InvalidPathException("Base folder name is empty or null.");
            }

            Date now = new Date();

            // Format year, month, date, time for folder and file
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH-mm-ss");

            String year = yearFormat.format(now);
            String month = monthFormat.format(now);
            String date = dateFormat.format(now);
            String time = timeFormat.format(now);

            // Folder structure: logs/year/month/date/
            String folderPath = "Logs/" + baseFolder + "/" + year + "/" + month + "/" + date;
            File folder = new File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created && !folder.exists()) {
                    throw new InvalidPathException("Failed to create folder path: " + folderPath);
                }
                System.out.println("[LOG] Created folders: " + folderPath);
            }

            // Create timestamped log file
            String fileName = "log_" + time + ".txt";
            File logFile = new File(folderPath, fileName);
            try {
                boolean fileCreated = logFile.createNewFile();
                // If file cannot be created, throw InvalidPathException
                if (!fileCreated && !logFile.exists()) {
                    throw new InvalidPathException("Failed to create log file: " + logFile.getAbsolutePath());
                }
            } catch (IOException ioe) {
                // Wrap and re-throw as InvalidPathException (user-defined)
                throw new InvalidPathException("I/O error while creating log file: " + ioe.getMessage(), ioe);
            }

            //homemadeAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
            currentLogFile = logFile; //Assign to currentLogFile so archiveLog()

            // Open writer
            try {
                writer = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException ioe) {
                // wrap and rethrow
                throw new InvalidPathException("I/O error while opening log writer: " + ioe.getMessage(), ioe);
            }

            log("[SYSTEM] Log initialized at " + now);
            System.out.println("[LOG] Log file created: " + logFile.getAbsolutePath());

        } catch (InvalidPathException ipe) {
            // rethrow user-defined exception unchanged
            throw ipe;
        } catch (Exception e) {
            // In case any unexpected runtime exception occurs, wrap in InvalidPathException
            throw new InvalidPathException("Unexpected error initializing log: " + e.getMessage(), e);
        }
    }

    // Write a log entry with timestamp
    public void log(String message) {
        try {
            if (writer == null) initializeLog();
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            writer.write("[" + timestamp + "] " + message + "\n");
            writer.flush();
        } catch (IOException | InvalidPathException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    // Close the log safely
    public void closeLog() {
        try {
            if (writer != null) {
                log("[SYSTEM] Log closed.");
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing log: " + e.getMessage());
        }
    }

    // Archive current log file
    public void archiveLog() {
        if (currentLogFile == null) {
            System.out.println("No log file to archive.");
            return;
        }

        try {
            // Extract year/month/date from currentLogFile path
            File parent = currentLogFile.getParentFile(); // points to date folder
            File monthFolder = parent.getParentFile();
            File yearFolder = monthFolder.getParentFile();

            // Create same structure inside Archive
            String archiveFolderPath = "Logs/" + baseFolder + "/Archive/" + yearFolder.getName() + "/" + monthFolder.getName() + "/" + parent.getName();
            File archiveDir = new File(archiveFolderPath);
            if (!archiveDir.exists()) archiveDir.mkdirs();

            File destFile = new File(archiveDir, currentLogFile.getName());

            try (FileInputStream in = new FileInputStream(currentLogFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            System.out.println("[LOG] Archived file to: " + destFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error archiving log: " + e.getMessage());
        }
    }

    public void viewLog() throws InvalidPathException {
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.print("Enter log base path (AGV/Battery/Overall/System): ");
                String baseFolder = sc.nextLine();
                if (!baseFolder.equals("AGV") && !baseFolder.equals("Battery")
                        && !baseFolder.equals("Overall") && !baseFolder.equals("System")) {
                    throw new InvalidPathException("Invalid base folder! Try again.");
                }

                System.out.print("Enter log year path (e.g., 2025): ");
                String year = sc.nextLine();
                if (!year.matches("\\d{4}")) throw new InvalidPathException("Year must be in yyyy format.");

                System.out.print("Enter log month path (e.g., Oct): ");
                String month = sc.nextLine();
                if (!month.matches("[A-Za-z]{3}")) throw new InvalidPathException("Month must be in MMM format.");

                System.out.print("Enter log date path (e.g., 28): ");
                String date = sc.nextLine();
                if (!date.matches("\\d{1,2}")) throw new InvalidPathException("Date must be numeric.");

                System.out.print("Enter log file name (e.g., log_12-34-56.txt): ");
                String fileName = sc.nextLine();
                if (!fileName.matches("log_\\d{2}-\\d{2}-\\d{2}\\.txt"))
                    throw new InvalidPathException("File name must be in log_HH-mm-ss.txt format.");

                File logFolder = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

                if (!logFolder.exists()) {
                    throw new InvalidPathException("File not found. Try again.");
                }

                // If everything is valid, open file and break the loop
                Desktop.getDesktop().open(logFolder);
                System.out.println("[LOG] Opened file: " + logFolder.getAbsolutePath());
                break;

            } catch (InvalidPathException | IOException e) {
                System.err.println("[ERROR] " + e.getMessage());
                // loop continues, user can re-enter
            }
        }
    }


    // ================================
    // Delete a log file by user input
    // ================================
    public void deleteLog() throws InvalidPathException {
        Scanner sc = new Scanner(System.in);

        while (true) {
            try {
                System.out.print("Enter log base path (AGV/Battery/Overall/System): ");
                String baseFolder = sc.nextLine();
                if (!baseFolder.equals("AGV") && !baseFolder.equals("Battery")
                        && !baseFolder.equals("Overall") && !baseFolder.equals("System")) {
                    throw new InvalidPathException("Invalid base folder! Try again.");
                }

                System.out.print("Enter log year path (e.g., 2025): ");
                String year = sc.nextLine();
                if (!year.matches("\\d{4}")) throw new InvalidPathException("Year must be in yyyy format.");

                System.out.print("Enter log month path (e.g., Oct): ");
                String month = sc.nextLine();
                if (!month.matches("[A-Za-z]{3}")) throw new InvalidPathException("Month must be in MMM format.");

                System.out.print("Enter log date path (e.g., 28): ");
                String date = sc.nextLine();
                if (!date.matches("\\d{1,2}")) throw new InvalidPathException("Date must be numeric.");

                System.out.print("Enter log file name (e.g., log_12-34-56.txt): ");
                String fileName = sc.nextLine();
                if (!fileName.matches("log_\\d{2}-\\d{2}-\\d{2}\\.txt"))
                    throw new InvalidPathException("File name must be in log_HH-mm-ss.txt format.");

                File logFile = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

                if (!logFile.exists()) {
                    throw new InvalidPathException("File not found. Try again.");
                }

                // Throw exception if file deletion fails
                if (!logFile.delete()) {
                    throw new InvalidPathException("Failed to delete file: " + logFile.getAbsolutePath());
                } else {
                    System.out.println("[LOG] File deleted successfully: " + logFile.getAbsolutePath());
                }

                break; // exit loop after successful deletion

            } catch (InvalidPathException e) {
                System.err.println("[ERROR] " + e.getMessage());
                // loop continues to allow re-entry
            }
        }
    }


    // ================================
    // Move a log file to another folder
    // ================================
    public void moveLog() {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter log base path (e.g., AGV): ");
        String baseFolder = sc.nextLine();

        System.out.print("Enter log year path (e.g., 2025): ");
        String year = sc.nextLine();
        System.out.print("Enter log month path (e.g., Oct): ");
        String month = sc.nextLine();
        System.out.print("Enter log date path (e.g., 28): ");
        String date = sc.nextLine();

        System.out.print("Enter log file name (e.g., log_12-34-56.txt): ");
        String fileName = sc.nextLine();

        File sourceFile = new File("Logs/" + baseFolder + "/" + year + "/" + month + "/" + date + "/" + fileName);

        if (!sourceFile.exists()) {
            System.out.println("[ERROR] Source file not found: " + sourceFile.getAbsolutePath());
            return;
        }

        System.out.print("Enter destination folder path (e.g., Logs/Archive/2025/Oct/28): ");
        String destinationPath = sc.nextLine();

        File destinationFolder = new File(destinationPath);
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
            System.out.println("[LOG] Created destination folder: " + destinationFolder.getAbsolutePath());
        }

        File destinationFile = new File(destinationFolder, fileName);

        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

        } catch (IOException e) {
            System.err.println("Error moving file: " + e.getMessage());
            return;
        }

// Delete after streams are fully closed
        if (sourceFile.delete()) {
            System.out.println("[LOG] File moved successfully to: " + destinationFile.getAbsolutePath());
        } else {
            System.out.println("[WARN] File copied but not deleted from source.");
        }
    }
}

// ========================
// Position Class
// ========================
class Position {
    private int row;
    private int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setRow(int r) {
        row = r;
    }

    public void setCol(int c) {
        col = c;
    }

    @Override
    public String toString() {
        return "[" + row + "," + col + "]";
    }
}

// ========================
// Battery Class
// ========================

class BatteryException extends Exception {
    public BatteryException(String message) {
        super(message);
    }
}

class Battery {
    private double level;

    public Battery() {
        level = Math.random() * 20 + 80; // start with 80-100%
    }

    public void discharge(double amount) {
        try {
            if (amount < 0) throw new BatteryException("Discharge amount cannot be negative!");
            level = Math.max(0, level - amount);
            System.out.println("[BATTERY] Discharged " + amount + "%. Current level: " + String.format("%.1f", level) + "%");
        } catch (BatteryException e) {
            System.err.println("[BATTERY ERROR] " + e.getMessage());
        }
    }

    public void recharge() {
        try {
            if (level >= 100) throw new BatteryException("Battery is already full!");
            System.out.println("[BATTERY] Charging started...");
            CapstoneProject.batteryLog.log("[BATTERY] Charging started...");
            CapstoneProject.overallLog.log("[BATTERY] Charging started...");

            while (level < 100) {
                long start = System.currentTimeMillis();

                level = Math.min(level + 20, 100); // increase by 20
                System.out.println("[BATTERY] Battery level: " + String.format("%.1f", level) + "%");
                CapstoneProject.batteryLog.log("[BATTERY] Battery level: " + String.format("%.1f", level) + "%");
                CapstoneProject.overallLog.log("[BATTERY] Battery level: " + String.format("%.1f", level) + "%");

                while (System.currentTimeMillis() - start < 1000) {
                    // wait ~1 second
                }
            }
            System.out.println("[BATTERY] Battery fully charged!");
            CapstoneProject.batteryLog.log("[BATTERY] Battery fully charged!");
            CapstoneProject.overallLog.log("[BATTERY] Battery fully charged!");

        } catch (BatteryException e) {
            System.err.println("[BATTERY ERROR] " + e.getMessage());
        }
    }

    public boolean isLow() {
        return level < 20;
    }

    public String getLevelString() {
        return String.format("%.1f", level);
    }
}

// ========================
// Abstract Resource
// ========================
abstract class Resource {
    protected String id;
    protected Position position;

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    protected abstract void displayInfo();
}

// ========================
// USER-DEFINED EXCEPTION
// ========================
class AGVException extends Exception {
    public AGVException(String message) {
        super(message);  // for simple messages
    }
}

// ========================
// AGV Class
// ========================
class AGV extends Resource {
    private Battery battery;
    private Box carriedBox;
    private boolean isActive;

    public AGV(String id) {
        this.id = id;
        this.battery = new Battery();
        this.isActive = false;
        this.position = new Position(10, 10); // random point //todolist //
    }

    public Battery getBattery() {
        return battery;
    }

    public void moveTo(Position target) {
        try {
            if (target == null) {
                throw new AGVException("Target position cannot be null!");
            }
            System.out.println("[AGV] AGV#" + id + " moved from " + position + " to " + target);
            CapstoneProject.agvLog.log("[AGV] AGV#" + id + " moved from " + position + " to " + target);
            CapstoneProject.systemLog.log("[AGV] AGV#" + id + " moved from " + position + " to " + target);
            CapstoneProject.overallLog.log("[AGV] AGV#" + id + " moved from " + position + " to " + target);
            this.position = target;
            battery.discharge(5);
        } catch (AGVException e) {
            System.err.println("[ERROR] AGV#" + id + ": " + e.getMessage());
            CapstoneProject.systemLog.log("[ERROR] AGV#" + id + ": " + e.getMessage());
            CapstoneProject.overallLog.log("[ERROR] AGV#" + id + ": " + e.getMessage());
        }
    }

    public void pickUpBox(Box box) {
        try {
            if (box == null) {
                throw new AGVException("Cannot pick up a null box!");
            }
            carriedBox = box;
            System.out.println("[AGV] AGV#" + id + " picked up Box#" + box.getId());
            CapstoneProject.agvLog.log("[AGV] AGV#" + id + " picked up Box#" + box.getId());
            CapstoneProject.systemLog.log("[AGV] AGV#" + id + " picked up Box#" + box.getId());
            CapstoneProject.overallLog.log("[AGV] AGV#" + id + " picked up Box#" + box.getId());
        } catch (AGVException e) {
            System.err.println("[ERROR] AGV#" + id + ": " + e.getMessage());
            CapstoneProject.systemLog.log("[ERROR] AGV#" + id + ": " + e.getMessage());
            CapstoneProject.overallLog.log("[ERROR] AGV#" + id + ": " + e.getMessage());
        }
    }

    public void dropBox(StorageArea area) {
        try {
            if (carriedBox == null) {
                throw new AGVException("No box to drop!");
            }
            if (area == null) {
                throw new AGVException("Storage area is null!");
            }

            area.storeBox(carriedBox);
            carriedBox = null;
        } catch (AGVException e) {
            System.err.println("[ERROR] " + e.getMessage());
            CapstoneProject.systemLog.log("[ERROR] " + e.getMessage());
            CapstoneProject.overallLog.log("[ERROR] " + e.getMessage());
        }
    }

    public void setActive(boolean state) {
        isActive = state;
    }

    @Override
    protected void displayInfo() {
        System.out.println("[INFO] AGV#" + id + " | Battery: " + battery.getLevelString() + "% | Active: " + isActive);
        CapstoneProject.agvLog.log("[INFO] AGV#" + id + " | Battery: " + battery.getLevelString() + "% | Active: " + isActive);
        CapstoneProject.batteryLog.log("[INFO] AGV#" + id + " | Battery: " + battery.getLevelString() + "% | Active: " + isActive);
        CapstoneProject.overallLog.log("[INFO] AGV#" + id + " | Battery: " + battery.getLevelString() + "% | Active: " + isActive);
    }
}

// ========================
// USER-DEFINED EXCEPTION
// ========================
class BoxException extends Exception {
    public BoxException(String message) {
        super(message);
    }
}

// ========================
// Box Class
// ========================
class Box extends Resource {
    private String weight;
    private String content;

    public Box(String id, String weight, String content) {
        this.id = id;
        this.weight = weight;
        this.content = content;
        this.position = new Position(-5, -5);
    }

    public void setPosition(int row, int col) {
        try {
            if (position == null) {
                throw new BoxException("Position object is null!");
            }
            position.setRow(row);
            position.setCol(col);
        } catch (BoxException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    public int getRow() {
        return position.getRow();
    }

    public int getColumn() {
        return position.getCol();
    }

    @Override
    public String toString() {
        return "Box ID: " + id + ", Weight: " + weight + " kg, Content:" + content;
    }

    @Override
    protected void displayInfo() {
        System.out.println("Box#" + id + " | " + content + " | Storage Pos: " + position);
    }
}

// ========================
// USER-DEFINED EXCEPTION
// ========================
class StorageAreaException extends Exception {
    public StorageAreaException(String message) {
        super(message);
    }
}

// ========================
// StorageArea Class
// ========================
class StorageArea {
    private Box[][] shelves;
    private int rows, cols;

    public StorageArea(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        shelves = new Box[rows][cols];
    }

    // Find first empty slot in shelves
    public Position findEmptySlot() {
        try {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (shelves[r][c] == null) {
                        return new Position(r, c);
                    }
                }
            }
            throw new StorageAreaException("No empty slot available.");
        } catch (StorageAreaException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return null; //full
        }
    }

    // Find box position by ID
    public Position findBoxById(String id) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (shelves[r][c] != null && shelves[r][c].getId().equals(id)) {
                    return new Position(r, c);
                }
            }
        }
        return null; // not found
    }

    public Box getBoxAt(int row, int col) {
        try {
            if (row >= 0 && col >= 0 && row < shelves.length && col < shelves[0].length) {
                return shelves[row][col];
            }
            throw new StorageAreaException("Invalid position [" + row + "," + col + "]");
        } catch (StorageAreaException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return null; //not found
        }
    }

    public void storeBox(Box box) {
        try {
            int row = box.getRow();
            int col = box.getColumn();
            if (row < 0 || col < 0 || row >= rows || col >= cols) {
                System.out.println("[ERROR] Invalid storage position " + box.getPosition());
                CapstoneProject.systemLog.log("[ERROR] Invalid storage position " + box.getPosition());
                CapstoneProject.overallLog.log("[ERROR] Invalid storage position " + box.getPosition());
                throw new StorageAreaException("Invalid storage position " + box.getPosition());
            }

            if (shelves[row][col] == null) {
                shelves[row][col] = box;
                System.out.println("[STORAGE] Stored Box#" + box.getId() + " at " + box.getPosition());
                CapstoneProject.systemLog.log("[STORAGE] Stored Box#" + box.getId() + " at " + box.getPosition());
                CapstoneProject.overallLog.log("[STORAGE] Stored Box#" + box.getId() + " at " + box.getPosition());
            } else {
                System.out.println("[ERROR] Position " + box.getPosition() + " is occupied!");
                CapstoneProject.systemLog.log("[ERROR] Position " + box.getPosition() + " is occupied!");
                CapstoneProject.overallLog.log("[ERROR] Position " + box.getPosition() + " is occupied!");
                throw new StorageAreaException("Position " + box.getPosition() + " is occupied!");
            }
        } catch (StorageAreaException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    public Box retrieveBox(int row, int col) {
        try {
            if (row < 0 || col < 0 || row >= rows || col >= cols) {
                System.out.println("[ERROR] Invalid retrieve position [" + row + "," + col + "]");
                CapstoneProject.systemLog.log("[ERROR] Invalid retrieve position [" + row + "," + col + "]");
                CapstoneProject.overallLog.log("[ERROR] Invalid retrieve position [" + row + "," + col + "]");
                throw new StorageAreaException("Invalid retrieve position [" + row + "," + col + "]");
            }

            if (shelves[row][col] != null) {
                Box box = shelves[row][col];
                shelves[row][col] = null;
                System.out.println("[RETRIEVE] Retrieved Box#" + box.getId() + " from [" + row + "," + col + "]");
                CapstoneProject.systemLog.log("[RETRIEVE] Retrieved Box#" + box.getId() + " from [" + row + "," + col + "]");
                CapstoneProject.overallLog.log("[RETRIEVE] Retrieved Box#" + box.getId() + " from [" + row + "," + col + "]");
                return box;
            } else {
                System.out.println("[RETRIEVE] No box at [" + row + "," + col + "]");
                CapstoneProject.systemLog.log("[RETRIEVE] No box at [" + row + "," + col + "]");
                CapstoneProject.overallLog.log("[RETRIEVE] No box at [" + row + "," + col + "]");
                throw new StorageAreaException("No box at [" + row + "," + col + "]");
            }
        } catch (StorageAreaException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return null; // still must return something
        }
    }

    public void displayAllBoxes() {
        System.out.println("=== Storage Area Contents ===");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Box box = shelves[i][j];
                if (box != null) {
                    System.out.println("Row " + i + ", Col " + j + " : " + box);
                } else {
                    System.out.println("Row " + i + ", Col " + j + ": [Empty]");
                }
            }
        }
    }
}

// ========================
// ChargingStation Class
// ========================

class ChargingStationException extends Exception {
    public ChargingStationException(String message) {
        super(message);
    }
}

class ChargingStation {
    private String stationId;
    private boolean isOccupied;
    private AGV currentAGV;
    private Position position;

    public ChargingStation(String id, Position pos) {
        this.stationId = id;
        this.position = pos;
        this.isOccupied = false;
    }

    public void assignAGV(AGV agv) {
        try {
            if (!isOccupied) {
                currentAGV = agv;
                isOccupied = true;
                agv.moveTo(position);
                System.out.println("[AGV] AGV#" + agv.getId() + " assigned to Charging Station#" + stationId);
                CapstoneProject.agvLog.log("[AGV] AGV#" + agv.getId() + " assigned to Charging Station#" + stationId);
                CapstoneProject.systemLog.log("[AGV] AGV#" + agv.getId() + " assigned to Charging Station#" + stationId);
                CapstoneProject.overallLog.log("[AGV] AGV#" + agv.getId() + " assigned to Charging Station#" + stationId);
            } else {
                throw new ChargingStationException("Charging Station#" + stationId + " is already occupied by AGV#"
                        + currentAGV.getId() + ". Cannot assign AGV#" + agv.getId());
            }
        } catch (ChargingStationException e) {
            System.err.println("[CHARGING ERROR] " + e.getMessage());
            CapstoneProject.systemLog.log("[CHARGING ERROR] " + e.getMessage());
            CapstoneProject.overallLog.log("[CHARGING ERROR] " + e.getMessage());
        }
    }

    public void chargeAGV() {
        try {
            if (currentAGV != null) {
                currentAGV.getBattery().recharge();
                System.out.println("[AGV] AGV#" + currentAGV.getId() + " fully charged at Station#" + stationId);
                CapstoneProject.agvLog.log("[AGV] AGV#" + currentAGV.getId() + " fully charged at Station#" + stationId);
                CapstoneProject.batteryLog.log("[AGV] AGV#" + currentAGV.getId() + " fully charged at Station#" + stationId);
                CapstoneProject.overallLog.log("[AGV] AGV#" + currentAGV.getId() + " fully charged at Station#" + stationId);
                isOccupied = false;
                currentAGV = null;
            } else {
                throw new ChargingStationException("No AGV assigned to Charging Station#" + stationId + " to charge.");
            }
        } catch (ChargingStationException e) {
            System.err.println("[CHARGING ERROR] " + e.getMessage());
            CapstoneProject.systemLog.log("[CHARGING ERROR] " + e.getMessage());
            CapstoneProject.overallLog.log("[CHARGING ERROR] " + e.getMessage());
        }
    }
}


// ========================
// Abstract Process
// ========================

class ProcessException extends Exception {
    public ProcessException(String message) {
        super(message);
    }
}

abstract class Process {
    protected AGV activeAGV;
    protected AGV standbyAGV;
    protected Box box;
    protected StorageArea storageArea;
    protected ChargingStation station;
    protected String status;
    protected Position PICKUP_POS = new Position(-1, -1);
    protected Position DROPOFF_POS = new Position(5, 5);

    public Process(AGV active, AGV standby, Box box, StorageArea area, ChargingStation station) {
        this.activeAGV = active;
        this.standbyAGV = standby;
        this.box = box;
        this.storageArea = area;
        this.station = station;
    }

    protected void checkAndSwapAGV() throws ProcessException {
        if (activeAGV.getBattery().isLow()) {
            System.out.println("[AGV] AGV#" + activeAGV.getId() + " low on battery. Switching with standby AGV.");
            CapstoneProject.agvLog.log("[AGV] AGV#" + activeAGV.getId() + " low on battery. Switching with standby AGV.");
            CapstoneProject.systemLog.log("[AGV] AGV#" + activeAGV.getId() + " low on battery. Switching with standby AGV.");
            CapstoneProject.overallLog.log("[AGV] AGV#" + activeAGV.getId() + " low on battery. Switching with standby AGV.");

            // send active to charge
            station.assignAGV(activeAGV);
            station.chargeAGV();

            // if standby AGV is also low, throw exception
            if (standbyAGV.getBattery().isLow()) {
                throw new ProcessException("Both AGVs have low battery. Process cannot continue!");
            }

            // swap
            AGV temp = activeAGV;
            activeAGV = standbyAGV;
            standbyAGV = temp;
            activeAGV.setActive(true);
            standbyAGV.setActive(false);
        }
    }

    protected abstract void execute() throws ProcessException;

    protected abstract void logProcess();
}

// ========================
// Storing Process
// ========================
class Storing extends Process {
    public Storing(AGV active, AGV standby, Box box, StorageArea area, ChargingStation station) {
        super(active, standby, box, area, station);
    }

    @Override
    protected void execute() throws ProcessException {
        checkAndSwapAGV();

        if (box == null) {
            throw new ProcessException("No box found to store!");
        }

        activeAGV.moveTo(PICKUP_POS);
        activeAGV.pickUpBox(box);

        activeAGV.moveTo(box.getPosition());
        activeAGV.getBattery().discharge(20);
        activeAGV.dropBox(storageArea);

        status = "Stored";
    }

    @Override
    protected void logProcess() {
        System.out.println("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
        CapstoneProject.systemLog.log("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
        CapstoneProject.overallLog.log("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
    }
}


// ========================
// Retrieving Process
// ========================
class Retrieving extends Process {
    public Retrieving(AGV active, AGV standby, Box box, StorageArea area, ChargingStation station) {
        super(active, standby, box, area, station);
    }

    @Override
    protected void execute() throws ProcessException {
        checkAndSwapAGV();

        if (box == null) {
            throw new ProcessException("No box assigned for retrieval!");
        }

        int row = box.getRow();
        int col = box.getColumn();

        activeAGV.moveTo(box.getPosition());
        activeAGV.pickUpBox(box);

        Box retrieved = storageArea.retrieveBox(row, col);
        if (retrieved != null) {
            activeAGV.moveTo(DROPOFF_POS);
            activeAGV.getBattery().discharge(15);
            status = "Retrieved";
        } else {
            status = "Failed";
            throw new ProcessException("Box could not be retrieved from storage!");
        }
    }

    @Override
    protected void logProcess() {
        System.out.println("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
        CapstoneProject.systemLog.log("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
        CapstoneProject.overallLog.log("[PROCESS] " + status + " Box#" + box.getId() + " by AGV#" + activeAGV.getId());
    }
}


// ========================
// User-defined Exception
// ========================
class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }
}

// ========================
// StorageSystem Subclasses with Exception Handling
// ========================
abstract class StorageSystem {
    protected static int totalBoxes = 0;
    protected static int enteredCount = 0;
    protected static int exitedCount = 0;

    protected abstract void recordEvent(Box box) throws StorageException;

    protected abstract void displayLog();
}

class isBoxEntered extends StorageSystem {
    @Override
    protected void recordEvent(Box box) throws StorageException {
        try {
            if (box == null) throw new StorageException("Cannot enter a null box!");
            enteredCount++;
            totalBoxes++;
            System.out.println("[EVENT] Box#" + box.getId() + " entered");
            CapstoneProject.systemLog.log("[EVENT] Box#" + box.getId() + " entered");
            CapstoneProject.overallLog.log("[EVENT] Box#" + box.getId() + " entered");
        } catch (StorageException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    @Override
    protected void displayLog() {
        System.out.println("[INFO] Total boxes entered: " + enteredCount);
        CapstoneProject.systemLog.log("[INFO] Total boxes entered: " + enteredCount);
    }
}

class isBoxStored extends StorageSystem {
    @Override
    protected void recordEvent(Box box) throws StorageException {
        try {
            if (box == null) throw new StorageException("Cannot store a null box!");
            System.out.println("[EVENT] Box#" + box.getId() + " stored");
            CapstoneProject.systemLog.log("[EVENT] Box#" + box.getId() + " stored");
            CapstoneProject.overallLog.log("[EVENT] Box#" + box.getId() + " stored");
        } catch (StorageException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    @Override
    protected void displayLog() {
        System.out.println("[INFO] Total boxes stored: " + totalBoxes);
        CapstoneProject.overallLog.log("[INFO] Total boxes stored: " + totalBoxes);
    }
}

class isBoxExited extends StorageSystem {
    @Override
    protected void recordEvent(Box box) throws StorageException {
        try {
            if (box == null) throw new StorageException("Cannot exit a null box!");
            exitedCount++;
            totalBoxes--;
            System.out.println("[EVENT] Box#" + box.getId() + " exited");
            CapstoneProject.systemLog.log("[EVENT] Box#" + box.getId() + " exited");
            CapstoneProject.overallLog.log("[EVENT] Box#" + box.getId() + " exited");
        } catch (StorageException e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    @Override
    protected void displayLog() {
        System.out.println("[INFO] Total boxes exited: " + exitedCount);
        CapstoneProject.overallLog.log("[INFO] Total boxes exited: " + exitedCount);
    }
}

// ========================
// MAIN SIMULATION
// ========================
public class CapstoneProject {
    public static LogManager agvLog;
    public static LogManager batteryLog;
    public static LogManager systemLog;
    public static LogManager overallLog;

    public static void main(String[] args) {
        try {
            agvLog = new LogManager("AGV");
            batteryLog = new LogManager("Battery");
            systemLog = new LogManager("System");
            overallLog = new LogManager("Overall");

            agvLog.initializeLog();
            batteryLog.initializeLog();
            systemLog.initializeLog();
            overallLog.initializeLog();

            systemLog.log("[INFO] Warehouse automation simulation started.");
            overallLog.log("[INFO] Warehouse automation simulation started.");

            StorageArea area = new StorageArea(5, 5);
            systemLog.log("[INFO] Storage area is 5x5");
            overallLog.log("[INFO] Storage area is 5x5");
            ChargingStation station1 = new ChargingStation("CS1", new Position(0, 5));
            ChargingStation station2 = new ChargingStation("CS2", new Position(1, 5));

            isBoxEntered enteredLog = new isBoxEntered();
            isBoxStored storedLog = new isBoxStored();
            isBoxExited exitedLog = new isBoxExited();

            AGV storingActive = new AGV("1");      // active for storing
            AGV storingStandby = new AGV("2");     // standby for storing
            AGV retrievingActive = new AGV("3");   // active for retrieving
            AGV retrievingStandby = new AGV("4");  // standby for retrieving

            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n==============================");
                System.out.println("-------------" + enteredLog.enteredCount + storedLog.totalBoxes + exitedLog.exitedCount);
                System.out.println("HELLO! WAREHOUSE MENU");
                System.out.println("1. Store Box");
                System.out.println("2. Retrieve Box");
                System.out.println("3. Display Info");
                System.out.println("4. Open Previous Log File/Move File/Delete File");
                System.out.println("0. Exit");
                System.out.print("Please enter the number: ");
                String choice = sc.nextLine();

                switch (choice) {
                    case "1": {
                        try {
                            String id;
                            while (true) {
                                System.out.print("Enter Box ID: ");
                                id = sc.nextLine();
                                // Check if Box ID already exists
                                if (area.findBoxById(id) != null) {
                                    System.out.println("[ERROR] Box ID already exists in storage. Please enter a different ID.");
                                    systemLog.log("[ERROR] Box ID already exists in storage. Please enter a different ID.");
                                    overallLog.log("[ERROR] Box ID already exists in storage. Please enter a different ID.");
                                } else {
                                    break; // ID is unique, proceed
                                }
                            }

                            String weight;
                            while (true) {
                                System.out.print("Enter weight (kg): ");
                                weight = sc.nextLine();
                                // Validate numeric
                                if (!weight.matches("\\d+(\\.\\d+)?")) {  // allows integer or decimal
                                    System.out.println("[ERROR] Weight must be a numeric value. Try again.");
                                    systemLog.log("[ERROR] Invalid weight entered: " + weight);
                                    overallLog.log("[ERROR] Invalid weight entered: " + weight);
                                } else {
                                    break;
                                }
                            }

                            System.out.print("Enter description: ");
                            String desc = sc.nextLine();

                            Box userBox = new Box(id, weight, desc);
                            Position emptySlot = area.findEmptySlot();

                            if (emptySlot != null) {
                                userBox.setPosition(emptySlot.getRow(), emptySlot.getCol());
                                enteredLog.recordEvent(userBox);

                                Storing storeProcess = new Storing(storingActive, storingStandby, userBox, area, station1);
                                storeProcess.execute();
                                storeProcess.logProcess();
                                storedLog.recordEvent(userBox);
                            } else {
                                throw new ProcessException("Storage area is full! Cannot store Box#" + id);
                            }
                        } catch (ProcessException pe) {
                            System.err.println("[PROCESS ERROR] " + pe.getMessage());
                            systemLog.log("[PROCESS ERROR] " + pe.getMessage());
                            overallLog.log("[PROCESS ERROR] " + pe.getMessage());
                        }
                        break;
                    }


                    case "2": {
                        try {
                            System.out.print("Enter Box ID to retrieve: ");
                            String id = sc.nextLine();
                            Position boxPos = area.findBoxById(id);

                            if (boxPos != null) {
                                Box storedBox = area.getBoxAt(boxPos.getRow(), boxPos.getCol());
                                storedBox.setPosition(boxPos.getRow(), boxPos.getCol());

                                Retrieving retrieveProcess = new Retrieving(retrievingActive, retrievingStandby, storedBox, area, station2);
                                retrieveProcess.execute();
                                retrieveProcess.logProcess();
                                exitedLog.recordEvent(storedBox);
                            } else {
                                throw new ProcessException("Box with ID " + id + " not found in storage.");
                            }
                        } catch (ProcessException pe) {
                            System.err.println("[PROCESS ERROR] " + pe.getMessage());
                            systemLog.log("[PROCESS ERROR] " + pe.getMessage());
                            overallLog.log("[PROCESS ERROR] " + pe.getMessage());
                        }
                        break;
                    }

                    case "3":
                        System.out.println("=== Storage Info ===");
                        area.displayAllBoxes();

                        System.out.println("\n=== AGV Info ===");
                        storingActive.displayInfo();
                        storingStandby.displayInfo();
                        retrievingActive.displayInfo();
                        retrievingStandby.displayInfo();

                        enteredLog.displayLog();
                        exitedLog.displayLog();
                        storedLog.displayLog();
                        break;
                    case "4":
                        System.out.print("Do you want to view a previous log file? (y/n): ");
                        if (sc.nextLine().equalsIgnoreCase("y")) {
                            systemLog.viewLog();
                        }

                        System.out.print("\nDo you want to delete a log file? (y/n): ");
                        if (sc.nextLine().equalsIgnoreCase("y")) {
                            systemLog.deleteLog();
                        }

                        System.out.print("\nDo you want to move a log file? (y/n): ");
                        if (sc.nextLine().equalsIgnoreCase("y")) {
                            systemLog.moveLog();
                        }
                        break;
                    case "0":
                        agvLog.log("[INFO] Simulation completed successfully.");
                        batteryLog.log("[INFO] Simulation completed successfully.");
                        systemLog.log("[INFO] Simulation completed successfully.");
                        overallLog.log("[INFO] Simulation completed successfully.");

                        agvLog.closeLog();
                        batteryLog.closeLog();
                        systemLog.closeLog();
                        overallLog.closeLog();

                        agvLog.archiveLog();
                        batteryLog.archiveLog();
                        systemLog.archiveLog();
                        overallLog.archiveLog();

                        System.out.println("Exiting system...");
                        return;
                    default:
                        System.out.println("Invalid input!");
                        break;
                }
            }
        } catch (InvalidPathException e) {
            System.err.println("[ERROR] Invalid log folder path: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected error: " + e.getMessage());
        } finally {
            // Ensure all log files are closed
            if (agvLog != null) agvLog.closeLog();
            if (batteryLog != null) batteryLog.closeLog();
            if (systemLog != null) systemLog.closeLog();
            if (overallLog != null) overallLog.closeLog();

            System.out.println("[INFO] All log files closed (if any were open).");
        }
    }
}