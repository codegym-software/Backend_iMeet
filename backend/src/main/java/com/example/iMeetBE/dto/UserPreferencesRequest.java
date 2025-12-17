package com.example.iMeetBE.dto;

import java.util.Map;

public class UserPreferencesRequest {
    private Map<String, String> groupColors; // groupId -> color hex
    private String defaultView; // "day", "week", "month"
    private String timezone;
    
    public UserPreferencesRequest() {}
    
    public UserPreferencesRequest(Map<String, String> groupColors, String defaultView, String timezone) {
        this.groupColors = groupColors;
        this.defaultView = defaultView;
        this.timezone = timezone;
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
