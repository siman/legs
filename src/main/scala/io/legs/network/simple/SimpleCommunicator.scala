package io.legs.network.simple

import com.typesafe.scalalogging.Logger
import io.legs.network.Communicator
import io.legs.utils.UserAgents
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import java.io.ByteArrayInputStream
import org.apache.tika.detect.AutoDetectReader
import java.nio.charset.Charset
import java.nio.ByteBuffer

import org.slf4j.LoggerFactory


object SimpleCommunicator extends Communicator {

	val logger = Logger(LoggerFactory.getLogger(getClass))

	def getHtmlStr(url: String): String = {
		val doc = Jsoup.connect(url).userAgent(UserAgents.getRandom).timeout(100000).get()
		doc.outputSettings().escapeMode(EscapeMode.xhtml)
		doc.toString
	}

	def getUrlStr(url: String): String =
		try {
			val client = HttpClientBuilder.create.build
			val request = new HttpGet(url)
			request.addHeader("User-Agent", "Mozilla/5.0")
			val response = client.execute(request)

			if (response.getEntity.getContentType.getValue.contains("text/") || response.getEntity.getContentType.getValue.contains("application/json")){
				val perlCharset = if (ContentType.getOrDefault(response.getEntity).getCharset != null)
					ContentType.getOrDefault(response.getEntity).getCharset.displayName else null

				val contentData = EntityUtils.toByteArray(response.getEntity)
				if (perlCharset != null) {
					Charset.forName(perlCharset).decode(ByteBuffer.wrap(contentData)).toString
				} else {
					val ad = new AutoDetectReader(new ByteArrayInputStream(contentData))
					val decoded = ad.getCharset.decode(ByteBuffer.wrap(contentData))
					decoded.toString
				}
			} else {
				throw new UnsupportedOperationException("could not parse content type:" + response.getEntity.getContentType.getValue)
			}

		} catch {
			case e: Throwable =>
				logger.error(s"failed fetching url:$url",e)
				""
		}

}