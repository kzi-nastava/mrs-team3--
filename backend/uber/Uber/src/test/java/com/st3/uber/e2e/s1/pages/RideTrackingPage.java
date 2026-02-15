package com.st3.uber.e2e.s1.pages;

import org.hibernate.annotations.processing.Find;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class RideTrackingPage {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;


    @FindBy(id = "badge_status")
    private WebElement rideStatus;


    public RideTrackingPage(WebDriver webDriver) {
        this.driver = webDriver;
        PageFactory.initElements(this.driver, this);
        this.actions = new Actions(this.driver);
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
    }



    public boolean isOnRideTrackingPageAndAccepted() {
        return this.wait.until(ExpectedConditions.textToBePresentInElement(rideStatus, "ACCEPTED"));
    }






}
