package io.legs.specialized

import io.legs.Specialization
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.phantomjs.{PhantomJSDriverService, PhantomJSDriver}
import scala.util.Failure
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}
import org.openqa.selenium.By
import scala.util.Success
import org.apache.commons.io.FileUtils
import java.io.File
import io.legs.Specialization.{RoutableFuture, Yield}
import scala.concurrent._

object WebDriver extends Specialization {

	// selenium guides
	//http://docs.seleniumhq.org/docs/03_webdriver.jsp#user-input-filling-in-forms
	//http://refcardz.dzone.com/refcardz/getting-started-selenium

	def WD_VISIT(state: Specialization.State, url : String)(implicit ctx : ExecutionContext) : RoutableFuture = future {
		val executablePath = "/tmp/phantomjs"
		val dstFile = new File(executablePath)
		if (!dstFile.exists()){
			FileUtils.copyURLToFile(getClass.getResource("/phantomjs"), dstFile)
		}

    dstFile.setExecutable(true)

		val capabilities = DesiredCapabilities.phantomjs()
		capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, executablePath)
		val driver = new PhantomJSDriver(capabilities)
		driver.get(url)
		Success(Yield(Some(driver)))
	}

	def WD_SELECT_DROPDOWN(state: Specialization.State, driver : Any, elementXpathSelector : String, elementValue : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		future {
			try {
				val _driver = driver.asInstanceOf[PhantomJSDriver]
				val foundElement = _driver.findElementByXPath(elementXpathSelector)
				val select = new Select(foundElement)
				select.selectByValue(elementValue)
				Success(Yield(None))
			} catch {
				case e: Throwable => Failure(e)
			}
		}

	def WD_CLICK(state: Specialization.State, driver : Any, elementXpathSelector : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		future {
			try {
				val _driver = driver.asInstanceOf[PhantomJSDriver]
				val foundElement = _driver.findElementByXPath(elementXpathSelector)
				foundElement.click()
				Success(Yield(None))
			} catch {
				case e: Throwable => Failure(e)
			}
		}

	def WD_WAIT_UNTIL_SELECTOR(state: Specialization.State, driver : Any, xpathValidator : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		future {
			try {
				val _driver = driver.asInstanceOf[PhantomJSDriver]
				new WebDriverWait(_driver, 10)
						.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathValidator)))
				Success(Yield(None))
			} catch {
				case e: Throwable => Failure(e)
			}
		}

	def WD_XPATH_CHECK(state: Specialization.State, driver : Any, xpath : String)(implicit ctx : ExecutionContext) : RoutableFuture =
		future {
			try {
				val _driver = driver.asInstanceOf[PhantomJSDriver]
				val isFound = try {
					_driver.findElementByXPath(xpath).isDisplayed
				} catch {
					case _: Throwable => false
				}
				Success(Yield(Some(isFound)))
			} catch {
				case e: Throwable => Failure(e)
			}
		}

	def WD_GET_HTML(state: Specialization.State, driver : Any)(implicit ctx : ExecutionContext) : RoutableFuture =
		future {
			try {
				val _driver = driver.asInstanceOf[PhantomJSDriver]
				val source = _driver.getPageSource
				Success(Yield(Some(source)))
			} catch {
				case e: Throwable => Failure(e)
			}
		}

	def WD_CLOSE(state: Specialization.State, driver : Any) : RoutableFuture = {
		driver.asInstanceOf[PhantomJSDriver].close()
		Future.successful(Success(Yield(None)))
	}

}
