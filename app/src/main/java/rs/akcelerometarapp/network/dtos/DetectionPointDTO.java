package rs.akcelerometarapp.network.dtos;

import java.util.Date;

/**
 * Created by RADEEE on 08-Oct-15.
 */
public class DetectionPointDTO {

    public double getRmsX() {
        return rmsX;
    }

    public void setRmsX(double rmsX) {
        this.rmsX = rmsX;
    }

    public double getRmsY() {
        return rmsY;
    }

    public void setRmsY(double rmsY) {
        this.rmsY = rmsY;
    }

    public double getRmsZ() {
        return rmsZ;
    }

    public void setRmsZ(double rmsZ) {
        this.rmsZ = rmsZ;
    }

    public double getRmsXYZ() {
        return rmsXYZ;
    }

    public void setRmsXYZ(double rmsXYZ) {
        this.rmsXYZ = rmsXYZ;
    }

    public double getMaxRmsXYZ() {
        return maxRmsXYZ;
    }

    public void setMaxRmsXYZ(double maxRmsXYZ) {
        this.maxRmsXYZ = maxRmsXYZ;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public Date getDetectionTime() {
        return detectionTime;
    }

    public void setDetectionTime(Date detectionTime) {
        this.detectionTime = detectionTime;
    }

    @Override
    public String toString() {
        return "DetectionPointDTO{" +
                "rmsX=" + rmsX +
                ", rmsY=" + rmsY +
                ", rmsZ=" + rmsZ +
                ", rmsXYZ=" + rmsXYZ +
                ", maxRmsXYZ=" + maxRmsXYZ +
                ", speed=" + speed +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", detectionTime=" + detectionTime +
                '}';
    }

    protected double rmsX;
    protected double rmsY;
    protected double rmsZ;
    protected double rmsXYZ;
    protected double maxRmsXYZ;
    protected double speed;
    protected double latitude;
    protected double longitude;
    protected double altitude;
    protected Date detectionTime;
}
