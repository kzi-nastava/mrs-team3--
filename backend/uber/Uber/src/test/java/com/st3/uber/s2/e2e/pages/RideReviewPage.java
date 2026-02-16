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
import java.util.List;

public class RideReviewPage {

    private static final int DRIVER_RATING_SECTION_INDEX = 0;
    private static final int VEHICLE_RATING_SECTION_INDEX = 1;

    @FindBy(className = "back-btn")
    private WebElement backButton;

    @FindBy(className = "review-header")
    private WebElement reviewHeader;

    @FindBy(className = "loading-state")
    private WebElement loadingState;

    @FindBy(className = "error-card")
    private WebElement errorCard;

    @FindBy(className = "deadline-card")
    private WebElement deadlineCard;

    @FindBy(className = "btn-submit")
    private WebElement submitButton;

    @FindBy(className = "success-banner")
    private WebElement successBanner;

    @FindBy(css = "textarea[name='comment']")
    private WebElement commentTextarea;

    @FindBy(className = "char-counter")
    private WebElement charCounter;

    @FindBy(className = "readonly-review")
    private WebElement readonlyReview;

    @FindBy(className = "existing-review-notice")
    private WebElement existingReviewNotice;

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    public RideReviewPage(WebDriver webDriver) {
        this.driver = webDriver;
        PageFactory.initElements(this.driver, this);
        this.actions = new Actions(this.driver);
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
    }

    public boolean isOnReviewPage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(reviewHeader));
            return reviewHeader.getText().contains("Review Your Ride");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canReview() {
        try {
            wait.until(ExpectedConditions.visibilityOf(deadlineCard));
            String deadlineText = deadlineCard.getText().toLowerCase();
            return deadlineText.contains("remaining") && !deadlineText.contains("expired");
        } catch (Exception e) {
            return false;
        }
    }

    public void setDriverRating(int rating) {
        setRatingForSection(DRIVER_RATING_SECTION_INDEX, rating);
    }

    public void setVehicleRating(int rating) {
        setRatingForSection(VEHICLE_RATING_SECTION_INDEX, rating);
    }

    private void setRatingForSection(int sectionIndex, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        List<WebElement> ratingSections = driver.findElements(By.className("rating-section"));

        if (sectionIndex >= ratingSections.size()) {
            throw new IllegalStateException("Rating section index " + sectionIndex + " not found");
        }

        WebElement ratingSection = ratingSections.get(sectionIndex);
        List<WebElement> stars = ratingSection.findElements(By.className("star-btn"));

        wait.until(ExpectedConditions.elementToBeClickable(stars.get(rating - 1)));
        actions.moveToElement(stars.get(rating - 1)).click().perform();
    }

    public int getDriverRating() {
        return getRatingForSection(DRIVER_RATING_SECTION_INDEX);
    }

    public int getVehicleRating() {
        return getRatingForSection(VEHICLE_RATING_SECTION_INDEX);
    }

    /**
     * Waits until the filled-star count in the given rating section stabilizes using WebDriverWait.
     */
    private int getRatingForSection(int sectionIndex) {
        // First wait for rating sections to be present
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("rating-section")));

        // Custom wait condition: star count stabilizes
        try {
            return wait.until(driver -> {
                List<WebElement> sections = driver.findElements(By.className("rating-section"));
                if (sections.size() <= sectionIndex) {
                    return null; // Section not found yet
                }

                int currentCount = sections.get(sectionIndex)
                        .findElements(By.cssSelector(".star-btn.filled"))
                        .size();

                // Double-check immediately to see if it's stable
                int recheckCount = driver.findElements(By.className("rating-section"))
                        .get(sectionIndex)
                        .findElements(By.cssSelector(".star-btn.filled"))
                        .size();

                // If counts match, Angular has settled
                return (currentCount == recheckCount) ? currentCount : null;
            });
        } catch (Exception e) {
            return 0;
        }
    }

    public void setComment(String comment) {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));

        // Use JavaScript to clear and set value to trigger Angular's change detection
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = ''; arguments[0].dispatchEvent(new Event('input'));",
                commentTextarea
        );

        if (comment != null && !comment.isEmpty()) {
            commentTextarea.sendKeys(comment);
        }
    }

    public String getComment() {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        return commentTextarea.getAttribute("value");
    }

    public int getCharacterCount() {
        wait.until(ExpectedConditions.visibilityOf(charCounter));
        String text = charCounter.getText();
        String[] parts = text.split(" / ");
        return Integer.parseInt(parts[0].trim());
    }

    public void submitReview() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        actions.moveToElement(submitButton).click().perform();
    }

    public boolean isSubmitButtonEnabled() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("btn-submit")));
            return submitButton.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSuccessMessageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(successBanner));
            return successBanner.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasExistingReview() {
        try {
            return existingReviewNotice.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void goBack() {
        wait.until(ExpectedConditions.elementToBeClickable(backButton));
        actions.moveToElement(backButton).click().perform();
    }
}