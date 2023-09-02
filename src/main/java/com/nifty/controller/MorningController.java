package com.nifty.controller;

import com.nifty.MorningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("nifty")
@RestController
public class MorningController {

    // http://localhost:8099/nifty/reloadCache
    @Autowired
    MorningService morningService;

    @GetMapping("/reloadCache")
    public String reloadCache() {
        morningService.performIntradayTrading();
        return "Cache Reloaded Successfully";
    }

}

