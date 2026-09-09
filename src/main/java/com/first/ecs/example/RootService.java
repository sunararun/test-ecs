package com.first.ecs.example;

import com.first.ecs.example.config.AppConfiguration;
import com.first.ecs.example.pojo.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RootService {
    private final RestTemplate restTemplate;
    private final  AppConfiguration appConfiguration;
    public List<Root> getRoot() {
        ResponseEntity<List<Root>> response = restTemplate.exchange(
                appConfiguration.getServiceUrl(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Root>>() {}
        );

        return response.getBody();
    }
}
