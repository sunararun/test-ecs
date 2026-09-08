package com.first.ecs.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/ecs")
public class ECSController {

    @GetMapping("/getAll")
    public List<String> getAll(){
        return List.of("Ganesh","Shiv","Shakti","Laxmi","Parvati");
    }
}
