    package com.st3.uber.e2e.s1.pages;

    import org.openqa.selenium.By;
    import org.openqa.selenium.JavascriptExecutor;
    import org.openqa.selenium.WebDriver;
    import org.openqa.selenium.WebElement;
    import org.openqa.selenium.interactions.Actions;
    import org.openqa.selenium.support.FindBy;
    import org.openqa.selenium.support.PageFactory;
    import org.openqa.selenium.support.ui.ExpectedConditions;
    import org.openqa.selenium.support.ui.WebDriverWait;

    import java.util.List;

    public class PassengerHistoryPage {

        @FindBy(className = "ride-card")
        List<WebElement> cardList;

        @FindBy(id = "home-btn")
        private WebElement homeButton;

        @FindBy(id = "ride-history-title")
        private WebElement rideHistoryTitle;




        private WebDriver driver;
        private WebDriverWait wait;
        private Actions actions;


        public PassengerHistoryPage(WebDriver webDriver) {
            this.driver = webDriver;
            this.wait = new WebDriverWait(this.driver, java.time.Duration.ofSeconds(10));
            PageFactory.initElements(this.driver, this);
            this.actions = new Actions(this.driver);
        }


        public void addToFavorite(int index) {
            this.wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
            WebElement card = cardList.get(index);
            WebElement heart = card.findElement(By.className("favorite-heart"));
            if (!heart.getText().equals("❤️")) {
                this.actions.moveToElement(heart).click().perform();
            }
        }

        public boolean isOnHistoryPage() {
            this.wait.until(ExpectedConditions.visibilityOf(rideHistoryTitle));
            return rideHistoryTitle.getText().equals("My Ride History");
        }

        public boolean isAddedToFavorite(int index) {
            this.wait.until(ExpectedConditions.visibilityOfAllElements(cardList));
            WebElement card = cardList.get(index);
            WebElement heart = card.findElement(By.className("favorite-heart"));
            return heart.getText().equals("❤️");
        }

        public void goToHomePage() {
            this.wait.until(ExpectedConditions.visibilityOf(homeButton));
            this.actions.moveToElement(homeButton).click().perform();
        }

        public String getFullRouteFromFirstRide(int index) {

            wait.until(ExpectedConditions.visibilityOfAllElements(cardList));

            cardList.get(index).click();

            List<WebElement> addresses =
                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
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
