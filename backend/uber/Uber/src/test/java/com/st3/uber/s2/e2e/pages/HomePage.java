    package com.st3.uber.s2.e2e.pages;

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

        @FindBy(id = "history-btn")
        private WebElement historyButton;

        @FindBy(id = "title-ride")
        private WebElement titleRide;


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
            return this.wait.until(ExpectedConditions.textToBePresentInElement(titleRide, "Let's ride!"));
        }


        public void goToDriverHistoryPage() {
            this.wait.until(ExpectedConditions.visibilityOf(historyButton));
            this.actions.moveToElement(historyButton).click().perform();
        }

    }
