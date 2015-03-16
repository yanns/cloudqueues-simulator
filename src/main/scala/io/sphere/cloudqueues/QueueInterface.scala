package io.sphere.cloudqueues

import akka.actor.ActorRef
import akka.util.Timeout
import io.sphere.cloudqueues.QueueInterface.{ClaimResponse, MessagesAdded, QueueCreationResponse}
import io.sphere.cloudqueues.QueueManager._
import io.sphere.cloudqueues.reply._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag


object QueueInterface {

  sealed trait QueueCreationResponse
  case object QueueCreated extends QueueCreationResponse
  case object QueueAlreadyExists extends QueueCreationResponse

  case class MessagesAdded(messages: List[MessageInQueue])


  sealed trait ClaimResponse
  case class ClaimCreated(claim: Claim) extends ClaimResponse
  case object NoMessagesToClaim extends ClaimResponse

  case class MessageDeleted(id: MessageId)

}

class QueueInterface(queueManager: ActorRef) {

  implicit val timeout: Timeout = 50.milliseconds

  def newQueue(name: QueueName): Future[QueueCreationResponse] =
    queueManager ? NewQueue(name)

  def addMessages(queue: QueueName, messages: List[Message]): Future[Option[MessagesAdded]] =
    ask(queue, PutNewMessage(messages))

  def claimMessages(queue: QueueName, ttl: Int, limit: Int): Future[Option[ClaimResponse]] =
    ask(queue, ClaimMessages(ttl, limit))

  def deleteMessages(queue: QueueName, messageId: MessageId, claimId: Option[ClaimId]): Future[Any] =
    ask(queue, DeleteMessage(messageId, claimId))

  private def ask[R](queue: QueueName, op: QueueOperation[R])(implicit tag: ClassTag[R]): Future[R] =
    queueManager ? AQueueOperation[R](queue, op)

}