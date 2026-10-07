package com.first.ecs.example.controller;

import com.first.ecs.example.RootService;
import com.first.ecs.example.log.OpenTelemetryConfig;
import com.first.ecs.example.pojo.Root;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/api/ecs")
@RequiredArgsConstructor
public class ECSController {
    private final RootService rootService;
    private static final Logger log = LoggerFactory.getLogger(ECSController.class);
    private final OpenTelemetryConfig openTelemetryConfig;

    @GetMapping("/getAll")
    public List<String> getAll(){

        return List.of("One","Two","Three","Four","Five");
    }
    @GetMapping("/root/getAll")
    public List<Root> getAllRoot(){
        log.info("Endpoint hit successfully!"); // <-- Traces will appear on this line!
        openTelemetryConfig.doHeavyWork("tenant");
        return rootService.getRoot();
    }
}
