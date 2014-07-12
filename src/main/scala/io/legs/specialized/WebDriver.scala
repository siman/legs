package io.legs.specialized

import java.io.File

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.utils.UserAgents
import org.apache.commons.io.FileUtils
import org.openqa.selenium.By
import org.openqa.selenium.phantomjs.{PhantomJSDriver, PhantomJSDriverService}
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.support.ui.{ExpectedConditions, Select, WebDriverWait}

import scala.concurrent._

object WebDriver extends Specialization {

	// selenium guides
	//http://docs.seleniumhq.org/docs/03_webdriver.jsp#user-input-filling-in-forms
	//http://refcardz.dzone.com/refcardz/getting-started-selenium

	def WD_VISIT(state: Specialization.State, url : String)(implicit ctx : ExecutionContext) : RoutableFuture = Future {
		val executablePath = "/tmp/phantomjs"
		val dstFile = new File(executablePath)
		if (!dstFile.exists()){
			FileUtils.copyURLToFile(getClass.getResource("/phantomjs"), dstFile)
		}

		dstFile.setExecutable(true)

		val capabilities = DesiredCapabilities.phantomjs()
		capabilities.setCapability("phantomjs.page.settings.userAgent", UserAgents.getRandom )
		capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, executablePath)
		val driver = new PhantomJSDriver(capabilities)
		driver.get(url)
		Yield(Some(driver))
	}

	def WD_SELECT_DROPDOWN(state: Specialization.State, driver : Any, elementXpathSelector : String, elementValue : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val foundElement = _driver.findElementByXPath(elementXpathSelector)
			val select = new Select(foundElement)
			select.selectByValue(elementValue)
			Yield(None)
		}

	def WD_CLICK(state: Specialization.State, driver : Any, elementXpathSelector : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val foundElement = _driver.findElementByXPath(elementXpathSelector)
			foundElement.click()
			Yield(None)
		}

	def WD_WAIT_UNTIL_SELECTOR(state: Specialization.State, driver : Any, xpathValidator : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			new WebDriverWait(_driver, 10)
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathValidator)))
			Yield(None)
		}

	def WD_XPATH_CHECK(state: Specialization.State, driver : Any, xpath : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val isFound = try {
				_driver.findElementByXPath(xpath).isDisplayed
			} catch {
				case _: Throwable => false
			}
			Yield(Some(isFound))
		}

	def WD_GET_HTML(state: Specialization.State, driver : Any)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val source = _driver.getPageSource
			Yield(Some(source))
		}

	def WD_CLOSE(state: Specialization.State, driver : Any) : RoutableFuture = {
		driver.asInstanceOf[PhantomJSDriver].close()
		Future.successful(Yield(None))
	}

}
