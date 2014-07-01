package io.legs.network

trait Communicator {

	def getHtmlStr(url: String) : String

	def getUrlStr(url:String) : String

}