package com.first.ecs.example.controller;

import com.first.ecs.example.RootService;
import com.first.ecs.example.pojo.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/ecs")
@RequiredArgsConstructor
public class ECSController {
    private final RootService rootService;

    @GetMapping("/getAll")
    public List<String> getAll(){

        return List.of("One","Two","Three","Four","Five");
    }
    @GetMapping("/root/getAll")
    public List<Root> getAllRoot(){
        return rootService.getRoot();
    }
}
