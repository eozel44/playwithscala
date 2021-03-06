package controllers

import models.TaskListInMemoryModel
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.mvc._

import javax.inject._

case class LoginData(username: String, password: String)

@Singleton
class TaskList @Inject() (cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  def login: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def taskList: Action[AnyContent] = Action { implicit request =>
    request.session
      .get("username")
      .map { username =>
        val tasks = TaskListInMemoryModel.getTasks(username)
        Ok(views.html.TaskList(tasks))
      }
      .getOrElse(Redirect(routes.TaskList.login))
  }

  def validateLoginGet(username: String, password: String): Action[AnyContent] = Action {

    Ok(s"username: $username logged in with $password")
  }

  //parameters encoding the body
  def validateLoginPost(): Action[AnyContent] = Action { implicit request =>
    request.body.asFormUrlEncoded.map { args =>
      val username = args("username").head
      val password = args("password").head

      if (TaskListInMemoryModel.validateUser(username, password))
        Redirect(routes.TaskList.taskList).withSession("username" -> username)
      else
        Redirect(routes.TaskList.login)

    }.getOrElse(Redirect(routes.TaskList.login)).flashing("error" -> "Invalid username/password combination")
  }

  def createUser: Action[AnyContent] = Action { implicit request =>
    request.body.asFormUrlEncoded.map { args =>
      val username = args("username").head
      val password = args("password").head
      if (TaskListInMemoryModel.createUser(username, password))
        Redirect(routes.TaskList.taskList).withSession("username" -> username)
      else
        Redirect(routes.TaskList.login)
    }.getOrElse(Redirect(routes.TaskList.login)).flashing("error" -> "User creation failed.")
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.TaskList.login).withNewSession
  }

  def addTask: Action[AnyContent] = Action { implicit request =>
    request.session
      .get("username")
      .map { username =>
        request.body.asFormUrlEncoded.map { args =>
          val task = args("newTask").head
          TaskListInMemoryModel.addTask(username, task)
          Redirect(routes.TaskList.taskList)
        }.getOrElse(Redirect(routes.TaskList.taskList))
      }
      .getOrElse(Redirect(routes.TaskList.login))
  }

  def deleteTask: Action[AnyContent] = Action { implicit request =>
    request.session
      .get("username")
      .map { username =>
        request.body.asFormUrlEncoded.map { args =>
          val index = args("index").head.toInt
          TaskListInMemoryModel.removeTask(username, index)
          Redirect(routes.TaskList.taskList)
        }.getOrElse(Redirect(routes.TaskList.taskList))
      }
      .getOrElse(Redirect(routes.TaskList.login))

  }

  val loginForm: Form[LoginData] = Form(
    mapping("username" -> text(3, 10), "password" -> text(8))(LoginData.apply)(LoginData.unapply)
  )

  def createUserForm: Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      ld =>
        if (TaskListInMemoryModel.createUser(ld.username, ld.password)) {
          Redirect(routes.TaskList.taskList).withSession("username" -> ld.username)
        } else {
          Redirect(routes.TaskList.login).flashing("error" -> "User creation failed.")
        }
    )
  }

}
