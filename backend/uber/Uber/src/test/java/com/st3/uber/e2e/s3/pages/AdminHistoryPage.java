package com.st3.uber.e2e.s3.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AdminHistoryPage {

  @FindBy(css = "[data-testid='from-date-trigger']")
  private WebElement fromDateTrigger;

  @FindBy(css = "[data-testid='to-date-trigger']")
  private WebElement toDateTrigger;

  @FindBy(css = "[data-testid='date-picker']")
  private WebElement datePicker;

  @FindBy(css = "[data-testid='sort-select']")
  private WebElement sortSelect;

  private WebDriver driver;
  private WebDriverWait wait;
  private Actions actions;

  public AdminHistoryPage(WebDriver webDriver) {
    this.driver = webDriver;
    this.wait = new WebDriverWait(this.driver, java.time.Duration.ofSeconds(10));
    PageFactory.initElements(this.driver, this);
    this.actions = new Actions(this.driver);
  }

  public void selectFromDate(String isoDate) {
    this.wait.until(ExpectedConditions.elementToBeClickable(fromDateTrigger));
    this.actions.moveToElement(fromDateTrigger).click().perform();

    this.wait.until(ExpectedConditions.visibilityOf(datePicker));

    WebElement day = this.wait.until(ExpectedConditions.elementToBeClickable(
        By.cssSelector("[data-testid='dp-day-" + isoDate + "']")
    ));

    this.actions.moveToElement(day).click().perform();

    this.wait.until(ExpectedConditions.invisibilityOf(datePicker));
  }

  public void selectToDate(String isoDate) {
    this.wait.until(ExpectedConditions.elementToBeClickable(toDateTrigger));
    this.actions.moveToElement(toDateTrigger).click().perform();

    this.wait.until(ExpectedConditions.visibilityOf(datePicker));

    WebElement day = this.wait.until(ExpectedConditions.elementToBeClickable(
        By.cssSelector("[data-testid='dp-day-" + isoDate + "']")
    ));

    this.actions.moveToElement(day).click().perform();

    this.wait.until(ExpectedConditions.invisibilityOf(datePicker));
  }

  /**
   * value examples:
   * - "startTime|desc"
   * - "price|asc"
   * - "panic|desc"
   */
  public void sortByValue(String value) {
    this.wait.until(ExpectedConditions.elementToBeClickable(sortSelect));

    Select select = new Select(sortSelect);
    String oldValue = select.getFirstSelectedOption().getAttribute("value");

    // already selected -> no action
    if (value != null && value.equals(oldValue)) return;

    // keep a reference to first card (if any) to detect refresh
    WebElement firstBefore = this.driver.findElements(By.cssSelector(".ride-card"))
        .stream().findFirst().orElse(null);

    // select option
    select.selectByValue(value);

    // wait select truly changed (stable)
    this.wait.until(d -> {
      Select s = new Select(sortSelect);
      return value.equals(s.getFirstSelectedOption().getAttribute("value"));
    });

    // wait list refresh
    if (firstBefore != null) {
      try {
        this.wait.until(ExpectedConditions.stalenessOf(firstBefore));
      } catch (TimeoutException ignored) {
        // fallback: wait at least something is rendered (cards or empty state)
        this.wait.until(d ->
            !d.findElements(By.cssSelector(".ride-card")).isEmpty()
                || !d.findElements(By.cssSelector(".empty-state")).isEmpty()
        );
      }
    } else {
      this.wait.until(d ->
          !d.findElements(By.cssSelector(".ride-card")).isEmpty()
              || !d.findElements(By.cssSelector(".empty-state")).isEmpty()
      );
    }
  }

  // Optional: helper if you want to assert page is open
  public boolean isOnAdminHistoryPage() {
    return !this.driver.findElements(By.cssSelector(".admin-history-container")).isEmpty();
  }
}
