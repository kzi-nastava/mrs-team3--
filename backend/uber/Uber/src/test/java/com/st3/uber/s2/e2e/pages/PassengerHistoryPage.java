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

public class PassengerHistoryPage {

    @FindBy(className = "ride-card")
    public List<WebElement> cardList;

    @FindBy(id = "ride-history-title")
    private WebElement rideHistoryTitle;

    @FindBy(className = "modal-overlay")
    private WebElement modalOverlay;

    @FindBy(className = "modal-content")
    private WebElement modalContent;

    @FindBy(className = "review-btn")
    private WebElement reviewButton;

    @FindBy(className = "deadline-card")
    private WebElement deadlineCard;

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    public PassengerHistoryPage(WebDriver webDriver) {
        this.driver = webDriver;
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
        PageFactory.initElements(this.driver, this);
        this.actions = new Actions(this.driver);
    }

    public boolean isOnHistoryPage() {
        try {
            this.wait.until(ExpectedConditions.visibilityOf(rideHistoryTitle));
            return rideHistoryTitle.getText().equals("My Ride History");
        } catch (Exception e) {
            return false;
        }
    }

    public int getRideCount() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
        return cardList.size();
    }

    public void openRideDetails(int rideIndex) {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        if (rideIndex < 0 || rideIndex >= cardList.size()) {
            throw new IndexOutOfBoundsException("Ride index " + rideIndex + " out of bounds. Total rides: " + cardList.size());
        }

        WebElement rideCard = cardList.get(rideIndex);
        wait.until(ExpectedConditions.elementToBeClickable(rideCard));
        actions.moveToElement(rideCard).click().perform();

        wait.until(ExpectedConditions.visibilityOf(modalContent));
    }

    public boolean canCurrentRideBeReviewed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            List<WebElement> reviewButtons = driver.findElements(By.className("review-btn"));

            if (reviewButtons.isEmpty()) {
                return false;
            }

            String buttonText = reviewButtons.get(0).getText().toLowerCase();
            return buttonText.contains("leave a review");

        } catch (Exception e) {
            return false;
        }
    }

    public void clickReviewButton() {
        try {
            List<WebElement> reviewButtons = driver.findElements(By.className("review-btn"));

            if (reviewButtons.isEmpty()) {
                throw new RuntimeException("Review button not found. Make sure modal is open and ride can be reviewed.");
            }

            WebElement reviewBtn = reviewButtons.get(0);
            wait.until(ExpectedConditions.elementToBeClickable(reviewBtn));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", reviewBtn);

            try {
                actions.moveToElement(reviewBtn).click().perform();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reviewBtn);
            }


        } catch (Exception e) {
            throw new RuntimeException("Failed to click review button: " + e.getMessage());
        }
    }

    public void closeModal() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("modal-overlay")));
            WebElement overlay = driver.findElement(By.className("modal-overlay"));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", overlay);

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("modal-overlay")));

        } catch (Exception e) {
            System.out.println("Warning: Could not close modal: " + e.getMessage());
        }
    }

    /**
     * Find first reviewable ride and open review page
     *
     * @return index of the reviewable ride, or -1 if none found
     */
    public int findAndOpenReviewableRide() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        int totalRides = cardList.size();

        for (int i = 0; i < totalRides; i++) {
            try {
                openRideDetails(i);

                if (canCurrentRideBeReviewed()) {
                    clickReviewButton();
                    return i; // Return the index of the reviewable ride
                }

                closeModal();

            } catch (Exception e) {
                closeModal();
            }
        }

        return -1; // No reviewable ride found
    }

    /**
     * Find first expired ride (> 3 days old) and open modal
     *
     * @return true if found and opened, false otherwise
     */
    public boolean findAndOpenExpiredRide() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        int totalRides = cardList.size();

        for (int i = 0; i < totalRides; i++) {
            try {
                openRideDetails(i);

                if (!canCurrentRideBeReviewed()) {
                    return true;
                }

                closeModal();

            } catch (Exception e) {
                try { closeModal(); } catch (Exception ignored) {}
            }
        }

        return false;
    }

    /**
     * Refresh the page and wait for it to fully reload
     * Used after submitting/updating reviews to get fresh data from backend
     */
    public void refreshPage() {
        driver.navigate().refresh();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ride-history-title")));
    }


    /**
     * Check if a ride has already been reviewed
     * Looks for rating stars in the modal
     */
    public boolean isRideAlreadyReviewed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            // Wait for DOM to be ready
            wait.until(driver -> {
                List<WebElement> ratings = driver.findElements(By.tagName("p-rating"));
                return true;
            });

            List<WebElement> ratings = driver.findElements(By.tagName("p-rating"));
            return ratings.size() >= 2;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the driver rating from the modal (when review exists)
     */
    public int getDriverRatingFromModal() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            List<WebElement> detailItems = modalContent.findElements(By.className("detail-item"));

            for (int i = 0; i < detailItems.size(); i++) {
                WebElement item = detailItems.get(i);

                List<WebElement> labels = item.findElements(By.className("detail-label"));
                if (labels.isEmpty()) continue;

                String label = labels.get(0).getText().trim();

                if (label.equalsIgnoreCase("Driver review")) {

                    // Check for "no review" marker first
                    List<WebElement> noReview = item.findElements(By.className("no-review"));
                    if (!noReview.isEmpty()) {
                        return 0;
                    }

                    // Try to find p-rating element
                    List<WebElement> ratings = item.findElements(By.tagName("p-rating"));

                    if (ratings.isEmpty()) {
                        return 0;
                    }

                    WebElement rating = ratings.get(0);

                    // Count active stars by looking for p-rating-option-active divs
                    List<WebElement> activeOptions = rating.findElements(By.cssSelector(".p-rating-option-active"));
                    int activeCount = activeOptions.size();

                    if (activeCount > 0) {
                        return activeCount;
                    }

                    return 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Get the vehicle/ride rating from the modal (when review exists)
     */
    public int getVehicleRatingFromModal() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            // Find all detail-items
            List<WebElement> detailItems = modalContent.findElements(By.className("detail-item"));

            for (int i = 0; i < detailItems.size(); i++) {
                WebElement item = detailItems.get(i);

                List<WebElement> labels = item.findElements(By.className("detail-label"));
                if (labels.isEmpty()) continue;

                String label = labels.get(0).getText().trim();

                if (label.equalsIgnoreCase("Ride review")) {  // Use equalsIgnoreCase instead of equals

                    // Check for "no review" marker first
                    List<WebElement> noReview = item.findElements(By.className("no-review"));
                    if (!noReview.isEmpty()) {
                        return 0;
                    }

                    // Try to find p-rating element
                    List<WebElement> ratings = item.findElements(By.tagName("p-rating"));

                    if (ratings.isEmpty()) {
                        return 0;
                    }

                    WebElement rating = ratings.get(0);

                    // Count active stars by looking for p-rating-option-active divs
                    List<WebElement> activeOptions = rating.findElements(By.cssSelector(".p-rating-option-active"));
                    int activeCount = activeOptions.size();

                    if (activeCount > 0) {
                        return activeCount;
                    }
                    return 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}