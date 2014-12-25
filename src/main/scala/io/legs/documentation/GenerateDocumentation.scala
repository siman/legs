package io.legs.documentation

import java.io.FileWriter

import io.legs.Specialization
import io.legs.Specialization._
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}

import scala.concurrent.ExecutionContext

object GenerateDocumentation {

	// TODO: add scala compiler to improve macro parsing into specific instance (using ToolBox
	// http://docs.scala-lang.org/overviews/reflection/annotations-names-scopes.html

	case class LegsFunction(
		name: String,
		functionAnnotation : LegsFunctionAnnotation,
		params : List[LegsParam]
	)

	case class LegsParam(
		name : String,
		paramType : String,
		annotation : LegsParamAnnotation
	)

	def main(args : Array[String]): Unit ={
		println("starting generation of Legs.io library functions")

		Specialization.registeredSpecializedClasses
			.map(extractLegsFunctions) match {
			case items =>
				val generated = genDocContents(items.flatten)
				val fw = new FileWriter("FUNCTIONS.md",false)
				fw.write(generated)
				fw.close()
				println("finished writing docs.")
		}

	}

	import scala.reflect.runtime.universe._
	import scala.reflect.runtime.{universe => ru}
	val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)

	def getType[T: TypeTag](obj: T) = typeOf[T]
	def getTypeTag[T: ru.TypeTag](obj: T) = ru.typeTag[T]

	def cleanAnnotationString(in: String) : String = (in.toList match {
		case '\"'::xs => xs
		case xs => xs
	}) match {
		case xs if xs.last == '\"' => xs.dropRight(1).mkString
		case xs => xs.mkString
	}

	def extractLegsFunctions[T <: Specialization](cls : T) : List[LegsFunction] =
		runtimeMirror.classSymbol(cls.getClass).toType.members
			.filter(_.isMethod) //get all methods
			.map(_.asMethod) // turn them into Method term symbols
			.filter(_.returnType == typeOf[RoutableFuture]) // only get those returning our special type
			.filter(_.name.toString != "invokeAction") // ignore the invokeAction base type
			.map (m=> {

				val relevantParams = m.paramLists.flatten
					.filter(_.typeSignature != typeOf[Specialization.State])
					.filter(_.typeSignature.baseClasses.forall(_.asType != typeOf[ExecutionContext].typeSymbol))
					.filter(_.typeSignature != typeOf[ExecutionContext])

				val paramAnnotations : List[LegsParamAnnotation] = relevantParams.map(_.typeSignature).map {
					case AnnotatedType(annotations,tpe) =>
						annotations.find(_.tree.tpe =:= typeOf[LegsParamAnnotation]).map { a =>
							a.tree.children match {
								case Nil => throw new Throwable(s"param did not have annotations! $cls -> $m -> $tpe")
								case t::xs if t.toString().contains(LegsParamAnnotation.getClass.getSimpleName.replace("$",""))=>
									LegsParamAnnotation(cleanAnnotationString(xs(0).toString()))
								case _ => throw new Throwable(s"param did not have annotations! $cls -> $m -> $tpe")
							}
						}.getOrElse(throw new Throwable(s"param did not have annotations! $cls -> $m -> $tpe"))
					case _ => throw new Throwable(s"param did not have annotations! $cls -> $m ")
				}



				val functionAnnotation = m.annotations.headOption.getOrElse(throw new Throwable(s"could not find function documentation $cls $m")).tree.children match {
					case x::args => LegsFunctionAnnotation(cleanAnnotationString(args(0).toString()),args(1).toString(),cleanAnnotationString(args(2).toString()))
					case _ => throw new Throwable(s"could not find function documentation $cls $m")
				}

				val params = (
					relevantParams.map(_.name.toString),
					relevantParams.map(_.typeSignature.typeSymbol.name.toString),
					paramAnnotations
				).zipped.toList.map(LegsParam.tupled)

				LegsFunction(
					name = m.name.toString,
					functionAnnotation = functionAnnotation,
					params = params
				)
			}).toList

	def genDocContents(functions: List[LegsFunction]) =
	s"""
	  |Legs.io - Functions Library
	  |====
	  |
	  |Below is an auto generated list of supported library functions in no particular order
	  |
	  |Methods
	  |------
	  |${functions.map(genFunctionOverviewDoc).mkString("\n")}
	  |
	  |
	  |Details
	  |-----
	  |${functions.map(genFunctionsDetailedDoc).mkString("\n\n")}
	  |
	""".stripMargin.trim

	private def genFunctionOverviewDoc(f : LegsFunction) =
		s"""
		  |* `${f.name}/${f.params.map(p=> s"${p.name} : ${p.paramType}").mkString("/")}` : `${f.functionAnnotation.yieldType}` [details](#${f.name})
		""".stripMargin.trim

	private def genFunctionsDetailedDoc(f : LegsFunction) =
		s"""
		   |#### <a name="${f.name}"></a>`${f.name}/${f.params.map(p=> s"${p.name} : ${p.paramType}").mkString("/")}` : `${f.functionAnnotation.yieldType}`
		   |${f.functionAnnotation.details}
		   |Yields : `${f.functionAnnotation.yieldType}` - ${f.functionAnnotation.yieldDetails}
		   |
		   |${f.params.map(genParamDetailsDoc).map(" * " + _).mkString("\n")}
		 """.stripMargin.trim

	private def genParamDetailsDoc(p : LegsParam) =
		s"""
		  |${p.name} : ${p.paramType} - ${p.annotation.details}
		""".stripMargin.trim

}
