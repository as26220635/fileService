package cn.file.service.impl;

import cn.file.Entity.CheckResult;
import cn.file.Entity.CxfFileWrapper;
import cn.file.overall.Properties;
import cn.file.service.inf.FileWS;
import cn.file.util.FileUtil;
import cn.file.util.TokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.*;

@Service
@Component("fileWS")
public class FileWSImpl extends BaseServiceImpl implements FileWS {
    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @Override
    public boolean upload(CxfFileWrapper file) {

        System.out.println("上传文件:" + file.getFileName());

        boolean result = true;
        OutputStream os = null;
        InputStream is = null;
        BufferedOutputStream bos = null;
        File dest = null;
        try {
            //验证令牌
            CheckResult checkResult = TokenUtil.validateJWT(file.getFileToken());
            if (!checkResult.isSuccess()) {
                result = false;
                return result;
            }
            String dir = Properties.FILE_DIR;
            is = file.getFile().getInputStream();
            dest = new File(dir + file.getFilePath());
            if (!dest.exists()) {
                dest.mkdirs();
            }
            dest = new File(dir + file.getFilePath().concat(File.separator).concat(file.getFileName()));
            os = new FileOutputStream(dest);
            bos = new BufferedOutputStream(os);
            byte[] buffer = new byte[1024 * 4];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
            try {
                String fileMD5 = FileUtil.getMd5ByFile(dest);
                System.out.println("接收文件MD5值" + file.getFileMD5());
                System.out.println("本地文件MD5值" + fileMD5);
                //Md5值不同删除文件
                if (!fileMD5.equals(file.getFileMD5())) {
                    result = false;
                    this.delete(file);
                }
            } catch (FileNotFoundException e4) {
                e4.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }

        }
        return result;
    }

    /**
     * 文件下载
     *
     * @param file
     * @return
     */
    @Override
    public CxfFileWrapper download(CxfFileWrapper file) {
        CxfFileWrapper fileWrapper = new CxfFileWrapper();
        //验证令牌
        CheckResult checkResult = TokenUtil.validateJWT(file.getFileToken());
        if (!checkResult.isSuccess()) {
            return fileWrapper;
        }
        String dir = Properties.FILE_DIR;
        try {
            File dest = new File(dir + file.getFilePath().concat(File.separator).concat(file.getFileName()));
            if (!(dest.isFile() && dest.exists())) {
                return fileWrapper;
            }
            DataSource source = new FileDataSource(dest);
            fileWrapper.setFile(new DataHandler(source));
            fileWrapper.setFileToken("true");
        } catch (Exception e) {
            e.printStackTrace();
            fileWrapper.setFileToken("false");
            return fileWrapper;
        }
        return fileWrapper;
    }

    /**
     * 文件删除
     *
     * @param file
     * @return
     */
    @Override
    public boolean delete(CxfFileWrapper file) {
        boolean result = false;
        try {
            //验证令牌
            CheckResult checkResult = TokenUtil.validateJWT(file.getFileToken());
            if (!checkResult.isSuccess()) {
                return result;
            }
            String dir = Properties.FILE_DIR;
            File dest = new File(dir + file.getFilePath().concat(File.separator).concat(file.getFileName()));
            if (dest.isFile() && dest.exists()) {
                result = dest.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

}