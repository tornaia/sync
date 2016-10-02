package com.github.tornaia.sync.server.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.github.tornaia.sync.server.controller.dto.FileState;
import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.shared.api.DeleteFileRequest;
import com.github.tornaia.sync.shared.util.FileSizeUtils;

@RestController("/api/file")
public class FileController {

	@Resource
	private FileRepository fileRepo;

	@RequestMapping(method = RequestMethod.PUT)
	public void putFile(@RequestParam("userid") String userid, @RequestPart("file") MultipartFile multipartFile)
			throws IOException {
		String path = multipartFile.getOriginalFilename();
		File file = fileRepo.findByPath(path);
		if (file == null) {
			file = new File(path, multipartFile.getBytes(), userid, 1);
			fileRepo.insert(file);
		} else {
			file.setRevision(file.getRevision() + 1);
			fileRepo.save(file);
		}
		System.out.println("PUT file: " + path + " (" + FileSizeUtils.toReadableFileSize(file.getData().length) + ")");
	}

	@RequestMapping(method = RequestMethod.DELETE)
	public void deleteFile(DeleteFileRequest deleteDirectoryRequest) throws IOException {
		String path = deleteDirectoryRequest.getRelativePath();
		File file = fileRepo.findByPath(path);
		if (file == null) {
			throw new FileNotFoundException(path);
		}
		fileRepo.delete(file);
		System.out.println("DELETE file: " + path);
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM)
	public @ResponseBody ResponseEntity getFile(@RequestParam("userid") String userid,
			@RequestParam("path") String path) throws IOException {

		File file = fileRepo.findByPath(path);
		if (file == null) {
			throw new FileNotFoundException(path);
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
		responseHeaders.add("X-FileRevision", file.getRevision().toString());
		System.out.println("GET file: " + file.getPath() + ")");
		return new ResponseEntity(file.getData(), responseHeaders, HttpStatus.OK);
	}

	@RequestMapping(value = "/api/file/state", method = RequestMethod.GET)
	public FileState getFileState(@RequestParam("userid") String userid, @RequestParam("path") String path)
			throws IOException {

		File file = fileRepo.findByPath(path);
		if (file == null) {
			throw new FileNotFoundException(path);
		}

		return new FileState(file);
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	class FileNotFoundException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public FileNotFoundException(String path) {
			super("Could not find file: '" + path + "'.");
		}
	}
}
