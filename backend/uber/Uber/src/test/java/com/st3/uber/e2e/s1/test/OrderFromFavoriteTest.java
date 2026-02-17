package com.st3.uber.e2e.s1.test;

import com.st3.uber.e2e.s1.pages.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.testng.Assert.assertTrue;

public class OrderFromFavoriteTest {

    WebDriver driver;
    HomePageUnregistered homePageUnregistered;
    LoginPage loginPage;
    HomePage homePage;
    PassengerHistoryPage passengerHistoryPage;
    RideTrackingPage rideTrackingPage;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/uberdb";
    private static final String DB_USER = "uberuser";
    private static final String DB_PASSWORD = "uberpass";

    @BeforeMethod
    public void initialize() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");

        this.driver = new ChromeDriver(options);
        this.homePageUnregistered = new HomePageUnregistered(this.driver);
        this.loginPage = new LoginPage(this.driver);
        this.homePage = new HomePage(this.driver);
        this.passengerHistoryPage = new PassengerHistoryPage(this.driver);
        this.rideTrackingPage = new RideTrackingPage(this.driver);
    }


    @Test
    public void testGoToLoginPage() {
        this.homePageUnregistered.clickLoginButton();
        assertTrue(this.loginPage.isOnLoginPage());
    }

    @Test(dataProvider = "getLoginData")
    public void testLogin(String email, String password) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
    }


    @Test(dataProvider = "getData")
    public void testAddToFavorite(String email, String password, int index) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
        this.homePage.goToDriverHistoryPage();
        assertTrue(this.passengerHistoryPage.isOnHistoryPage());
        this.passengerHistoryPage.addToFavorite(index);
        assertTrue(this.passengerHistoryPage.isAddedToFavorite(index));
    }

    @Test(dataProvider = "getData")
    public void testOrderFromFavorite(String email, String password, int index) {
        this.homePageUnregistered.clickLoginButton();
        loginPage.login(email,password);
        assertTrue(homePage.isLoggedIn());
        this.homePage.goToDriverHistoryPage();
        assertTrue(this.passengerHistoryPage.isOnHistoryPage());
        this.passengerHistoryPage.addToFavorite(index);
        assertTrue(this.passengerHistoryPage.isAddedToFavorite(index));
        String historyRoute = this.passengerHistoryPage.getFullRouteFromFirstRide(index);
        this.passengerHistoryPage.goToHomePage();
        assertTrue(this.homePage.isLoggedIn());

        assertTrue(homePage.selectFavoriteByExactRoute(historyRoute));

        this.homePage.bookARide();

        this.homePage.goToRideTracking();

        assertTrue(this.rideTrackingPage.isOnRideTrackingPageAndAccepted());

    }


    @DataProvider
    Object[][] getLoginData() {
        return new Object[][] {
                {"prlincevic04@gmail.com", "Lukaprle123"}
        };
    }

    @DataProvider
    Object[][] getData() {
        return new Object[][] {
                {"prlincevic04@gmail.com", "Lukaprle123",1}
        };
    }


    @AfterMethod
    public void tearDown() {
        if(this.driver != null) {
            this.driver.quit();
        }
    }

    @AfterSuite
    public void cleanupAfterTests() {

        if(this.driver != null) {
            this.driver.quit();
        }

        System.out.println("\nCleaning up after tests");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            stmt.execute("""
                UPDATE rides
                SET status = 'COMPLETED'
                WHERE creator_id = 2
                ORDER BY created_at DESC
                LIMIT 1
            """);


            stmt.execute("""
               UPDATE drivers
                SET current_ride_id = NULL,
                    free = 1,
                    available = 1,
                    active = 1
               """);


            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

            System.out.println("Test data cleaned and last ride completed");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR during cleanup: " + e.getMessage());
        }

        System.out.println("Cleanup finished.");
    }

}
