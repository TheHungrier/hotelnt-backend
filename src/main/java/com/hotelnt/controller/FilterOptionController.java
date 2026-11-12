package com.hotelnt.controller;

import com.hotelnt.dto.FilterOptionDtos.*;
import com.hotelnt.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/filter-options")
public class FilterOptionController {

    private final FilterOptionService service;

    public FilterOptionController(FilterOptionService service) {
        this.service = service;
    }

    // GET /api/filter-options
    @GetMapping
    public FilterOptionsResponse get() {
        return service.get();
    }
}
