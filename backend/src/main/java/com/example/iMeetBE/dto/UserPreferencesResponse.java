package com.example.iMeetBE.dto;

import java.util.Map;

public class UserPreferencesResponse {
    private boolean success;
    private String message;
    private Map<String, String> groupColors;
    private String defaultView;
    private String timezone;
    
    public UserPreferencesResponse() {}
    
    public UserPreferencesResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public UserPreferencesResponse(boolean success, String message, UserPreferencesRequest preferences) {
        this.success = success;
        this.message = message;
        if (preferences != null) {
            this.groupColors = preferences.getGroupColors();
            this.defaultView = preferences.getDefaultView();
            this.timezone = preferences.getTimezone();
        }
    }
    
    public UserPreferencesResponse(boolean success, String message, 
                                  Map<String, String> groupColors, 
                                  String defaultView, 
                                  String timezone) {
        this.success = success;
        this.message = message;
        this.groupColors = groupColors;
        this.defaultView = defaultView;
        this.timezone = timezone;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, String> getGroupColors() {
        return groupColors;
    }
    
    public void setGroupColors(Map<String, String> groupColors) {
        this.groupColors = groupColors;
    }
    
    public String getDefaultView() {
        return defaultView;
    }
    
    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
