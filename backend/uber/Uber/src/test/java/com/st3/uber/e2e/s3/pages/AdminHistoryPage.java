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

public class AdminHistoryPage {
  @FindBy(className = "from-date-admin")
  private WebElement fromDateInput;

  @FindBy(className = "to-date-admin")
  private WebElement toDateInput;

  private WebDriver driver;
  private WebDriverWait wait;
  private Actions actions;

  public AdminHistoryPage(WebDriver webDriver) {
    this.driver = webDriver;
    this.wait = new WebDriverWait(this.driver, java.time.Duration.ofSeconds(10));
    PageFactory.initElements(this.driver, this);
    this.actions = new Actions(this.driver);
  }

}
