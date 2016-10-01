package com.github.tornaia.sync.server.hello;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.tornaia.sync.server.controller.FolderController;
import com.github.tornaia.sync.server.data.document.Folder;
import com.github.tornaia.sync.server.data.document.Request;
import com.github.tornaia.sync.server.data.repository.FolderRepository;
import com.github.tornaia.sync.server.data.repository.RequestRepository;

@RestController
public class HelloController {
	
	@Autowired
	private RequestRepository requestRepository;
	
	@Autowired
	private FolderRepository folderRepository;
	
    private AtomicInteger counter = new AtomicInteger();

    @RequestMapping("/api/hello")
	public HelloResponse hello() {
		int incremented = counter.getAndIncrement();

		requestRepository.save(new Request(new Date(), incremented));

		// fetch all customers
		System.out.println("Request found with findAll():");
		System.out.println("-------------------------------");
		for (Request customer : requestRepository.findAll()) {
			System.out.println(customer);
		}
		System.out.println();
		
		Folder save = folderRepository.save(new Folder("root"));

		return new HelloResponse("Hello " + save.getId());
	}
}
