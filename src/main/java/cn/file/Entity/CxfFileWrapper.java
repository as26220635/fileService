package cn.file.Entity;
import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlMimeType;


public class CxfFileWrapper {
    //文件名
    private String fileName = null;
    //文件相对路径
    private String filePath = null;
    //文件MD5值
    private String fileMD5 = null;
    // 文件
    private DataHandler file = null;
    //令牌
    private String fileToken = null;

    public final String getFileName() {
        return fileName;
    }

    public final void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public String getFileMD5() {
		return fileMD5;
	}

	public void setFileMD5(String fileMD5) {
		this.fileMD5 = fileMD5;
	}

	public String getFileToken() {
		return fileToken;
	}

	public void setFileToken(String fileToken) {
		this.fileToken = fileToken;
	}

	// 二进制文件流
    @XmlMimeType("application/octet-stream")
    public final DataHandler getFile() {
        return file;
    }

    public final void setFile(DataHandler file) {
        this.file = file;
    }
}