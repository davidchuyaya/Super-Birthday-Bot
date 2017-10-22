import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main
{
	final String URL = "https://cornelldti.slack.com/messages/C0P9H3V28/details/";
	final String USERNAME = ""; //TODO enter your username
	final String PASSWORD = ""; //TODO enter your password
	final int SCROLL_WAIT = 200; //adjust this to prevent stale element exception
	final int CLOSE_WAIT = 300; //adjust this to prevent clicks into dark filter
	final WebDriver chrome;
	final JavascriptExecutor jsExecutor;
	final WebDriverWait wait;

	public Main() throws Exception
	{
		ChromeDriverService service = new ChromeDriverService.Builder()
				.usingDriverExecutable(new File("chromedriver"))
				.usingAnyFreePort()
				.build();
		service.start();

		chrome = new RemoteWebDriver(service.getUrl(), DesiredCapabilities.chrome());
		chrome.get(URL);
		jsExecutor = (JavascriptExecutor) chrome;
		wait = new WebDriverWait(chrome, 10);

		login();
		Set<String> members = findMembers();
		askBirthdays(members);
	}

	private void login()
	{
		WebElement email = chrome.findElement(By.id("email"));
		WebElement password = chrome.findElement(By.id("password"));
		email.sendKeys(USERNAME);
		password.sendKeys(PASSWORD);
		password.submit();
	}

	private Set<String> findMembers() throws Exception
	{
		WebElement membersButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("member_count_title")));
		membersButton.click();

		jsExecutor.executeScript("channel_page_scroller.scrollBy(0, 500);");
		WebElement seeAllMembersButton = chrome.findElement(By.id("see_all_members"));
		seeAllMembersButton.click();

		//scroll to see all members
		Set<String> members = new HashSet<>();
		WebElement scroller = chrome.findElement(By.id("channel_membership_dialog_scroller"));

		//keep scrolling down looking for new members
		boolean foundMoreMembers = true;
		while (foundMoreMembers)
		{
			foundMoreMembers = false;
			List<WebElement> memberItems = scroller.findElements(By.className("member_item"));
			for (WebElement memberItem : memberItems)
			{
				WebElement nameHolder = memberItem.findElement(By.className("c-member__display-name"));
				boolean isNewMember = members.add(nameHolder.getText());
				if (isNewMember)
					foundMoreMembers = true;
			}

			//scroll
			jsExecutor.executeScript("channel_membership_dialog_scroller.scrollBy(0, 500);");
			//wait for scroll to finish
			Thread.sleep(SCROLL_WAIT);
		}

		WebElement closeButton = chrome.findElement(By.className("close"));
		closeButton.click();
		Thread.sleep(CLOSE_WAIT);

		//print out the members we found
		System.out.println(String.join(",", members));
		return members;
	}

	private void askBirthdays(Set<String> members) throws Exception
	{
		WebElement birthdayBotLink = chrome.findElement(By.xpath("//span[contains(text(), 'birthdaybot')]/.."));
		birthdayBotLink.click();

		for (String member : members)
		{
			if (member.isEmpty())
				continue;

			String query = "When is @" + member + " birthday";
			new Actions(chrome)
					.click(chrome.findElement(By.id("msg_input")))
					.sendKeys(query)
					.sendKeys(Keys.RETURN)
					.perform();

			Thread.sleep(1000);

			try
			{
				WebElement askBdayButton = chrome.findElement(By.xpath("//button[@title='Ask Bday']"));
				askBdayButton.click();
				WebElement confirmButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[contains(text(), 'Yes')]")));
				confirmButton.click();

				System.out.println("Asked birthday with: " + query);
				Thread.sleep(CLOSE_WAIT * 2);

			} catch (Exception e) {}
		}
	}

	public static void main(String[] args) throws Exception
	{
		new Main();
	}
}
