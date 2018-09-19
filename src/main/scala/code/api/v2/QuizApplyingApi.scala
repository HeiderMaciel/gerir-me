package code
package api

import code.model._
import code.util._
//import code.service._

import net.liftweb._
import http.js._
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.http.js.JE._
import net.liftweb.http.rest._
import net.liftweb.json.JsonAST.JBool
//import net.liftweb.mapper.By
import net.liftweb.mapper._ 

//implicit val formats = DefaultFormats // Brings in default date formats etc.


object QuizApplyingApi extends RestHelper with ReportRest with net.liftweb.common.Logger {
  serve {
    case "api" :: "v2" :: "quiz_applying" :: id :: print :: Nil JsonGet _ => {
      quizJson(id.toLong, print.toBoolean)
    }

    case "api" :: "v2" :: "quiz_applying" :: Nil Post _ => {
      for {
        id <- S.param("id")
        questions <- S.param("questions")
      } yield {
        questions.split(",").map(saveAnswer(id.toLong, _))
      }
      JBool(true)
    }
    case "api" :: "v2" :: "getQuestions" :: quizId :: Nil JsonGet _ =>{
      val quizParm = if (quizId != "" && quizId != "0") {
        quizId.toLong
      } else {
        1l // migracao para evitar none.get exception
      }
      val quiz = Quiz.findByKey(quizParm).get
      JsArray(QuizQuestion.findAll (BySql ("quizsection in ( select id from quizsection where quiz = ?)",
              IHaveValidatedThisSQL("",""), quizParm)).
        map((a) => {
        JsObj(
            ("status","success"),
            ("name",a.name.is),
            ("id",a.id.is)
          )
        }))
    }

// tá para testar com o jose bento  no atlas
// a seção tb tá fixa
// passar os parms na url
    case "api" :: "v2" :: "sectionCross" :: Nil Post _ =>{

        val SQL = """
select qa.applydate, qq.short_name,  qr.valuestr from quizapplying qa
left join quizsection qs on qs.quiz = qa.quiz and qs.id = 125 /* secao 2*/
left join quizquestion qq on qq.quizsection = qs.id
left join quizanswer qr on qr.quizapplying = qa.id and qr.quizquestion = qq.id
where qa.company = ? and qa.business_pattern = 485290 and qa.quiz = 115
order by qa.applydate, qq.orderinsection
        """
        toResponse(SQL,
          List(AuthUtil.company.id.is))
    } 

  }

  def saveAnswer(applyingId: Long, questionId: String)= {
    val questionIdClean = questionId.split("L")(0)
    val questionIdLong = questionIdClean.toLong
    val ac = QuizQuestion.findByKey (questionIdLong).get;
    val value: String = if(questionId contains "L") {
      S.params(questionId).mkString(",")
    } else {
      S.param(questionIdClean).getOrElse("")
    }

    var domainItem = 0;
    var valDomain = value;
    if (ac.quizDomainName != "") {
      if (ac.domainItemNoAnswer.toString == value) {
        valDomain = ""
      }
    }
    if ((value != "" && valDomain != "") || ac.saveIfNoAnswer_?) {
      val quizAnswer =
        QuizAnswer.findAll(
          By(QuizAnswer.quizApplying, applyingId),
          By(QuizAnswer.quizQuestion, questionIdLong)) match {
            case l:List[QuizAnswer] if !l.isEmpty => l.head
            case _ => QuizAnswer.createInCompany
          }

      val r = quizAnswer
        .quizApplying(applyingId)
        .quizQuestion(questionIdLong)
        .valueStr(value)
        .save
    }  
  }

  def quizJson(quizApplyingId: Long, print:Boolean) = {
    val qa = QuizApplying.findByKey(quizApplyingId)
    val quiz = qa.get.quiz.obj.get
    val bp = qa.get.business_pattern.obj.get
    JsObj(
      ("id", quiz.id.is),
      ("name", quiz.name.is),
      ("date", Project.dateToStr(qa.get.applyDate.is)),
      ("obs", quiz.obs.is),
      ("applyobs", qa.get.obs.is),
      ("bpName", bp.name.is),
      ("bpId", bp.id.is),
      ("sections", JsArray(quiz.sections(quizApplyingId, print).map(sectionJson(_, quizApplyingId, bp)))))
  }

  def sectionJson(section: QuizSection, quizApplyingId: Long, customer:Customer) = {
    JsObj(
      ("id", section.id.is),
      ("name", section.name.is),
      ("obs", section.obs.is),
      ("questions", JsArray(section.questions.map(questionJson(_, quizApplyingId, customer)))))
  }

  def questionJson(question: QuizQuestion, quizApplyingId: Long, customer:Customer) = {
    var size = "";
    val value = QuizAnswer.findAll(
      By(QuizAnswer.quizApplying, quizApplyingId),
      By(QuizAnswer.quizQuestion, question.id.is)) match {
      case (l: List[QuizAnswer]) if (!l.isEmpty) => l(0).valueStr.get
      case _ => ""
    }
    if (question.quizQuestionSize.is == 0) {
      size = "mini"
    } else if (question.quizQuestionSize.is == 1) {
      size = "small"
    } else if (question.quizQuestionSize.is == 2) {
      size = "medium"
    } else if (question.quizQuestionSize.is == 3) {
      size = "large"
    } else if (question.quizQuestionSize.is == 4) {
      size = "xlarge"
    } else if (question.quizQuestionSize.is == 5) {
      size = "xxlarge"
    }

    var message_aux = Customer.replaceMessage (customer, question.message.is)
    
    JsObj(
    ("id", question.id.is),
    ("name", question.name.is),
    ("short_name", question.short_name.is),
    ("type", question.quizQuestionType.is),
    ("format", question.quizQuestionFormat.is),
    ("addon", question.addon.is),
    ("sufix", question.sufix.is),
    ("message", message_aux),
    ("size", size),
    ("position", question.quizQuestionPosition.is),
    ("obs", question.obs.is),
    ("domain", JsArray(question.domain.map(domainJson(_)))),
    ("value", value))
  }

  def domainJson(domain: QuizDomainItem) = {
    JsObj(
      ("id", domain.id.is),
      ("name", domain.name.is),
      ("short_name", domain.short_name.is),
      ("valueStr", domain.valueStr.is),
      ("obs", domain.obs.is),
      ("color", domain.color.is))
  }
}

