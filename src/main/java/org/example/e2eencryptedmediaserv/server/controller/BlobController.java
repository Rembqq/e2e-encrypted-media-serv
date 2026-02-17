package org.example.e2eencryptedmediaserv.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BlobController {

    @GetMapping("/helath")
    public String health() {
        return "OK";
    }
}
