package dev.zhipei.businessops.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController { @GetMapping("/") String dashboard() { return "index"; } }
