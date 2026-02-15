package com.st3.uber.e2e.s3.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {
    @FindBy(id = "email")
    private WebElement email;

    @FindBy(id = "password")
    private WebElement password;

    @FindBy(css = "#loginButton button")
    private WebElement loginButton;

    @FindBy(id = "welcome-back-text")
    private WebElement welcomeBack;

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    public LoginPage(WebDriver webDriver) {
        this.driver = webDriver;
        this.wait = new WebDriverWait(this.driver, java.time.Duration.ofSeconds(10));
        PageFactory.initElements(this.driver, this);
        this.actions = new Actions(this.driver);
    }

    public void login(String emailTxt, String passwordTxt) {
        this.wait.until(ExpectedConditions.visibilityOf(email)).clear();
        this.actions.moveToElement(email).click().sendKeys(emailTxt).perform();

        this.wait.until(ExpectedConditions.visibilityOf(password)).clear();
        this.actions.moveToElement(password).click().sendKeys(passwordTxt).perform();

        this.wait.until(ExpectedConditions.visibilityOf(loginButton));
        this.actions.moveToElement(loginButton).click().perform();

    }

    public boolean isOnLoginPage() {
        return this.wait.until(ExpectedConditions.textToBePresentInElement(welcomeBack, "Welcome Back"));
    }
}
