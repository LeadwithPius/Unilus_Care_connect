

package com.unilus.careconnect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HealthController {
@GetMapping("/api/health")
public String checkHealth() {
    return "CareConnect backend is running!";
}
}
