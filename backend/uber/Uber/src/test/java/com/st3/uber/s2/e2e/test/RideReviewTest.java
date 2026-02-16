package com.st3.uber.s2.e2e.test;

import com.st3.uber.s2.e2e.pages.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import static org.testng.Assert.*;

/**
 * E2E Tests for Student 2: Ride Review Functionality (Functionality 2.8)
 */
public class RideReviewTest {

    WebDriver driver;
    WebDriverWait wait;
    HomePageUnregistered homePageUnregistered;
    LoginPage loginPage;
    HomePage homePage;
    PassengerHistoryPage passengerHistoryPage;
    RideReviewPage rideReviewPage;

    private static final String TEST_EMAIL = "marko.milutin.djudo+9999@gmail.com";
    private static final String TEST_PASSWORD = "1";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/uberdb";
    private static final String DB_USER = "uberuser";
    private static final String DB_PASSWORD = "uberpass";

    @BeforeSuite
    public void setupTestData() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("RIDE REVIEW E2E TESTS - Student 2");
        System.out.println("Automated Test Data Setup");
        System.out.println("=".repeat(70));

        runSQLSetup();

        System.out.println("✓ Test data created");
        System.out.println("✓ Ready to run tests");
        System.out.println("=".repeat(70) + "\n");
    }

    @BeforeMethod
    public void setupBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(this.driver, Duration.ofSeconds(10));
        this.homePageUnregistered = new HomePageUnregistered(this.driver);
        this.loginPage = new LoginPage(this.driver);
        this.homePage = new HomePage(this.driver);
        this.passengerHistoryPage = new PassengerHistoryPage(this.driver);
        this.rideReviewPage = new RideReviewPage(this.driver);
    }

    /**
     * Login and navigate to history page.
     * Called at the start of every test that needs it.
     */
    private void loginAndGoToHistory() {
        homePageUnregistered.clickLoginButton();
        assertTrue(loginPage.isOnLoginPage(), "Should be on login page");
        loginPage.login(TEST_EMAIL, TEST_PASSWORD);
        assertTrue(homePage.isLoggedIn(), "Should be logged in");
        homePage.goToDriverHistoryPage();
        assertTrue(passengerHistoryPage.isOnHistoryPage(), "Should be on history page");
    }

    @AfterMethod
    public void closeBrowser() {
        if (this.driver != null) {
            this.driver.quit();
        }
    }

    /**
     * TEST 0: Login and navigate (baseline test)
     */
    @Test(priority = 0)
    public void test00_LoginAndNavigate() {
        System.out.println("\n>>> TEST 0: Login and Navigate to History");

        loginAndGoToHistory();
        System.out.println("  ✓ Logged in successfully");
        System.out.println("  ✓ On history page");

        int rideCount = passengerHistoryPage.getRideCount();
        System.out.println("  ✓ Found " + rideCount + " rides");
        assertTrue(rideCount >= 3, "Should have at least 3 test rides");

        System.out.println("✓ TEST 0 PASSED\n");
    }

    /**
     * TEST 1: Cannot review expired ride (> 3 days old)
     */
    @Test(priority = 1)
    public void test01_CannotReviewExpiredRide() {
        System.out.println("\n>>> TEST 1: Cannot Review Expired Ride (> 3 days)");

        loginAndGoToHistory();

        boolean foundExpired = passengerHistoryPage.findAndOpenExpiredRide();
        assertTrue(foundExpired, "Should find expired ride (ride 9998 is 5 days old)");
        System.out.println("  ✓ Found expired ride");

        assertTrue(passengerHistoryPage.isOnHistoryPage(), "Should remain on history page");
        System.out.println("  ✓ Still on history page (no review button for expired ride)");

        assertFalse(passengerHistoryPage.canCurrentRideBeReviewed(),
                "Expired ride should have no review button");
        System.out.println("  ✓ No 'Leave a review' button shown for expired ride");

        passengerHistoryPage.closeModal();

        System.out.println("✓ TEST 1 PASSED\n");
    }

    /**
     * TEST 2: Cannot submit without driver rating + Check not submitted
     */
    @Test(priority = 2)
    public void test02_CannotSubmitWithoutDriverRating() {
        System.out.println("\n>>> TEST 2: Cannot Submit Without Driver Rating");

        loginAndGoToHistory();

        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        assertTrue(rideReviewPage.canReview(), "Should be within 3-day window");
        System.out.println("  ✓ On reviewable ride page");

        // Set ONLY vehicle rating (no driver rating)
        rideReviewPage.setVehicleRating(5);
        rideReviewPage.setComment("Test comment without driver rating");
        System.out.println("  ✓ Set only vehicle rating (driver rating = 0)");

        // Submit button should be DISABLED
        assertFalse(rideReviewPage.isSubmitButtonEnabled(),
                "Submit button should be DISABLED without driver rating");
        System.out.println("  ✓ Submit button is disabled (cannot submit)");

        // Go back to history
        rideReviewPage.goBack();
        assertTrue(passengerHistoryPage.isOnHistoryPage(), "Should be back on history");
        System.out.println("  ✓ Navigated back to history");

        // Verify review was NOT submitted - open same ride again
        passengerHistoryPage.openRideDetails(rideIndex);

        // Check that review doesn't exist (no stars showing)
        assertFalse(passengerHistoryPage.isRideAlreadyReviewed(),
                "Review should NOT exist - submission failed correctly");
        System.out.println("  ✓ Confirmed: Review was not submitted (validation worked)");

        passengerHistoryPage.closeModal();

        System.out.println("✓ TEST 2 PASSED\n");
    }

    /**
     * TEST 3: Cannot submit without vehicle rating + Check not submitted
     */
    @Test(priority = 3)
    public void test03_CannotSubmitWithoutVehicleRating() {
        System.out.println("\n>>> TEST 3: Cannot Submit Without Vehicle Rating");

        loginAndGoToHistory();

        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        System.out.println("  ✓ On reviewable ride page");

        // Set ONLY driver rating (no vehicle rating)
        rideReviewPage.setDriverRating(4);
        rideReviewPage.setComment("Test comment without vehicle rating");
        System.out.println("  ✓ Set only driver rating (vehicle rating = 0)");

        // Submit button should be DISABLED
        assertFalse(rideReviewPage.isSubmitButtonEnabled(),
                "Submit button should be DISABLED without vehicle rating");
        System.out.println("  ✓ Submit button is disabled (cannot submit)");

        // Go back to history
        rideReviewPage.goBack();
        assertTrue(passengerHistoryPage.isOnHistoryPage());
        System.out.println("  ✓ Navigated back to history");

        // Verify review was NOT submitted
        passengerHistoryPage.openRideDetails(rideIndex);
        assertFalse(passengerHistoryPage.isRideAlreadyReviewed(),
                "Review should NOT exist");
        System.out.println("  ✓ Confirmed: Review was not submitted");

        passengerHistoryPage.closeModal();

        System.out.println("✓ TEST 3 PASSED\n");
    }

    /**
     * TEST 4: Character counter updates correctly
     */
    @Test(priority = 4)
    public void test04_CharacterCounterUpdates() {
        System.out.println("\n>>> TEST 4: Character Counter Updates (FIXED)");

        loginAndGoToHistory();

        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        System.out.println("  ✓ On review page");

        // Test 1: Empty comment
        int initialCount = rideReviewPage.getCharacterCount();
        assertEquals(initialCount, 0, "Initial character count should be 0");
        System.out.println("  ✓ Initial count: 0");

        // Test 2: Short comment
        String shortComment = "Good ride";
        rideReviewPage.setComment(shortComment);
        int shortCount = rideReviewPage.getCharacterCount();
        assertEquals(shortCount, shortComment.length(),
                "Character count should match short comment length");
        System.out.println("  ✓ After '" + shortComment + "': " + shortCount + " characters");

        // Test 3: Longer comment
        String longComment = "This was an excellent ride! The driver was professional and the car was clean.";
        rideReviewPage.setComment(longComment);
        int longCount = rideReviewPage.getCharacterCount();
        assertEquals(longCount, longComment.length(),
                "Character count should match long comment length");
        System.out.println("  ✓ After longer comment: " + longCount + " characters");

        // Test 4: Clear and verify
        rideReviewPage.setComment("");
        int clearedCount = rideReviewPage.getCharacterCount();
        assertEquals(clearedCount, 0, "Character count should be 0 after clearing");
        System.out.println("  ✓ After clearing: 0 characters");

        System.out.println("✓ TEST 4 PASSED\n");
    }

    /**
     * TEST 5: Comment respects 1000 character limit
     */
    @Test(priority = 5)
    public void test05_CommentMaxLength() {
        System.out.println("\n>>> TEST 5: Comment Max Length (1000 characters)");

        loginAndGoToHistory();

        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        System.out.println("  ✓ On review page");

        // Create a 1200-character string
        String longComment = "a".repeat(1200);
        System.out.println("  ✓ Created 1200-character string");

        // Try to input it
        rideReviewPage.setComment(longComment);

        // The textarea has maxlength="1000", so it should truncate
        int actualCount = rideReviewPage.getCharacterCount();
        String actualComment = rideReviewPage.getComment();

        assertTrue(actualCount <= 1000, "Character count should not exceed 1000");
        assertTrue(actualComment.length() <= 1000, "Comment length should not exceed 1000");
        System.out.println("  ✓ Input truncated to: " + actualCount + " characters (max 1000)");

        System.out.println("✓ TEST 5 PASSED\n");
    }

    /**
     * TEST 6: Submit valid review + Verify in history (IMPROVED - validates ratings AND comment!)
     */
    @Test(priority = 6)
    public void test06_SubmitValidReviewAndVerify() {
        System.out.println("\n>>> TEST 6: Submit Valid Review + Verify (IMPROVED)");

        loginAndGoToHistory();

        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        System.out.println("  ✓ On review page");

        // Submit a complete review with specific values
        int expectedDriverRating = 5;
        int expectedVehicleRating = 4;
        String expectedComment = "Excellent service! Very professional driver.";

        rideReviewPage.setDriverRating(expectedDriverRating);
        rideReviewPage.setVehicleRating(expectedVehicleRating);
        rideReviewPage.setComment(expectedComment);
        System.out.println("  ✓ Set ratings: driver=" + expectedDriverRating +
                ", vehicle=" + expectedVehicleRating);
        System.out.println("  ✓ Set comment: \"" + expectedComment + "\"");

        assertTrue(rideReviewPage.isSubmitButtonEnabled(), "Submit button should be enabled");
        rideReviewPage.submitReview();
        System.out.println("  ✓ Submitted review");

        assertTrue(rideReviewPage.isSuccessMessageDisplayed(),
                "Success message should appear");
        System.out.println("  ✓ Success message displayed");

        // Go back to history
        rideReviewPage.goBack();
        assertTrue(passengerHistoryPage.isOnHistoryPage());
        System.out.println("  ✓ Back on history page");

        // Refresh page to get fresh data from backend
        passengerHistoryPage.refreshPage();
        assertTrue(passengerHistoryPage.isOnHistoryPage(), "Should still be on history page after refresh");
        System.out.println("  ✓ Page refreshed to load new review data");

        // Verify review exists in history modal
        passengerHistoryPage.openRideDetails(rideIndex);

        boolean hasReview = passengerHistoryPage.isRideAlreadyReviewed();
        assertTrue(hasReview, "Review should exist in history");

        int actualDriverRating = passengerHistoryPage.getDriverRatingFromModal();
        int actualVehicleRating = passengerHistoryPage.getVehicleRatingFromModal();

        assertEquals(actualDriverRating, expectedDriverRating,
                "Driver rating should match what was submitted");
        assertEquals(actualVehicleRating, expectedVehicleRating,
                "Vehicle rating should match what was submitted");

        System.out.println("  ✓ Driver rating verified: " + actualDriverRating + " (expected: " +
                expectedDriverRating + ")");
        System.out.println("  ✓ Vehicle rating verified: " + actualVehicleRating + " (expected: " +
                expectedVehicleRating + ")");

        passengerHistoryPage.closeModal();

        // Also verify on the review page itself
        passengerHistoryPage.openRideDetails(rideIndex);
        passengerHistoryPage.clickReviewButton();
        assertTrue(rideReviewPage.isOnReviewPage());

        int reviewPageDriverRating = rideReviewPage.getDriverRating();
        int reviewPageVehicleRating = rideReviewPage.getVehicleRating();
        String reviewPageComment = rideReviewPage.getComment();

        assertEquals(reviewPageDriverRating, expectedDriverRating,
                "Driver rating on review page should match");
        assertEquals(reviewPageVehicleRating, expectedVehicleRating,
                "Vehicle rating on review page should match");
        assertEquals(reviewPageComment, expectedComment,
                "Comment on review page should match");

        System.out.println("  ✓ Review page shows: driver=" + reviewPageDriverRating +
                ", vehicle=" + reviewPageVehicleRating);
        System.out.println("  ✓ Comment verified: \"" + reviewPageComment + "\"");

        System.out.println("✓ TEST 6 PASSED - All values verified!\n");
    }

    /**
     * TEST 7: Update existing review
     */
    @Test(priority = 7, dependsOnMethods = "test06_SubmitValidReviewAndVerify")
    public void test07_UpdateExistingReviewAndVerify() {
        System.out.println("\n>>> TEST 7: Update Existing Review + Verify Changes (IMPROVED)");

        loginAndGoToHistory();

        // Find the same ride (should have review from test 6)
        int rideIndex = passengerHistoryPage.findAndOpenReviewableRide();
        assertTrue(rideIndex >= 0, "Should find reviewable ride");
        assertTrue(rideReviewPage.isOnReviewPage());
        System.out.println("  ✓ On review page for ride with existing review");

        // Verify existing review notice is shown
        assertTrue(rideReviewPage.hasExistingReview(),
                "Should show 'existing review' notice");
        System.out.println("  ✓ Existing review notice displayed");

        // Verify form is pre-populated with previous review data (from test 6)
        int previousDriverRating = 5;
        int previousVehicleRating = 4;

        assertEquals(rideReviewPage.getDriverRating(), previousDriverRating,
                "Should load previous driver rating");
        assertEquals(rideReviewPage.getVehicleRating(), previousVehicleRating,
                "Should load previous vehicle rating");
        System.out.println("  ✓ Form pre-populated with existing ratings (driver=" +
                previousDriverRating + ", vehicle=" + previousVehicleRating + ")");

        // Update the review with new values
        int newDriverRating = 3;
        int newVehicleRating = 3;
        String newComment = "Updated review - changed my mind.";

        rideReviewPage.setDriverRating(newDriverRating);
        rideReviewPage.setVehicleRating(newVehicleRating);
        rideReviewPage.setComment(newComment);
        System.out.println("  ✓ Updated: driver=" + newDriverRating + ", vehicle=" +
                newVehicleRating);
        System.out.println("  ✓ Updated comment: \"" + newComment + "\"");

        // Submit update
        assertTrue(rideReviewPage.isSubmitButtonEnabled());
        rideReviewPage.submitReview();
        System.out.println("  ✓ Submitted updated review");

        // Verify success message
        assertTrue(rideReviewPage.isSuccessMessageDisplayed(),
                "Success message should appear after update");
        System.out.println("  ✓ Update success message displayed");

        // Go back to history
        rideReviewPage.goBack();
        assertTrue(passengerHistoryPage.isOnHistoryPage());
        System.out.println("  ✓ Back on history page");

        // Refresh to get updated data
        passengerHistoryPage.refreshPage();

        assertTrue(passengerHistoryPage.isOnHistoryPage());
        System.out.println("  ✓ Page refreshed to load updated review");

        // Verify updated review in history modal
        passengerHistoryPage.openRideDetails(rideIndex);
        assertTrue(passengerHistoryPage.isRideAlreadyReviewed(),
                "Updated review should still exist");

        int updatedDriverRating = passengerHistoryPage.getDriverRatingFromModal();
        int updatedVehicleRating = passengerHistoryPage.getVehicleRatingFromModal();

        assertEquals(updatedDriverRating, newDriverRating,
                "Driver rating should be updated to " + newDriverRating);
        assertEquals(updatedVehicleRating, newVehicleRating,
                "Vehicle rating should be updated to " + newVehicleRating);

        passengerHistoryPage.closeModal();

        // Also verify on the review page itself
        passengerHistoryPage.openRideDetails(rideIndex);
        passengerHistoryPage.clickReviewButton();
        assertTrue(rideReviewPage.isOnReviewPage());

        // Check editable form values (not readonly since ride can still be reviewed)
        int reviewPageDriverRating = rideReviewPage.getDriverRating();
        int reviewPageVehicleRating = rideReviewPage.getVehicleRating();
        String reviewPageComment = rideReviewPage.getComment();

        assertEquals(reviewPageDriverRating, newDriverRating,
                "Driver rating on review page should be updated");
        assertEquals(reviewPageVehicleRating, newVehicleRating,
                "Vehicle rating on review page should be updated");
        assertEquals(reviewPageComment, newComment,
                "Comment on review page should be updated");

        System.out.println("✓ TEST 7 PASSED - All updated values verified!\n");
    }

    /**
     * Automatically run SQL setup script
     */
    private void runSQLSetup() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();

            System.out.println("Running SQL setup...");

            // All setup SQL in one go
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("INSERT IGNORE INTO vehicles (id, model, type, registration_number, seating_capacity, baby_transport, pet_transport) VALUES (999, 'Test Vehicle', 'STANDARD', 'TEST-999-E2E', 4, 0, 1)");

            stmt.execute("INSERT IGNORE INTO users (id, email, password, name, surname, phone_number, address, profile_image, blocked, block_reason, image_path, role, verified) VALUES (9999, 'marko.milutin.djudo+9999@gmail.com', '$2a$12$uig2Rjdm7fsRBjDfJMouU.f6SLyakg2n4D2KynQHsQtUnMXBTmXDm', 'Test', 'Passenger', '+381651111111', 'Test Street 1, Novi Sad', NULL, 0, NULL, NULL, 'PASSENGER', 1)");

            stmt.execute("INSERT IGNORE INTO users (id, email, password, name, surname, phone_number, address, profile_image, blocked, block_reason, image_path, role, verified) VALUES (9998, 'marko.milutin.djudo+9998@gmail.com', '$2a$12$uig2Rjdm7fsRBjDfJMouU.f6SLyakg2n4D2KynQHsQtUnMXBTmXDm', 'Test', 'Driver', '+381652222222', 'Test Street 2, Novi Sad', NULL, 0, NULL, NULL, 'DRIVER', 1)");

            stmt.execute("INSERT IGNORE INTO passengers (id) VALUES (9999)");

            stmt.execute("INSERT IGNORE INTO drivers (id, vehicle_id, free, activity_request, active, available, working_minutes_per_day) VALUES (9998, 999, 1, 0, 1, 1, 1)");

            // Ride 9999 - Recent (2 hours ago)
            stmt.execute("INSERT IGNORE INTO rides (id, driver_id, creator_id, status, start_lat, start_lng, start_address, end_lat, end_lng, end_address, actual_end_lat, actual_end_lng, actual_end_address, created_at, scheduled_at, started_at, finished_at, stopped_early, stopped_at, vehicle_type, distance, base_price, calculated_price, cancelled_by, cancelled_at, termination_reason, panic, pet_transport, baby_transport, estimated_time_minutes) VALUES (9999, 9998, 9999, 'COMPLETED', 45.254844, 19.845350, 'Freedom Square, Novi Sad', 45.267136, 19.833549, 'Strand, Novi Sad', 45.267136, 19.833549, 'Strand, Novi Sad', NOW() - INTERVAL 2 HOUR - INTERVAL 30 MINUTE, NULL, NOW() - INTERVAL 2 HOUR - INTERVAL 15 MINUTE, NOW() - INTERVAL 2 HOUR, 0, NULL, 'STANDARD', 5.5, 200, 880, NULL, NULL, NULL, 0, 1, 0, 25)");

            stmt.execute("INSERT IGNORE INTO ride_passengers (ride_id, passenger_id) VALUES (9999, 9999)");

            // Ride 9998 - Old (5 days ago) - EXPIRED
            stmt.execute("INSERT IGNORE INTO rides (id, driver_id, creator_id, status, start_lat, start_lng, start_address, end_lat, end_lng, end_address, actual_end_lat, actual_end_lng, actual_end_address, created_at, scheduled_at, started_at, finished_at, stopped_early, stopped_at, vehicle_type, distance, base_price, calculated_price, cancelled_by, cancelled_at, termination_reason, panic, pet_transport, baby_transport, estimated_time_minutes) VALUES (9998, 9998, 9999, 'COMPLETED', 45.267136, 19.833549, 'Strand, Novi Sad', 45.254844, 19.845350, 'Freedom Square, Novi Sad', 45.254844, 19.845350, 'Freedom Square, Novi Sad', NOW() - INTERVAL 5 DAY - INTERVAL 2 HOUR, NULL, NOW() - INTERVAL 5 DAY - INTERVAL 1 HOUR - INTERVAL 45 MINUTE, NOW() - INTERVAL 5 DAY - INTERVAL 1 HOUR - INTERVAL 30 MINUTE, 0, NULL, 'STANDARD', 5.5, 200, 880, NULL, NULL, NULL, 0, 0, 0, 25)");

            stmt.execute("INSERT IGNORE INTO ride_passengers (ride_id, passenger_id) VALUES (9998, 9999)");

            // Ride 9997 - Recent with stop (1 day ago)
            stmt.execute("INSERT IGNORE INTO rides (id, driver_id, creator_id, status, start_lat, start_lng, start_address, end_lat, end_lng, end_address, actual_end_lat, actual_end_lng, actual_end_address, created_at, scheduled_at, started_at, finished_at, stopped_early, stopped_at, vehicle_type, distance, base_price, calculated_price, cancelled_by, cancelled_at, termination_reason, panic, pet_transport, baby_transport, estimated_time_minutes) VALUES (9997, 9998, 9999, 'COMPLETED', 45.240000, 19.850000, 'Bulevar Oslobodjenja, Novi Sad', 45.260000, 19.820000, 'Detelinara, Novi Sad', 45.260000, 19.820000, 'Detelinara, Novi Sad', NOW() - INTERVAL 1 DAY - INTERVAL 3 HOUR, NULL, NOW() - INTERVAL 1 DAY - INTERVAL 2 HOUR - INTERVAL 45 MINUTE, NOW() - INTERVAL 1 DAY - INTERVAL 2 HOUR - INTERVAL 30 MINUTE, 0, NULL, 'STANDARD', 8.2, 200, 1248, NULL, NULL, NULL, 0, 0, 1, 30)");

            stmt.execute("INSERT IGNORE INTO ride_passengers (ride_id, passenger_id) VALUES (9997, 9999)");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            stmt.close();
            conn.close();

            System.out.println("✓ SQL setup complete");

        } catch (Exception e) {
            System.err.println("ERROR running SQL setup: " + e.getMessage());
            e.printStackTrace();
            fail("Failed to setup test data: " + e.getMessage());
        }
    }

    /**
     * Automatically run SQL cleanup script
     */
    @AfterSuite
    public void cleanupTestData() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Cleaning up test data...");

        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("DELETE FROM reviews WHERE ride_id IN (9999, 9998, 9997)");
            stmt.execute("DELETE FROM notifications WHERE user_id = 9999");
            stmt.execute("DELETE FROM actual_ride_stops WHERE ride_id IN (9999, 9998, 9997)");
            stmt.execute("DELETE FROM ride_stops WHERE ride_id IN (9999, 9998, 9997)");
            stmt.execute("DELETE FROM ride_passengers WHERE ride_id IN (9999, 9998, 9997)");
            stmt.execute("DELETE FROM rides WHERE id IN (9999, 9998, 9997)");
            stmt.execute("DELETE FROM passengers WHERE id = 9999");
            stmt.execute("DELETE FROM drivers WHERE id = 9998");
            stmt.execute("DELETE FROM users WHERE id IN (9999, 9998)");
            stmt.execute("DELETE FROM vehicles WHERE id = 999");

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            stmt.close();
            conn.close();

            System.out.println("✓ Test data cleaned up");

        } catch (Exception e) {
            System.err.println("ERROR during cleanup: " + e.getMessage());
        }

        System.out.println("=".repeat(70));
        System.out.println("ALL TESTS COMPLETED");
        System.out.println("=".repeat(70) + "\n");
    }
}