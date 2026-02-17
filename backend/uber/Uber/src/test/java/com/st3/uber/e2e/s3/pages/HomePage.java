package com.st3.uber.e2e.s3.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HomePage {

    @FindBy(id = "home-btn")
    private WebElement historyButton;

    @FindBy(className = "admin-history-header")
    private WebElement titleAdminHistory;

    @FindBy(className = "profile-container")
    private WebElement profileContainer;

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    public HomePage(WebDriver webDriver) {
        this.driver = webDriver;
        PageFactory.initElements(this.driver, this);
        this.actions = new Actions(this.driver);
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));

    }

    public boolean isLoggedIn() {
        WebElement e = this.wait.until(ExpectedConditions.visibilityOf(profileContainer));
        return e.isDisplayed();
    }

    public void goToRideHistoryPage() {
        this.wait.until(ExpectedConditions.visibilityOf(historyButton));
        this.actions.moveToElement(historyButton).click().perform();
    }

    public boolean isOnHistoryPage() {
        WebElement e = this.wait.until(ExpectedConditions.visibilityOf(titleAdminHistory));
        return e.isDisplayed();
    }

}
