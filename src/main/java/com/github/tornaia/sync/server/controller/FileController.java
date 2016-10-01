package com.github.tornaia.sync.server.controller;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.shared.api.DeleteFileRequest;
import com.github.tornaia.sync.shared.util.FileSizeUtils;

@RestController
public class FileController {
	
	@Autowired
    private MongoOperations mongoOperations;
	
	private FileRepository fileRepo;

    @RequestMapping(value = "/api/file", method = RequestMethod.PUT)
    public void putFile(@RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile) throws IOException {
        String relativePathWithinSyncDir = multipartFile.getOriginalFilename();
        byte[] bytes = multipartFile.getBytes();
        fileRepo.save(new File(relativePathWithinSyncDir, bytes));
        System.out.println("PUT file: " + relativePathWithinSyncDir + " (" + FileSizeUtils.toReadableFileSize(bytes.length) + ")");
    }
    
    @RequestMapping(value = "/api/file", method = RequestMethod.DELETE)
    public void deleteFile(DeleteFileRequest deleteDirectoryRequest) throws IOException {
        String userid = deleteDirectoryRequest.getUserid();
        String relativePath = deleteDirectoryRequest.getRelativePath();

		File findOne = mongoOperations.findOne(Query.query(Criteria.where("path").is(relativePath)), File.class);
		if (findOne != null) {
			fileRepo.delete(findOne);
		}
        System.out.println("DELETE file: " + relativePath);
    }
    
    @RequestMapping(value = "/api/file", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@RequestParam("userid") String userid, @RequestParam("path") String path) throws IOException {
        
    	File findOne = mongoOperations.findOne(Query.query(Criteria.where("path").is(path)), File.class);
    	
    	java.io.File file = new java.io.File(findOne.getPath());
    	FileUtils.writeByteArrayToFile(file, findOne.getData());
    	
    	
        ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=file.jar");
        
        System.out.println("GET file: " + findOne + ")");
        
        return response.build();
        
        
    }

}
