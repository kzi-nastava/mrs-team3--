package com.st3.uber.e2e.s1.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePageUnregistered {

    @FindBy(id = "login-btn")
    private WebElement loginButton;


    private WebDriver driver;
    private WebDriverWait wait;
    private static final String PATH = "http://localhost:4200/";


    public HomePageUnregistered(WebDriver webDriver) {
        this.driver = webDriver;
        PageFactory.initElements(this.driver, this);
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
        this.driver.navigate().to(PATH);

    }

    public void clickLoginButton() {
        this.loginButton.click();
    }





}
