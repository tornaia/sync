package com.github.tornaia.sync.server.controller;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.*;

import com.github.tornaia.sync.server.data.document.Folder;
import com.github.tornaia.sync.server.data.repository.FolderRepository;

@RestController
public class FolderController {

	@Resource
	private FolderRepository folderRepository;

	@RequestMapping(value = "/folders/{id}", method = RequestMethod.GET, produces = "application/json", consumes = "application/json")
	public Folder getFolder(@PathVariable("id") String id) {
		return folderRepository.findOne(id);
	}

	@RequestMapping(value = "/folders", method = RequestMethod.POST)
	public Folder postFolder(@RequestBody Folder folder) {
		Folder savedFolder = folderRepository.insert(folder);
		return savedFolder;
	}

}
