package cn.file.controlller;

import cn.file.Entity.BASE64DecodedMultipartFile;
import cn.file.Entity.CheckResult;
import cn.file.overall.Properties;
import cn.file.util.FileUtil;
import cn.file.util.TokenUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.util.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.mvc.LastModified;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by 余庚鑫 on 2019/11/30
 * 文件服务器对外接口
 */
@Controller
public class FileController implements LastModified {
    static {
        ImageIO.scanForPlugins();
    }

    public static final String[] ALLOW_SUFFIX_IMG = {"jpg", "jpeg", "png", "gif", "bmp"};
    public static final String FORMAT2 = "yyyy-MM-dd";

    /**
     * 缓存
     */
    private long lastModified = System.currentTimeMillis();

    /**
     * 预览文件
     *
     * @param base64
     * @param webRequest
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping("/preview/{base64}")
    public void preview(@PathVariable("base64") String base64, WebRequest webRequest, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (webRequest.checkNotModified(lastModified)) {
            return;
        }
        if (isEmpty(base64)) {
            return;
        }
        response.setCharacterEncoding("UTF-8");

        OutputStream os = null;
        InputStream is = null;
        InputStream inputStream = null;
        try {
            //base64解密 获得url信息
            String[] paths = new String(Base64.decode(base64), "UTF-8").split("@@@");
            String filePath = paths[0];
            String fileName = paths[1];
            //获得文件
            File file = new File(Properties.FILE_DIR + filePath + File.separator + fileName);
            if (!file.exists()) {
                return;
            }

            inputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            inputStream.read(data);
            inputStream.close();

            if (FileUtil.isCheckSuffix(fileName, ALLOW_SUFFIX_IMG)) {
                response.setContentType("image/" + FileUtil.getSuffix(fileName));
            }

            os = response.getOutputStream();
            os.write(data);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * 视频播放
     *
     * @param base64   加密后的串
     * @param request
     * @param response
     */
    @GetMapping("/player/{base64}")
    public void player(@PathVariable("base64") String base64, HttpServletRequest request, HttpServletResponse response) {
        if (isEmpty(base64)) {
            return;
        }
        BufferedInputStream bis = null;
        try {
            //base64解密 获得url信息
            String[] paths = new String(Base64.decode(base64), "UTF-8").split("@@@");
            String filePath = paths[0];
            String fileName = paths[1];
            //获得文件
            File file = new File(Properties.FILE_DIR + filePath + File.separator + fileName);
            if (!file.exists()) {
                return;
            }

            long p = 0L;
            long toLength = 0L;
            long contentLength = 0L;
            int rangeSwitch = 0; // 0,从头开始的全文下载；1,从某字节开始的下载（bytes=27000-）；2,从某字节开始到某字节结束的下载（bytes=27000-39000）
            long fileLength;
            String rangBytes = "";
            fileLength = file.length();

            // get file content
            InputStream ins = new FileInputStream(file);
            bis = new BufferedInputStream(ins);

            // tell the client to allow accept-ranges
            response.reset();
            response.setHeader("Accept-Ranges", "bytes");

            // client requests a file block download start byte
            String range = request.getHeader("Range");
            if (range != null && range.trim().length() > 0 && !"null".equals(range)) {
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
                rangBytes = range.replaceAll("bytes=", "");
                if (rangBytes.endsWith("-")) { // bytes=270000-
                    rangeSwitch = 1;
                    p = Long.parseLong(rangBytes.substring(0, rangBytes.indexOf("-")));
                    contentLength = fileLength - p; // 客户端请求的是270000之后的字节（包括bytes下标索引为270000的字节）
                } else { // bytes=270000-320000
                    rangeSwitch = 2;
                    String temp1 = rangBytes.substring(0, rangBytes.indexOf("-"));
                    String temp2 = rangBytes.substring(rangBytes.indexOf("-") + 1, rangBytes.length());
                    p = Long.parseLong(temp1);
                    toLength = Long.parseLong(temp2);
                    contentLength = toLength - p + 1; // 客户端请求的是 270000-320000 之间的字节
                }
            } else {
                contentLength = fileLength;
            }

            // 如果设设置了Content-Length，则客户端会自动进行多线程下载。如果不希望支持多线程，则不要设置这个参数。
            // Content-Length: [文件的总大小] - [客户端请求的下载的文件块的开始字节]
            response.setHeader("Content-Length", new Long(contentLength).toString());

            // 断点开始
            // 响应的格式是:
            // Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
            if (rangeSwitch == 1) {
                String contentRange = new StringBuffer("bytes ").append(new Long(p).toString()).append("-")
                        .append(new Long(fileLength - 1).toString()).append("/")
                        .append(new Long(fileLength).toString()).toString();
                response.setHeader("Content-Range", contentRange);
                bis.skip(p);
            } else if (rangeSwitch == 2) {
                String contentRange = range.replace("=", " ") + "/" + new Long(fileLength).toString();
                response.setHeader("Content-Range", contentRange);
                bis.skip(p);
            } else {
                String contentRange = new StringBuffer("bytes ").append("0-").append(fileLength - 1).append("/")
                        .append(fileLength).toString();
                response.setHeader("Content-Range", contentRange);
            }

            response.setContentType("application/octet-stream");
            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);

            OutputStream out = response.getOutputStream();
            int n = 0;
            long readLength = 0;
            int bsize = 1024;
            byte[] bytes = new byte[bsize];
            if (rangeSwitch == 2) {
                // 针对 bytes=27000-39000 的请求，从27000开始写数据
                while (readLength <= contentLength - bsize) {
                    n = bis.read(bytes);
                    readLength += n;
                    out.write(bytes, 0, n);
                }
                if (readLength <= contentLength) {
                    n = bis.read(bytes, 0, (int) (contentLength - readLength));
                    out.write(bytes, 0, n);
                }
            } else {
                while ((n = bis.read(bytes)) != -1) {
                    out.write(bytes, 0, n);
                }
            }
            out.flush();
            out.close();
            bis.close();
        } catch (IOException ie) {
            // 忽略 ClientAbortException 之类的异常
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载文件
     *
     * @param ID
     * @return
     * @throws Exception
     */
    @GetMapping("/download/{base64}")
    public ResponseEntity<byte[]> download(@PathVariable("base64") String base64) throws Exception {
        if (isEmpty(base64)) {
            return null;
        }
        InputStream inputStream = null;
        byte[] body = null;
        try {
            //base64解密 获得url信息
            String[] paths = new String(Base64.decode(base64), "UTF-8").split("@@@");
            String filePath = paths[0];
            String fileName = paths[1];
            //获得文件
            File file = new File(Properties.FILE_DIR + filePath + File.separator + fileName);
            if (!file.exists()) {
                return null;
            }
            inputStream = new FileInputStream(file);
            body = new byte[inputStream.available()];
            inputStream.read(body);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attchement;filename=" + fileName);
            headers.setContentDispositionFormData("download", new String(fileName.getBytes("UTF-8"), "ISO8859-1"));
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(body, headers, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 上传base64图片
     *
     * @param nameField
     * @param valField
     * @param mapParam
     * @param request
     * @return
     * @throws Exception
     */
    @CrossOrigin(maxAge = 3600)
    @PostMapping("/uploadBase64Imgage")
    @ResponseBody
    public String uploadBase64Imgage(String[] fileNames, String[] uploadImg, @RequestParam Map<String, Object> mapParam, HttpServletRequest request) throws Exception {
        JSONObject jsonObject = new JSONObject();
        try {
            String uploadToken = toString(mapParam.get("uploadToken"));
            //验证token
            CheckResult checkResult = TokenUtil.validateJWT(uploadToken);
            if (!checkResult.isSuccess()) {
                throw new NullPointerException("token错误");
            }

            int fileCount = isEmpty(mapParam.get("fileCount")) ? uploadImg.length : Integer.parseInt(toString(mapParam.get("fileCount")));
            if (uploadImg == null || uploadImg.length == 0) {
                throw new NullPointerException("上传文件不能为空");
            }
            //如果上传数量为1个 但是别数组为2个的时候拼接
            if (fileCount == 1 && uploadImg.length == 2) {
                uploadImg[0] = uploadImg[0] + "," + uploadImg[1];
                uploadImg[1] = null;
            }
            String typeCode = toString(mapParam.get("SF_TYPE_CODE"));
            String extendName = toString(mapParam.get("SF_EXTEND_NAME"));
            //保存路径
            String dir = Properties.FILE_DIR;
            String filepath = typeCode + "/" + (isEmpty(extendName) ? "" : extendName + "/") + FileUtil.getDate(FORMAT2) + "/";


            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < uploadImg.length; i++) {
                if (uploadImg[i] == null) {
                    continue;
                }
                String ID = fileNames[i];
                MultipartFile file = FileUtil.base64ToMultipart(uploadImg[i]);

                JSONObject fileObject = saveFile(file, ID, filepath);
                jsonArray.add(fileObject);
            }

            jsonObject.put("imageArray", jsonArray);
            jsonObject.put("code", 1);
        } catch (Exception e) {
            jsonObject.put("code", 0);
            jsonObject.put("message", e.getMessage());
        }
        return jsonObject.toString();
    }

    /**
     * 上传文件
     *
     * @param mapParam
     * @param request
     * @return
     * @throws IOException
     */
    @CrossOrigin(maxAge = 3600)
    @PostMapping("/upload")
    @ResponseBody
    public String upload(@RequestParam Map<String, Object> mapParam, HttpServletRequest request) throws Exception {
        JSONObject jsonObject = new JSONObject();
        try {
            String uploadToken = toString(mapParam.get("uploadToken"));
            //验证token
            CheckResult checkResult = TokenUtil.validateJWT(uploadToken);
            if (!checkResult.isSuccess()) {
                throw new NullPointerException("token错误");
            }
            MultipartFile file = FileUtil.getMultipartFile(request);

            String typeCode = toString(mapParam.get("SF_TYPE_CODE"));
            String extendName = toString(mapParam.get("SF_EXTEND_NAME"));
            //保存路径
            String dir = Properties.FILE_DIR;
            String filepath = typeCode + File.separator + (isEmpty(extendName) ? "" : extendName + File.separator) + FileUtil.getDate(FORMAT2) + File.separator;

            String ID = toString(mapParam.get("fileName"));

            JSONObject fileObject = saveFile(file, ID, filepath);
            //判断是否有缩略图
            String suffix = toString(fileObject.get("SF_SUFFIX"));
            if ("mp4".equals(suffix) || "mov".equals(suffix)) {
                //保存缩略图
                FileUtil.getVideoPic(toString(fileObject.get("absoluteFile")), dir + filepath.concat(File.separator).concat(ID + "-thumbnails.jpg"));
            }

            jsonObject.put("code", 1);
            jsonObject.put("message", fileObject);
        } catch (Exception e) {
            jsonObject.put("code", 0);
            jsonObject.put("message", e.getMessage());
        }
        return jsonObject.toString();
    }

    /**
     * 保存文件
     *
     * @param file
     * @param ID
     * @param filepath
     * @return
     */
    public JSONObject saveFile(MultipartFile file, String ID, String filepath) {
        JSONObject fileObject = new JSONObject();

        String dir = Properties.FILE_DIR;
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String fileName = ID + "." + suffix;

        OutputStream os = null;
        InputStream is = null;
        BufferedOutputStream bos = null;
        File dest = null;
        try {
            is = file.getInputStream();
            dest = new File(dir + filepath);
            if (!dest.exists()) {
                dest.mkdirs();
            }
            dest = new File(dir + filepath.concat(File.separator).concat(fileName));
            os = new FileOutputStream(dest);
            bos = new BufferedOutputStream(os);
            byte[] buffer = new byte[1024 * 4];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
            //得到绝对路径
            fileObject.put("absoluteFile", dest.getAbsoluteFile());
        } catch (Exception e) {
            throw new NullPointerException("上传文件出错");
        } finally {
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }

        fileObject.put("SF_NAME_NO", ID);
        fileObject.put("SF_PATH", filepath);
        fileObject.put("SF_SUFFIX", suffix);
        fileObject.put("SF_NAME", fileName);
        fileObject.put("SF_ORIGINAL_NAME", originalFilename);
        fileObject.put("SF_SIZE", file.getSize());
        return fileObject;
    }

    public static String toString(Object str) {
        try {
            return str == null ? "" : new String(str.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isEmpty(Object value) {
        return value == null || "".equals(value) || "null".equals(value.toString().toLowerCase());
    }

    public long getLastModified(HttpServletRequest httpServletRequest) {
        return lastModified;
    }
}
