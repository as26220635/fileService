package cn.file.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by 余庚鑫 on 2017/3/5.
 */
@Component
public class FileUtil {

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    /**
     * 获取MD5
     *
     * @param bytes
     * @return
     */
    public static String getMd5ByBytes(byte[] bytes) {
        byte[] uploadBytes = bytes;

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] digest = md5.digest(uploadBytes);
        String hashString = new BigInteger(1, digest).toString(16);
        return hashString.toUpperCase();
    }

    public static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);

	         /*解除MappedByteBuffer文件占用*/
            Method getCleanerMethod = byteBuffer.getClass().getMethod("cleaner", new Class[0]);
            getCleanerMethod.setAccessible(true);
            sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(byteBuffer, new Object[0]);
            cleaner.clean();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value.toUpperCase();
    }
}
