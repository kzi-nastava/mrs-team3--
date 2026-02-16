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

    public boolean isLoadingDisplayed() {
        try {
            return loadingState.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isErrorDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(errorCard));
            return errorCard.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        wait.until(ExpectedConditions.visibilityOf(errorCard));
        return errorCard.getText();
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

    public boolean isReviewExpired() {
        try {
            wait.until(ExpectedConditions.visibilityOf(deadlineCard));
            return deadlineCard.getText().toLowerCase().contains("expired");
        } catch (Exception e) {
            return false;
        }
    }

    public int getDaysRemaining() {
        wait.until(ExpectedConditions.visibilityOf(deadlineCard));
        String text = deadlineCard.getText();

        // Extract number before "day" or "days"
        String[] parts = text.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("day") || parts[i].equals("days")) {
                try {
                    return Integer.parseInt(parts[i - 1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void setDriverRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        List<WebElement> ratingSections = driver.findElements(By.className("rating-section"));
        WebElement driverRatingSection = ratingSections.get(0); // First rating section is for driver

        List<WebElement> stars = driverRatingSection.findElements(By.className("star-btn"));

        wait.until(ExpectedConditions.elementToBeClickable(stars.get(rating - 1)));
        actions.moveToElement(stars.get(rating - 1)).click().perform();
    }

    public void setVehicleRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        List<WebElement> ratingSections = driver.findElements(By.className("rating-section"));
        WebElement vehicleRatingSection = ratingSections.get(1); // Second rating section is for vehicle

        List<WebElement> stars = vehicleRatingSection.findElements(By.className("star-btn"));

        wait.until(ExpectedConditions.elementToBeClickable(stars.get(rating - 1)));
        actions.moveToElement(stars.get(rating - 1)).click().perform();
    }

    public int getDriverRating() {
        return getRatingForSection(0);
    }

    public int getVehicleRating() {
        return getRatingForSection(1);
    }

    /**
     * Waits until the filled-star count in the given rating section stabilises,
     * then returns it. This handles Angular's async class binding ([class.filled]).
     */
    private int getRatingForSection(int sectionIndex) {
        // Short poll: re-read until the count is stable for two consecutive reads
        int last = -1;
        long deadline = System.currentTimeMillis() + 3000; // 3s max
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> sections = driver.findElements(By.className("rating-section"));
            if (sections.size() > sectionIndex) {
                int current = sections.get(sectionIndex)
                        .findElements(By.cssSelector(".star-btn.filled")).size();
                if (current == last) {
                    return current; // stable — Angular change detection has settled
                }
                last = current;
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return last < 0 ? 0 : last;
    }

    public void setComment(String comment) {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        commentTextarea.clear();
        commentTextarea.sendKeys(comment);
    }

    public String getComment() {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        return commentTextarea.getAttribute("value");
    }

    public int getCharacterCount() {
        wait.until(ExpectedConditions.visibilityOf(charCounter));
        String text = charCounter.getText();
        // Extract number before " /"
        String[] parts = text.split(" / ");
        return Integer.parseInt(parts[0].trim());
    }

    public void submitReview() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        actions.moveToElement(submitButton).click().perform();
    }

    public boolean isSubmitButtonEnabled() {
        try {
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

    public String getSuccessMessage() {
        wait.until(ExpectedConditions.visibilityOf(successBanner));
        return successBanner.getText();
    }

    public boolean hasExistingReview() {
        try {
            return existingReviewNotice.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isReviewReadonly() {
        try {
            return readonlyReview.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getReadonlyDriverRating() {
        wait.until(ExpectedConditions.visibilityOf(readonlyReview));

        List<WebElement> ratingDisplays = readonlyReview.findElements(By.className("rating-display"));
        WebElement driverRating = ratingDisplays.get(0);

        List<WebElement> filledStars = driverRating.findElements(By.cssSelector(".stars-display span.filled"));
        return filledStars.size();
    }

    public int getReadonlyVehicleRating() {
        wait.until(ExpectedConditions.visibilityOf(readonlyReview));

        List<WebElement> ratingDisplays = readonlyReview.findElements(By.className("rating-display"));
        WebElement vehicleRating = ratingDisplays.get(1);

        List<WebElement> filledStars = vehicleRating.findElements(By.cssSelector(".stars-display span.filled"));
        return filledStars.size();
    }

    public String getReadonlyComment() {
        wait.until(ExpectedConditions.visibilityOf(readonlyReview));

        try {
            WebElement commentDisplay = readonlyReview.findElement(By.className("comment-display"));
            return commentDisplay.findElement(By.tagName("p")).getText();
        } catch (Exception e) {
            return ""; // No comment
        }
    }

    public void goBack() {
        wait.until(ExpectedConditions.elementToBeClickable(backButton));
        actions.moveToElement(backButton).click().perform();
    }

    public String getRideId() {
        String url = driver.getCurrentUrl();
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    public boolean isMapDisplayed() {
        try {
            WebElement map = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("reviewMap")));
            return map.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getDriverName() {
        try {
            List<WebElement> infoItems = driver.findElements(By.className("info-item"));
            for (WebElement item : infoItems) {
                String label = item.findElement(By.className("info-label")).getText();
                if (label.equals("Driver")) {
                    return item.findElement(By.className("info-value")).getText();
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public String getVehicleType() {
        try {
            List<WebElement> infoItems = driver.findElements(By.className("info-item"));
            for (WebElement item : infoItems) {
                String label = item.findElement(By.className("info-label")).getText();
                if (label.equals("Vehicle Type")) {
                    return item.findElement(By.className("info-value")).getText();
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public void waitForSuccessMessageToDisappear() {
        try {
            wait.until(ExpectedConditions.invisibilityOf(successBanner));
        } catch (Exception e) {
            // Already disappeared or never appeared
        }
    }

    public boolean isReviewFormVisible() {
        try {
            WebElement reviewFormCard = driver.findElement(By.className("review-form-card"));
            return reviewFormCard.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}