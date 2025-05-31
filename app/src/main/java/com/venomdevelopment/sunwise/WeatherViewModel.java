package com.venomdevelopment.sunwise;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class WeatherViewModel extends ViewModel {

    private MutableLiveData<String> currentTemperature = new MutableLiveData<>();
    private MutableLiveData<String> highTemperature = new MutableLiveData<>();
    private MutableLiveData<String> lowTemperature = new MutableLiveData<>();
    private MutableLiveData<String> description = new MutableLiveData<>();
    private MutableLiveData<String> humidity = new MutableLiveData<>();
    private MutableLiveData<String> wind = new MutableLiveData<>();
    private MutableLiveData<String> precipitation = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> hourlyGraphData = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> dailyGraphDataDay = new MutableLiveData<>();
    private MutableLiveData<LineGraphSeries<DataPoint>> dailyGraphDataNight = new MutableLiveData<>();

    public LiveData<String> getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(String temperature) {
        this.currentTemperature.setValue(temperature);
    }

    public LiveData<String> getHighTemperature() {
        return highTemperature;
    }

    public void setHighTemperature(String highTemperature) {
        this.highTemperature.setValue(highTemperature);
    }

    public LiveData<String> getLowTemperature() {
        return lowTemperature;
    }

    public void setLowTemperature(String lowTemperature) {
        this.lowTemperature.setValue(lowTemperature);
    }

    public LiveData<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description.setValue(description);
    }

    public LiveData<String> getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity.setValue(humidity);
    }

    public LiveData<String> getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind.setValue(wind);
    }

    public LiveData<String> getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(String precipitation) {
        this.precipitation.setValue(precipitation);
    }

    public LiveData<LineGraphSeries<DataPoint>> getHourlyGraphData() {
        return hourlyGraphData;
    }

    public void setHourlyGraphData(LineGraphSeries<DataPoint> hourlyGraphData) {
        this.hourlyGraphData.setValue(hourlyGraphData);
    }

    public LiveData<LineGraphSeries<DataPoint>> getDailyGraphDataDay() {
        return dailyGraphDataDay;
    }

    public void setDailyGraphDataDay(LineGraphSeries<DataPoint> dailyGraphDataDay) {
        this.dailyGraphDataDay.setValue(dailyGraphDataDay);
    }

    public LiveData<LineGraphSeries<DataPoint>> getDailyGraphDataNight() {
        return dailyGraphDataNight;
    }

    public void setDailyGraphDataNight(LineGraphSeries<DataPoint> dailyGraphDataNight) {
        this.dailyGraphDataNight.setValue(dailyGraphDataNight);
    }
}