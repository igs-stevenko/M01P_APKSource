package model;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileControl {

    public static boolean IsFileExist(String FilePath) {

        File file = new File(FilePath);

        return file.exists();
    }

    public static String CalculateMD5(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(filePath);
            DigestInputStream dis = new DigestInputStream(fis, md);

            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // 读取文件并更新 MD5 散列
            }

            byte[] digest = md.digest();

            // 转换字节数组为十六进制字符串
            StringBuilder md5StringBuilder = new StringBuilder();
            for (byte b : digest) {
                md5StringBuilder.append(String.format("%02x", b));
            }

            fis.close();

            return md5StringBuilder.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String ReadStringFromFile(String filePath) {

        File file = new File(filePath);
        if (file.exists() == false)
            return "";

        StringBuilder content = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return content.toString().replace("\n", "");
    }

    public static int Unzip(String zipFilePath, String destDirectory)  {

        int rtn = 0;

        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();

            while (entry != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDirectory + File.separator + entryName);

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    FileOutputStream fos = new FileOutputStream(entryFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();
                    fos.getFD().sync();

                }
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            Log.d(TAGS, "Unzip IOException");
            rtn = -1;
        }

        return rtn;
    }

    public static int RemoveFolder(String folderPath) {

        int rtn = 0;
        File folder = new File(folderPath);
        rtn = deleteFolder(folder);
        try {
            Runtime.getRuntime().exec("sync");
        } catch (IOException e) {
            return -1;
        }
        return rtn;
    }

    private static int deleteFolder(File folder) {

        boolean deletionStatus = true;
        int rtn = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子文件夹的内容
                    rtn = deleteFolder(file);
                    if(rtn < 0){
                        return rtn;
                    }
                }
                // 删除文件或空文件夹
                deletionStatus = file.delete();
                if(deletionStatus != true){
                    return -1;
                }
            }
            return 0;
        }
        else{
            return -1;
        }
    }

    public static int CopyAllDir(String source, String dest)  {

        File sourceFile = new File(source);
        File destFile = new File(dest);

        return CopyDir(sourceFile, destFile);
    }

    private static int CopyDir(File soruce, File dest) {

        int rtn = 0;

        /* 檢查來源是否為資料夾 */
        if (!soruce.isDirectory()) {
            return -1;
        }
        /* 檢查目標資料夾是否存在，若不存在則先創建 */
        if (!dest.exists()) {
            dest.mkdir();
            dest.setReadable(true, false);
            dest.setWritable(true, false);
            dest.setExecutable(true, false);
        }

        File[] files = soruce.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File newDestinationDirectory = new File(dest, file.getName());
                    CopyDir(file, newDestinationDirectory);
                } else {
                    Path sourcePath = file.toPath();
                    Path destinationPath = new File(dest, file.getName()).toPath();
                    rtn = CopyFile(sourcePath.toString(), destinationPath.toString());
                    if(rtn != 0) {
                        rtn = -2;
                        break;
                    }
                    //Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    File targetfile = new File(destinationPath.toString());
                    targetfile.setReadable(true, false);
                    targetfile.setWritable(true, false);
                    targetfile.setExecutable(true, false);
                }
            }
        }

        return rtn;
    }

    public static int CopyFile(String source, String dest) {

        int rtn = 0;
        File sourceFile = new File(source);
        File destFile = new File(dest);

        //Log.d(TAGS, "File Len = " + sourceFile.length());

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(sourceFile);
            fos = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
            fos.getFD().sync();
            fis.close();
            fos.close();

        } catch (IOException e) {
            Log.d(TAGS, "CopyFile ERROR");

            rtn = -1;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                Log.d(TAGS, "IOException closing file stream failed");
                rtn = -2;
            }
        }

        return rtn;
    }

    static String TAGS = "## [KO] FileControl";
}
