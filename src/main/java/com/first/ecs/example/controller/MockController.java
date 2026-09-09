package com.first.ecs.example.controller;

import com.first.ecs.example.pojo.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MockController {

    @GetMapping("/mock/comments")
    public List<Root> getAllRoot(){

        return List.of(new Root(500,123,"Server-Down","abc@xec"
                ,"Not able to communicate to main service. This is Mock Service" +
                ", once server is up you will get proper response")) ;
    }
}
