package me.kitakeyos;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

public class Tool {
    private static final byte[] TEXT_ENCRYPTION_KEY = new byte[]{7};
    private static final byte[] IMAGE_ENCRYPTION_KEY = new byte[]{7, 3, 8, 7, 5, 9, 4, 2, 5, 4, 9, 5, 4, 9, 3, 1, 2, 7, 5, 2};
    private static final byte[] PNG_HEADER_BYTES = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82};
    private static boolean decode;
    private static final Logger logger = Logger.getLogger(Tool.class.getName());

    static {
        try {
            LogManager.getLogManager().reset();
            logger.setLevel(Level.ALL);

            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(Level.INFO);
            logger.addHandler(ch);

            FileHandler fh = new FileHandler("tool.log", true);
            fh.setLevel(Level.FINE);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize logger handler.", e);
        }
    }

    public static void main(String[] args) {
        // true: Giải mã, false: Mã hoá
        decode = true;
        logger.info("Chương trình bắt đầu.");

        try {
            String inputFolder = "resources";
            String decodeOutputFolder = "resources_decoded";
            String encodeOutputFolder = "resources_encoded";

            logger.info("Thư mục đầu vào: " + inputFolder);
            logger.info("Thư mục đầu ra sau khi giải mã: " + decodeOutputFolder);
            logger.info("Thư mục đầu ra sau khi mã hóa: " + encodeOutputFolder);
            if (decode) {
                // Decode all resources
                logger.info("Bắt đầu giải mã tất cả tài nguyên.");
                processAllResources(inputFolder, decodeOutputFolder, false);
                logger.info("Giải mã hoàn tất.");
            } else {
                // Encode all resources
                logger.info("Bắt đầu mã hóa tất cả tài nguyên.");
                processAllResources(decodeOutputFolder, encodeOutputFolder, true);
                logger.info("Mã hóa hoàn tất.");
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Đã xảy ra lỗi trong quá trình xử lý.", e);
        }

        logger.info("Chương trình kết thúc.");
    }

    private static void processAllResources(String inputFolderPath, String outputFolderPath, boolean encode) throws IOException {
        logger.fine("Xử lý tất cả tài nguyên từ " + inputFolderPath + " đến " + outputFolderPath + (encode ? " (Mã hóa)" : " (Giải mã)"));
        Path inputPath = Paths.get(inputFolderPath).toAbsolutePath();
        if (!Files.exists(inputPath)) {
            String message = "Thư mục đầu vào không tồn tại: " + inputFolderPath;
            logger.severe(message);
            throw new IllegalArgumentException(message);
        }

        processFilesRecursively(inputPath.toFile(), inputPath, outputFolderPath, encode);
    }

    private static void processFilesRecursively(File file, Path basePath, String outputFolderPath, boolean encode) throws IOException {
        if (file.isDirectory()) {
            logger.fine("Đang xử lý thư mục: " + file.getAbsolutePath());
            File[] files = file.listFiles();
            if (files == null) {
                String message = "Không thể liệt kê các tệp trong thư mục: " + file.getAbsolutePath();
                logger.severe(message);
                throw new IOException(message);
            }

            for (File childFile : files) {
                processFilesRecursively(childFile, basePath, outputFolderPath, encode);
            }
        } else {
            logger.fine("Đang xử lý tệp: " + file.getAbsolutePath());
            processFile(file, basePath, outputFolderPath, encode);
        }
    }

    private static void processFile(File file, Path basePath, String outputFolderPath, boolean encode) throws IOException {
        String fileName = file.getName();
        String extension = getFileExtension(fileName).toLowerCase();

        logger.info((encode ? "Mã hóa" : "Giải mã") + " tệp: " + fileName + " với phần mở rộng: " + extension);

        if (isTextFile(extension)) {
            if (encode && extension.equals("txt")) {
                logger.fine("Mã hóa tài nguyên văn bản.");
                encodeTextResource(file, basePath, outputFolderPath, "bin");
            } else if (!encode && extension.equals("bin")) {
                logger.fine("Giải mã tài nguyên văn bản.");
                decodeTextResource(file, basePath, outputFolderPath, "txt");
            }
        } else if (isImageFile(extension)) {
            if (encode && extension.equals("png")) {
                logger.fine("Mã hóa tài nguyên hình ảnh.");
                encodeImageResource(file, basePath, outputFolderPath, "gf");
            } else if (!encode && extension.equals("gf")) {
                logger.fine("Giải mã tài nguyên hình ảnh.");
                decodeImageResource(file, basePath, outputFolderPath, "png");
            }
        } else {
            logger.warning("Phần mở rộng không xác định: " + extension + ". Đang sao chép tệp.");
            copyFile(file, basePath, outputFolderPath);
        }
    }

    // Text Resource Methods
    private static void encodeTextResource(File file, Path basePath, String outputFolder, String outputExtension) throws IOException {
        logger.fine("Đọc dữ liệu từ tệp văn bản để mã hóa: " + file.getAbsolutePath());
        byte[] data = Files.readAllBytes(file.toPath());
        logger.fine("Mã hóa dữ liệu văn bản.");
        encodeResourceBytes(data);
        logger.fine("Ghi dữ liệu đã mã hóa đến tệp: " + outputExtension);
        writeFile(data, file, basePath, outputFolder, outputExtension);
    }

    private static void decodeTextResource(File file, Path basePath, String outputFolder, String outputExtension) throws IOException {
        logger.fine("Đọc dữ liệu từ tệp mã hóa để giải mã: " + file.getAbsolutePath());
        byte[] data = Files.readAllBytes(file.toPath());
        logger.fine("Giải mã dữ liệu văn bản.");
        decodeResourceBytes(data);
        logger.fine("Ghi dữ liệu đã giải mã đến tệp: " + outputExtension);
        writeFile(data, file, basePath, outputFolder, outputExtension);
    }

    private static void encodeResourceBytes(byte[] data) {
        logger.fine("Bắt đầu mã hóa dữ liệu văn bản.");
        processResourceBytes(data, true);
        logger.fine("Hoàn tất mã hóa dữ liệu văn bản.");
    }

    private static void decodeResourceBytes(byte[] data) {
        logger.fine("Bắt đầu giải mã dữ liệu văn bản.");
        processResourceBytes(data, false);
        logger.fine("Hoàn tất giải mã dữ liệu văn bản.");
    }

    // Image Resource Methods
    private static void encodeImageResource(File file, Path basePath, String outputFolder, String outputExtension) throws IOException {
        logger.fine("Đọc dữ liệu từ tệp hình ảnh để mã hóa: " + file.getAbsolutePath());
        byte[] data = Files.readAllBytes(file.toPath());
        logger.fine("Mã hóa dữ liệu hình ảnh.");
        byte[] encodedData = encodeImageBytes(data);
        logger.fine("Ghi dữ liệu đã mã hóa đến tệp: " + outputExtension);
        writeFile(encodedData, file, basePath, outputFolder, outputExtension);
    }

    private static void decodeImageResource(File file, Path basePath, String outputFolder, String outputExtension) throws IOException {
        logger.fine("Đọc dữ liệu từ tệp mã hóa hình ảnh để giải mã: " + file.getAbsolutePath());
        byte[] data = Files.readAllBytes(file.toPath());
        logger.fine("Giải mã dữ liệu hình ảnh.");
        byte[] decodedData = decodeImageBytes(data, file.getName());
        logger.fine("Ghi dữ liệu đã giải mã đến tệp: " + outputExtension);
        writeFile(decodedData, file, basePath, outputFolder, outputExtension);
    }

    private static byte[] encodeImageBytes(byte[] data) {
        logger.fine("Kiểm tra và loại bỏ header PNG nếu có.");
        // Remove PNG header if present
        byte[] dataWithoutHeader = data;
        if (data.length > PNG_HEADER_BYTES.length && startsWith(data, PNG_HEADER_BYTES)) {
            dataWithoutHeader = new byte[data.length - PNG_HEADER_BYTES.length];
            System.arraycopy(data, PNG_HEADER_BYTES.length, dataWithoutHeader, 0, dataWithoutHeader.length);
            logger.fine("Đã loại bỏ header PNG.");
        } else {
            logger.fine("Không tìm thấy header PNG.");
        }

        // Encrypt image data
        logger.fine("Bắt đầu mã hóa dữ liệu hình ảnh.");
        processImageBytes(dataWithoutHeader, true);
        logger.fine("Hoàn tất mã hóa dữ liệu hình ảnh.");

        return dataWithoutHeader;
    }

    private static byte[] decodeImageBytes(byte[] data, String fileName) {
        logger.fine("Bắt đầu giải mã dữ liệu hình ảnh.");
        // Decrypt image data
        processImageBytes(data, false);
        logger.fine("Hoàn tất giải mã dữ liệu hình ảnh.");

        // Add PNG header
        logger.fine("Thêm header PNG vào dữ liệu đã giải mã.");
        byte[] decodedData = new byte[data.length + PNG_HEADER_BYTES.length];
        System.arraycopy(PNG_HEADER_BYTES, 0, decodedData, 0, PNG_HEADER_BYTES.length);
        System.arraycopy(data, 0, decodedData, PNG_HEADER_BYTES.length, data.length);
        logger.fine("Header PNG đã được thêm vào.");

        return decodedData;
    }

    private static void processImageBytes(byte[] data, boolean encrypt) {
        int keyIndex = 0;
        int step = 10;
        String action = encrypt ? "mã hóa" : "giải mã";
        logger.fine("Bắt đầu " + action + " dữ liệu hình ảnh với bước nhảy: " + step);
        for (int i = 0; i < data.length; i += step) {
            data[i] = (byte) (encrypt ? data[i] - IMAGE_ENCRYPTION_KEY[keyIndex] : data[i] + IMAGE_ENCRYPTION_KEY[keyIndex]);
            keyIndex = (keyIndex + 1) % IMAGE_ENCRYPTION_KEY.length;
        }
        logger.fine("Hoàn tất " + action + " dữ liệu hình ảnh.");
    }

    // Utility Methods
    private static void processResourceBytes(byte[] data, boolean encrypt) {
        String action = encrypt ? "mã hóa" : "giải mã";
        logger.fine("Bắt đầu " + action + " dữ liệu với khóa: " + bytesToHex(Tool.TEXT_ENCRYPTION_KEY));
        int keyIndex = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] += (byte) (encrypt ? TEXT_ENCRYPTION_KEY[keyIndex] : -Tool.TEXT_ENCRYPTION_KEY[keyIndex]);
            keyIndex = (keyIndex + 1) % Tool.TEXT_ENCRYPTION_KEY.length;
        }
        logger.fine("Hoàn tất " + action + " dữ liệu.");
    }

    private static void writeFile(byte[] data, File inputFile, Path basePath, String outputFolder, String outputExtension) throws IOException {
        Path inputPath = inputFile.toPath().toAbsolutePath();
        Path relativePath = basePath.relativize(inputPath);
        String outputFileName = replaceExtension(relativePath.getFileName().toString(), outputExtension);
        Path outputFilePath = Paths.get(outputFolder).toAbsolutePath().resolve(relativePath.getParent()).resolve(outputFileName);

        Files.createDirectories(outputFilePath.getParent());
        logger.fine("Ghi dữ liệu đến tệp: " + outputFilePath.toString());
        Files.write(outputFilePath, data);
        logger.info("Đã ghi tệp: " + outputFilePath.toString());
    }

    private static void copyFile(File inputFile, Path basePath, String outputFolder) throws IOException {
        Path inputPath = inputFile.toPath().toAbsolutePath();
        Path relativePath = basePath.relativize(inputPath);
        Path outputFilePath = Paths.get(outputFolder).toAbsolutePath().resolve(relativePath);

        Files.createDirectories(outputFilePath.getParent());
        logger.fine("Sao chép tệp từ " + inputPath.toString() + " đến " + outputFilePath.toString());
        Files.copy(inputPath, outputFilePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Đã sao chép tệp: " + outputFilePath.toString());
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private static boolean isTextFile(String extension) {
        return extension.equals("txt") || extension.equals("bin");
    }

    private static boolean isImageFile(String extension) {
        return extension.equals("png") || extension.equals("gf");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static String replaceExtension(String fileName, String newExtension) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return fileName + "." + newExtension;
        } else {
            return fileName.substring(0, dotIndex + 1) + newExtension;
        }
    }
}
