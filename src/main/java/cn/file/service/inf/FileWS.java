package cn.file.service.inf;

import cn.file.Entity.CxfFileWrapper;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;



@WebService(name = "FileWS", serviceName = "FileWS")
public interface FileWS {
	 /**
	  * 文件上传
	  * @param file
	  * @return
	  * @author 王丽辉 2017-2-20 下午6:11:11
	  */
	@WebMethod(operationName="upload")
    boolean upload(@WebParam(name = "file") CxfFileWrapper file);

	/**
	 * 文件下载
	 * @param file
	 * @return
	 * @author 王丽辉 2017-2-20 下午6:11:00
	 */
	@WebMethod(operationName="download")
    CxfFileWrapper download(@WebParam(name = "file") CxfFileWrapper file);
    
	/**
	 * 文件删除
	 * @param file
	 * @return
	 * @author 王丽辉 2017-2-20 下午6:10:47
	 */
	@WebMethod(operationName="delete")
    boolean delete(@WebParam(name = "file") CxfFileWrapper file);
    
}