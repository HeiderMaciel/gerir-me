package code
package api

import code.model._
import code.util._
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.http.js.JE._
import net.liftweb.http.rest._
import net.liftweb.json.JsonAST.JBool
//import net.liftweb.mapper.By
import net.liftweb.mapper._ 

//implicit val formats = DefaultFormats // Brings in default date formats etc.


object QuizApplyingApi extends RestHelper with net.liftweb.common.Logger {
  serve {
    case "api" :: "v2" :: "quiz_applying" :: id :: Nil JsonGet _ => {
      quizJson(id.toLong)
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
  }

  def saveAnswer(applyingId: Long, questionId: String)= {
    val questionIdClean = questionId.split("L")(0)
    val questionIdLong = questionIdClean.toLong
    val value: String = if(questionId contains "L") {
      S.params(questionId).mkString(",")
    } else {
      S.param(questionIdClean).getOrElse("")
    }

    //info(questionId + " => " + value)
    val quizAnswer =
      QuizAnswer.findAll(
        By(QuizAnswer.quizApplying, applyingId),
        By(QuizAnswer.quizQuestion, questionIdLong)) match {
          case l:List[QuizAnswer] if !l.isEmpty => l.head
          case _ => QuizAnswer.createInCompany
        }

    quizAnswer
      .quizApplying(applyingId)
      .quizQuestion(questionIdLong)
      .valueStr(value)
      .save
  }

  def quizJson(quizApplyingId: Long) = {
    val qa = QuizApplying.findByKey(quizApplyingId)
    val quiz = qa.get.quiz.obj.get
    val bp = qa.get.business_pattern.obj.get
    JsObj(
      ("id", quiz.id.is),
      ("name", quiz.name.is),
      ("date", Project.dateToStr(qa.get.applyDate.is)),
      ("obs", quiz.obs.is),
      ("bpName", bp.name.is),
      ("bpId", bp.id.is),
      ("sections", JsArray(quiz.sections.map(sectionJson(_, quizApplyingId)))))
  }

  def sectionJson(section: QuizSection, quizApplyingId: Long) = {
    JsObj(
      ("id", section.id.is),
      ("name", section.name.is),
      ("obs", section.obs.is),
      ("questions", JsArray(section.questions.map(questionJson(_, quizApplyingId)))))
  }

  def questionJson(question: QuizQuestion, quizApplyingId: Long) = {
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
    
    JsObj(
    ("id", question.id.is),
    ("name", question.name.is),
    ("short_name", question.short_name.is),
    ("type", question.quizQuestionType.is),
    ("format", question.quizQuestionFormat.is),
    ("addon", question.addon.is),
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
      ("obs", domain.obs.is),
      ("color", domain.color.is))
  }
}

