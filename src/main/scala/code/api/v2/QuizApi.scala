package code
package api

import code.model._
import code.util._
import code.service._
import code.actors._

import net.liftweb._
import common._
import http._
import rest._
import json._
import http.js._
import JE._
import net.liftweb.util.Helpers
import net.liftweb.mapper._ 
import scala.xml._

import java.text.ParseException
import java.util.Date

import net.liftweb.json._
//implicit val formats = DefaultFormats // Brings in default date formats etc.


object QuizApi extends  RestHelper with ReportRest with net.liftweb.common.Logger {
		serve {
			case "api" :: "v2" :: "quiz" :: Nil Post _ => {
				for {
					customer <- S.param("customer") ?~ "customer parameter missing" ~> 400
					date <- S.param("date") ?~ "date parameter missing" ~> 400
					obs <- S.param("obs") ?~ "obs parameter missing" ~> 400
					quiz <- S.param("quiz") ?~ "type parameter missing" ~> 400
					val ac = Quiz.findByKey(quiz.toLong).get
					val cust = Customer.findByKey(customer.toLong).get
				} yield {
					var message_aux = Customer.replaceMessage (cust, ac.message.is)
					message_aux = User.replaceMessage (AuthUtil.user, message_aux)
					try {
					//JBool(
						QuizApplying.createInCompany.
						business_pattern(customer.toLong).
						obs(obs).quiz(quiz.toInt).
						applyDate(Project.strToDateOrToday(date)).
						message(message_aux).
						save
					//	)
						val thisQuiz = QuizApplying.findAll (By(QuizApplying.business_pattern, customer.toLong),
							By(QuizApplying.quiz, quiz.toInt),
							OrderBy(QuizApplying.id,Descending))(0); 
						JInt (thisQuiz.id.is)
					} catch {
						case e:Exception => JString(e.getMessage)
					}
				}
			}
			case  "api" :: "v2" :: "quiz" :: id :: Nil Delete _ => {
				try{
					JBool(QuizApplying.findByKey(id.toLong).get.delete_!)
					JInt(1)
				} catch {
					case e:Exception => JString(e.getMessage)
				}
			}

			case "api" :: "v2" :: "quiz" :: id :: Nil JsonGet _ => {
				quizJson(QuizApplying.findByKey(id.toLong).get.quiz.obj.get)
			}
/* too big to fit.....
			case "api" :: "v2" :: "getAnswers" :: quizId :: Nil JsonGet _ =>{
				val quizParm = if (quizId != "" && quizId != "0") {
					quizId.toLong
				} else {
					1l // migracao para evitar none.get exception
				}
				val quiz = Quiz.findByKey(quizParm).get
				JsArray(QuizQuestion.findAll (BySql ("quizsection in ( select id from quizsection where quiz = ?)",
                IHaveValidatedThisSQL("",""), quiz)).
					map((a) => {
					JsObj(
							("status","success"),
							("name",a.name.is),
							("id",a.id.is)
						)
					}))
			}
*/
		}

		def quizJson(quiz: Quiz) = {
println ("vaiiiii ========================== AQUI NAO DEVE" )

			JsObj(
				("id", quiz.id.is),
				("name", quiz.name.is),
				("sections", JsArray(quiz.sections(-1,false).map(sectionJson(_)))))
		}

		def sectionJson(section: QuizSection) = {
			JsObj(
				("id", section.id.is),
				("name", section.name.is), 
				("questions", JsArray(section.questions(-1,false).map(questionJson(_)))))
		}

		def questionJson(question: QuizQuestion) = {
			JsObj(
				("id", question.id.is),
				("name", question.name.is), 
				("type", question.quizQuestionType.is),
				("obs", question.obs.is),
				("domain", JsArray(question.domain(-1,-1,false).map(domainJson(_)))))
		}

		def domainJson(domain: QuizDomainItem) = {
			JsObj(
				("id", domain.id.is),
				("name", domain.name.is))
		}
}
