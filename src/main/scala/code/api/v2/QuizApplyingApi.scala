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
      ("sections", JsArray(quiz.sections(quizApplyingId, print).map(sectionJson(_, quizApplyingId, bp, print)))))
  }

  def sectionJson(section: QuizSection, quizApplyingId: Long, customer:Customer, print:Boolean) = {
    JsObj(
      ("id", section.id.is),
      ("name", section.name.is),
      ("format", section.quizSectionFormat.is),
      ("obs", section.obs.is),
      ("questions", JsArray(section.questions(quizApplyingId, print).map(questionJson(_, quizApplyingId, customer, print)))))
  }

  def questionJson(question: QuizQuestion, quizApplyingId: Long, customer:Customer, print:Boolean) = {
    var size = "";

/*
    val answers = QuizAnswer.findAll(
      By(QuizAnswer.quizApplying, quizApplyingId),
      By(QuizAnswer.quizQuestion, question.id.is)).map( (u) => {
*/

    val sql = """
      select qr.id, qr.valuestr from quizquestion qq
      left join quizanswer qr on  qr.quizapplying = ? and qr.quizquestion = qq.id
      where qq.id = ?;
    """
    val r = DB.performQuery(sql,
      quizApplyingId::question.id.is::Nil)

    val answers = r._2.map( (p) => {

      val answerid = p(0).asInstanceOf[Long]
      val sqlId = if (answerid > 0) {
        " id = %s ".format (answerid);
      } else {
        " 1 = 1 "
      }

      var defaultValue = QuizAnswer.findAll(
        BySql (""" 
          quizApplying in (select id from quizapplying 
          where business_pattern = ? and id <> ?)
          """, IHaveValidatedThisSQL("",""), customer.id, quizApplyingId),
        By(QuizAnswer.quizQuestion, question.defaultQuestion),
        OrderBy(QuizAnswer.id, Descending)) match {
        case (l: List[QuizAnswer]) if (!l.isEmpty) => l(0).valueStr.get
        case _ => ""
      }

      var value = QuizAnswer.findAll(
        BySql (sqlId, IHaveValidatedThisSQL("","")),
        By(QuizAnswer.quizApplying, quizApplyingId),
        By(QuizAnswer.quizQuestion, question.id.is)) match {
        case (l: List[QuizAnswer]) if (!l.isEmpty) => l(0).valueStr.get
        case _ => ""
      }
      if (question.quizQuestionSize.is == 0) {
        size = "mini"
      } else if (question.quizQuestionSize.is == 1) {
        if (print) {
          if (question.quizQuestionType.is == 2 /*lista*/ &&
            question.quizQuestionPosition != 3 /* fim */) {
            size = "mini" // reduz
          } else {
            size = "small" 
          }
        } else {
          size = "small"
        }
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
      message_aux = User.replaceMessage (AuthUtil.user, message_aux)

      if (question.defaultQuestion != 0 && value == "") {
        value = defaultValue;
      }
      if (question.quizQuestionType == 8 /* texto rico */
        && value == "") {
        value = message_aux;
      }   
      
      val typeAux = if (print) {
        if (question.quizQuestionType.is == 2 /*lista*/ &&
          question.quizQuestionPosition != 3 /* fim */) {
          // era lista
          0 // printa texto 
          // marretado para não fazer texto no fim pq puxa alinha de baixo
          // qdo não tá no final da tela, talvez contar, mas tem que ver se é mini small
          // etc
        } else {
          question.quizQuestionType.is
        }
      } else {
        question.quizQuestionType.is
      }

      val domainAux = if (print) {
        if (question.quizQuestionType.is == 2 /*lista*/ &&
          question.quizQuestionPosition != 3 /* fim */) {
          JsArray(question.domain(-1, -1, print).map(domainJson(_)))
        } else {
          JsArray(question.domain(question.id.is, quizApplyingId, false).map(domainJson(_)))
        }  
      } else {
        JsArray(question.domain(question.id.is, quizApplyingId, print).map(domainJson(_)))
      }    
      val valueAux = if (print) {
        if (question.quizQuestionType.is == 2 /*lista*/ &&
          question.quizQuestionPosition != 3 /* fim */ &&
          value != "") {
          val ac = QuizDomainItem.findAll (
            By(QuizDomainItem.quizDomain,question.quizDomain),
            By(QuizDomainItem.id,value.toLong)
            )
          if (ac.length > 0) {
            ac(0).valueStr.get
          } else {
            ""
          }
        } else {
          value
        }  
      } else {
        value
      }
      JsObj(
      ("id", question.id.is),
      ("name", question.name.is),
      ("short_name", question.short_name.is),
      ("type", typeAux),
      ("format", question.quizQuestionFormat.is),
      ("addon", question.addon.is),
      ("sufix", question.sufix.is),
      ("message", message_aux),
      ("size", size),
      ("position", question.quizQuestionPosition.is),
      ("obs", question.obs.is),
      ("domain", domainAux),
      ("value", valueAux),
      ("answerid", answerid))
    });
    JsArray(answers)
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

