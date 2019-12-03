package cn.file.util;

import cn.file.Entity.BASE64DecodedMultipartFile;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

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

    /**
     * 校验后缀
     *
     * @param name
     * @param suffix
     * @return
     */
    public static boolean isCheckSuffix(String name, String[] suffix) {
        if (name != null && !"".equals(name)) {
            name = name.toLowerCase();
            //判断后缀不区分大小写
            for (String s : suffix) {
                if (name.endsWith(s.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 拿到文件后缀
     *
     * @param fileName
     * @return
     */
    public static String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getDate(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }

    /**
     * byte[] 转为MultipartFile
     *
     * @param base64
     * @return
     */
    public static MultipartFile base64ToMultipart(String base64) {
        try {
            String[] baseStrs = base64.split(",");

            BASE64Decoder decoder = new BASE64Decoder();
            byte[] b = new byte[0];
            b = decoder.decodeBuffer(baseStrs[1]);

            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }

            return new BASE64DecodedMultipartFile(b, baseStrs[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取上传单个文件
     *
     * @param request
     * @return
     */
    public static MultipartFile getMultipartFile(HttpServletRequest request) {
        MultipartFile imgUpload = null;

        //创建一个通用的多部分解析器
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(request.getSession().getServletContext());
        //判断 request 是否有文件上传,即多部分请求
        if (multipartResolver.isMultipart(request)) {
            //转换成多部分request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            //取得request中的所有文件名
            Iterator<String> iter = multiRequest.getFileNames();
            while (iter.hasNext()) {
                imgUpload = multiRequest.getFile(iter.next());
            }
        }
        return imgUpload;
    }

    /**
     * @param videoPath 源视频文件
     * @param picPath   截图存放路径
     */
    public static void getVideoPic(String videoPath, String picPath) throws Exception {
        try {
            FFmpegFrameGrabber ff = FFmpegFrameGrabber.createDefault(videoPath);
            ff.start();

            // 截取第5帧
            int i = 0;
            int length = ff.getLengthInFrames();
            Frame frame = null;
            while (i < length) {
                frame = ff.grabImage();
                if (i > 5 && frame.image != null) {
                    break;
                }
                i++;
            }
            //图片是否旋转 矫正
            String rotate = ff.getVideoMetadata("rotate");

            // 截取的帧图片
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage srcImage = converter.getBufferedImage(frame);
            int srcImageWidth = srcImage.getWidth();
            int srcImageHeight = srcImage.getHeight();

            // 对截图进行等比例缩放(缩略图)
            int width = 480;
            int height = (int) (((double) width / srcImageWidth) * srcImageHeight);
            BufferedImage thumbnailImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            thumbnailImage.getGraphics().drawImage(srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);

            File picFile = new File(picPath);
            if (rotate != null) {
                //旋转图片
                thumbnailImage = rotate(thumbnailImage, Integer.parseInt(rotate));
            }
            ImageIO.write(thumbnailImage, "jpg", picFile);

            ff.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 旋转图片
     *
     * @param src
     * @param angel
     * @return
     */
    public static BufferedImage rotate(BufferedImage src, int angel) {
        int src_width = src.getWidth(null);
        int src_height = src.getHeight(null);
        int type = src.getColorModel().getTransparency();
        Rectangle rect_des = calcRotatedSize(new Rectangle(new Dimension(src_width, src_height)), angel);
        BufferedImage bi = new BufferedImage(rect_des.width, rect_des.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rect_des.width - src_width) / 2, (rect_des.height - src_height) / 2);
        g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return bi;
    }

    /**
     * 计算比例
     *
     * @param src
     * @param angel
     * @return
     */
    public static Rectangle calcRotatedSize(Rectangle src, int angel) {
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);
        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new java.awt.Rectangle(new Dimension(des_width, des_height));
    }
}
