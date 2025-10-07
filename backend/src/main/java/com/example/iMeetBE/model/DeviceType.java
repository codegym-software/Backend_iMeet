package com.example.iMeetBE.model;

public enum DeviceType {
    MIC("Mic"),
    CAM("Cam"),
    LAPTOP("Laptop"),
    BANG("Bảng")    ,
    MAN_HINH("Màn hình"),
    MAY_CHIEU("Máy chiếu"),
    KHAC("Khác");
    
    private final String displayName;
    
    DeviceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
