package com.inn.cafe.restimpl;

import com.inn.cafe.rest.DashboardRest;
import com.inn.cafe.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardRestImpl implements DashboardRest {

    private final DashboardService dashboardService;

    @Override
    public ResponseEntity<Map<String, Object>> getCount() {
        return dashboardService.getCount();
    }
}
