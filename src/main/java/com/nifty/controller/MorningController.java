package com.nifty.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("nifty")
@RestController
public class MorningController {

    @GetMapping("/reloadCache")
    public String reloadCache() {
        return "Cache Reloaded Successfully";
    }

}

