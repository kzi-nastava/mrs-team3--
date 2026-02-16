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

    /**
     * Check if we're on the history page
     */
    public boolean isOnHistoryPage() {
        try {
            this.wait.until(ExpectedConditions.visibilityOf(rideHistoryTitle));
            return rideHistoryTitle.getText().equals("My Ride History");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get total number of ride cards displayed
     */
    public int getRideCount() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
        return cardList.size();
    }

    /**
     * Open ride details modal by clicking on a ride card
     */
    public void openRideDetails(int rideIndex) {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        if (rideIndex < 0 || rideIndex >= cardList.size()) {
            throw new IndexOutOfBoundsException("Ride index " + rideIndex + " out of bounds. Total rides: " + cardList.size());
        }

        WebElement rideCard = cardList.get(rideIndex);
        wait.until(ExpectedConditions.elementToBeClickable(rideCard));
        actions.moveToElement(rideCard).click().perform();

        // Wait for modal to appear
        wait.until(ExpectedConditions.visibilityOf(modalContent));
    }

    /**
     * Check if the currently opened ride can be reviewed (< 3 days old)
     * This checks the deadline card in the modal
     */
    public boolean canCurrentRideBeReviewed() {
        try {
            // Wait for modal to be fully visible
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            // Look for the review button
            List<WebElement> reviewButtons = driver.findElements(By.className("review-btn"));

            if (reviewButtons.isEmpty()) {
                return false;
            }

            // Check if button text indicates we can review (not expired)
            String buttonText = reviewButtons.get(0).getText().toLowerCase();
            return buttonText.contains("leave a review") || buttonText.contains("review");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Click the "Leave a review" button in the open modal
     * This navigates to the review page
     */
    public void clickReviewButton() {
        try {
            List<WebElement> reviewButtons = driver.findElements(By.className("review-btn"));

            if (reviewButtons.isEmpty()) {
                throw new RuntimeException("Review button not found. Make sure modal is open and ride can be reviewed.");
            }

            WebElement reviewBtn = reviewButtons.get(0);
            wait.until(ExpectedConditions.elementToBeClickable(reviewBtn));

            // Scroll button into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", reviewBtn);

            // Click using JavaScript if normal click fails
            try {
                actions.moveToElement(reviewBtn).click().perform();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", reviewBtn);
            }

            // Wait for navigation to review page
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to click review button: " + e.getMessage());
        }
    }

    /**
     * Close the currently open modal
     */
    public void closeModal() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("modal-overlay")));
            WebElement overlay = driver.findElement(By.className("modal-overlay"));

            // Click overlay to close modal
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", overlay);

            // Wait for modal to disappear
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("modal-overlay")));

        } catch (Exception e) {
            System.out.println("Warning: Could not close modal: " + e.getMessage());
        }
    }

    /**
     * MAIN METHOD: Find first reviewable ride and open review page
     * This is the primary method tests should use
     *
     * @return true if found and opened, false otherwise
     */
    public boolean findAndOpenReviewableRide() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        int totalRides = cardList.size();
        System.out.println("Checking " + totalRides + " rides for reviewable ones...");

        for (int i = 0; i < totalRides; i++) {
            try {
                System.out.println("  Checking ride " + (i + 1) + "/" + totalRides);

                // Open ride details
                openRideDetails(i);

                // Check if this ride can be reviewed
                if (canCurrentRideBeReviewed()) {
                    System.out.println("  ✓ Found reviewable ride at index " + i);

                    // Click review button
                    clickReviewButton();
                    return true;
                }

                // Can't review this one, close and try next
                System.out.println("  ✗ Ride at index " + i + " cannot be reviewed");
                closeModal();

                // Small delay between checks
                Thread.sleep(500);

            } catch (Exception e) {
                System.out.println("  ✗ Error checking ride " + i + ": " + e.getMessage());
                closeModal();
            }
        }

        System.out.println("✗ No reviewable rides found in " + totalRides + " rides");
        return false;
    }

    /**
     * Find first expired ride (> 3 days old) and open review page
     * Used for testing expired review scenario
     *
     * @return true if found and opened, false otherwise
     */
    public boolean findAndOpenExpiredRide() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        int totalRides = cardList.size();
        System.out.println("Looking for expired ride (> 3 days old)...");

        for (int i = 0; i < totalRides; i++) {
            try {
                // Open ride details
                openRideDetails(i);

                // An expired ride has no "Leave a review" button
                if (!canCurrentRideBeReviewed()) {
                    System.out.println("  ✓ Found expired ride at index " + i);
                    // Leave the modal open so the test can inspect it
                    return true;
                }

                // Not expired, close and try next
                closeModal();
                Thread.sleep(500);

            } catch (Exception e) {
                System.out.println("  ✗ Error checking ride " + i + ": " + e.getMessage());
                try { closeModal(); } catch (Exception ignored) {}
            }
        }

        System.out.println("✗ No expired rides found");
        return false;
    }

    /**
     * Open review page for specific ride by index
     * Throws exception if ride cannot be reviewed
     */
    public void openReviewForRide(int rideIndex) {
        openRideDetails(rideIndex);

        if (!canCurrentRideBeReviewed()) {
            throw new RuntimeException("Ride at index " + rideIndex + " cannot be reviewed (> 3 days old or already reviewed)");
        }

        clickReviewButton();
    }

    /**
     * Get ride end time text from a ride card
     * Useful for debugging which rides are available
     */
    public String getRideEndTimeText(int rideIndex) {
        try {
            wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
            WebElement rideCard = cardList.get(rideIndex);

            List<WebElement> infoItems = rideCard.findElements(By.className("info-item"));
            for (WebElement item : infoItems) {
                String label = item.findElement(By.className("info-label")).getText();
                if (label.equals("End date")) {
                    return item.findElement(By.className("info-value")).getText();
                }
            }
        } catch (Exception e) {
            return "Unknown";
        }
        return "Unknown";
    }

    /**
     * Get full route text from the currently open ride modal
     * Format: "Start Address → Stop1 → Stop2 → End Address"
     */
    public String getFullRouteFromOpenModal() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            List<WebElement> addresses = driver.findElements(By.className("route-detail-address"));

            StringBuilder route = new StringBuilder();
            for (int i = 0; i < addresses.size(); i++) {
                route.append(addresses.get(i).getText().trim());
                if (i < addresses.size() - 1) {
                    route.append(" → ");
                }
            }

            return route.toString();

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if a ride has already been reviewed
     * Looks for rating stars in the modal
     */
    public boolean isRideAlreadyReviewed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(modalContent));

            // Look for driver review rating (p-rating component)
            List<WebElement> ratings = driver.findElements(By.tagName("p-rating"));

            // If ratings are present and not showing as "no review", it's been reviewed
            return ratings.size() >= 2; // Driver review + Ride review

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Print all rides for debugging
     * Shows which rides are reviewable
     */
    public void printAllRides() {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

        System.out.println("\n=== ALL RIDES IN HISTORY ===");
        for (int i = 0; i < cardList.size(); i++) {
            try {
                String endTime = getRideEndTimeText(i);
                System.out.println("Ride " + (i + 1) + ": End time = " + endTime);
            } catch (Exception e) {
                System.out.println("Ride " + (i + 1) + ": Error reading data");
            }
        }
        System.out.println("===========================\n");
    }

    /**
     * Legacy method for compatibility with existing tests
     */
    public String getFullRouteFromFirstRide(int index) {
        wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
        cardList.get(index).click();

        List<WebElement> addresses = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.className("route-detail-address")));

        StringBuilder route = new StringBuilder();
        for (int i = 0; i < addresses.size(); i++) {
            route.append(addresses.get(i).getText().trim());
            if (i < addresses.size() - 1) {
                route.append(" → ");
            }
        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("p-toast")));

        WebElement overlay = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("modal-overlay")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", overlay);

        return route.toString();
    }
}