package com.example.iMeetBE.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OAuth2ErrorController {

    @GetMapping("/login")
    public RedirectView loginError(@RequestParam(value = "error", required = false) String error) {
        if ("true".equals(error)) {
            // Log the error and redirect to frontend with error information
            
            // Redirect to frontend login page with error
            return new RedirectView("https://imeeet.netlify.app/login?oauth_error=true"); // Default to production
        }
        
        // Normal login redirect
        return new RedirectView("https://imeeet.netlify.app/login"); // Default to production
    }
}