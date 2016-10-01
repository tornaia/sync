package com.github.tornaia.sync.server.controller;

import com.github.tornaia.sync.server.data.document.Folder;
import com.github.tornaia.sync.server.data.repository.FolderRepository;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
public class FolderController {

    @Resource
    private FolderRepository folderRepository;

    @RequestMapping(value = "/folders/{id}", method = RequestMethod.GET)
    public Folder getFolder(@PathVariable("id") String id) {
        return folderRepository.findOne(id);
    }

    @RequestMapping(value = "/folders", method = RequestMethod.POST)
    public Folder postFolder(@RequestBody Folder folder) {
        Folder savedFolder = folderRepository.insert(folder);
        return savedFolder;
    }
}
