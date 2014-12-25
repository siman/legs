package io.legs.specialized

import java.io.File

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsParamAnnotation, LegsFunctionAnnotation}
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

	//TOOD: refactor WD to LIVE_VISIT
	@LegsFunctionAnnotation(
		details = "start a WebDriver session ",
		yieldType = "WebDriver",
		yieldDetails = "a WebDriver instance"
	)
	def WD_VISIT(state: Specialization.State,
		url : String @LegsParamAnnotation("web url")
	)(implicit ctx : ExecutionContext) : RoutableFuture = Future {
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

	@LegsFunctionAnnotation(
		details = "select value from dropdown",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def WD_SELECT_DROPDOWN(state: Specialization.State,
		driver : Any @LegsParamAnnotation("instance of a WebDriver"),
		xpath : String @LegsParamAnnotation("XPATH"),
		elementValue : String @LegsParamAnnotation("element value to select from the dropdown items")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val foundElement = _driver.findElementByXPath(xpath)
			val select = new Select(foundElement)
			select.selectByValue(elementValue)
			Yield(None)
		}

	@LegsFunctionAnnotation(
		details = "click on item",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def WD_CLICK(state: Specialization.State,
		driver : Any @LegsParamAnnotation("instance of WebDriver"),
		xpath : String @LegsParamAnnotation("XPATH")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val foundElement = _driver.findElementByXPath(xpath)
			foundElement.click()
			Yield(None)
		}

	@LegsFunctionAnnotation(
		details = "wait until XPATH returns vlaue inside a live page",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def WD_WAIT_UNTIL_SELECTOR(state: Specialization.State,
		driver : Any @LegsParamAnnotation("instance of WebDriver"),
		xpath : String @LegsParamAnnotation("XPATH")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			new WebDriverWait(_driver, 10)
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)))
			Yield(None)
		}

	@LegsFunctionAnnotation(
		details = "execute XPATH and return true if something is returned",
		yieldType = Boolean,
		yieldDetails = "true if found something, false otherwise"
	)
	def WD_XPATH_CHECK(state: Specialization.State,
		driver : Any @LegsParamAnnotation("instance of WebDriver"),
		xpath : String @LegsParamAnnotation("XPATH")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val isFound = try {
				_driver.findElementByXPath(xpath).isDisplayed
			} catch {
				case _: Throwable => false
			}
			Yield(Some(isFound))
		}

	@LegsFunctionAnnotation(
		details = "get current HTML value of given WebDriver instance",
		yieldType = "String",
		yieldDetails = "string value of HTML"
	)
	def WD_GET_HTML(state: Specialization.State,
		driver : Any @LegsParamAnnotation("WebDriver instance")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val _driver = driver.asInstanceOf[PhantomJSDriver]
			val source = _driver.getPageSource
			Yield(Some(source))
		}

	@LegsFunctionAnnotation(
		details = "shutdown and cleanup WebDriver instance",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def WD_CLOSE(state: Specialization.State,
		driver : Any @LegsParamAnnotation("WebDriver instance")
	) : RoutableFuture = {
		driver.asInstanceOf[PhantomJSDriver].close()
		Future.successful(Yield(None))
	}

}
