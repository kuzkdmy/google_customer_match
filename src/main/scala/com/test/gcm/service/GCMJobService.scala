package com.test.gcm.service

import com.google.ads.googleads.v13.common.CustomerMatchUserListMetadata
import com.google.ads.googleads.v13.enums.OfflineUserDataJobStatusEnum
import com.google.ads.googleads.v13.enums.OfflineUserDataJobTypeEnum.OfflineUserDataJobType
import com.google.ads.googleads.v13.resources.{OfflineUserDataJob, UserList}
import com.google.ads.googleads.v13.services._
import com.google.ads.googleads.v13.utils.{ErrorUtils, ResourceNames}
import com.test.gcm.domain.{ConnectionId, NotFoundError, OfflineUserDataJobId}
import com.test.gcm.repository.GCMConnectionsStore
import zio.{&, Task, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}

trait GCMJobService {
  def getJob(connectionId: ConnectionId, jobId: OfflineUserDataJobId): Task[Option[OfflineUserDataJob]]
  def pendingJobs(connectionId: ConnectionId, userList: UserList): Task[List[OfflineUserDataJobId]]
  def createJob(connectionId: ConnectionId, userList: UserList): Task[CreateOfflineUserDataJobResponse]
  def addJobOps(connectionId: ConnectionId, userList: UserList, jobId: OfflineUserDataJobId, ops: List[OfflineUserDataJobOperation]): Task[AddOfflineUserDataJobOperationsResponse]
  def runJob(connectionId: ConnectionId, jobId: OfflineUserDataJobId): Task[Unit]
}

case class GCMJobServiceImpl(clients: GCMClients, repo: GCMConnectionsStore) extends GCMJobService {

  override def getJob(connectionId: ConnectionId, jobId: OfflineUserDataJobId): Task[Option[OfflineUserDataJob]] = {
    ZIO.scoped(for {
      connection             <- repo.getOauthConnection(connectionId)
      googleAdsServiceClient <- clients.googleAdsServiceClient(connection)
      query = s"""SELECT
                 | offline_user_data_job.resource_name,
                 | offline_user_data_job.id,
                 | offline_user_data_job.status,
                 | offline_user_data_job.type,
                 | offline_user_data_job.failure_reason,
                 | offline_user_data_job.customer_match_user_list_metadata.user_list
                 | FROM offline_user_data_job
                 | WHERE offline_user_data_job.id = '${jobId.value}'
                 |""".stripMargin.replaceAll("\n", "")
      response <- ZIO.attempt(googleAdsServiceClient.search(connection.customerId.toString, query))
      res      <- ZIO.attempt(response.iterateAll.asScala.toList.flatMap(r => Option(r.getOfflineUserDataJob)).headOption)
    } yield res)
  }

  override def pendingJobs(connectionId: ConnectionId, userList: UserList): Task[List[OfflineUserDataJobId]] = {
    ZIO.scoped(for {
      connection             <- repo.getOauthConnection(connectionId)
      googleAdsServiceClient <- clients.googleAdsServiceClient(connection)
      query = s"""SELECT
                 | offline_user_data_job.resource_name,
                 | offline_user_data_job.id,
                 | offline_user_data_job.status,
                 | offline_user_data_job.type,
                 | offline_user_data_job.failure_reason,
                 | offline_user_data_job.customer_match_user_list_metadata.user_list
                 | FROM offline_user_data_job
                 | WHERE offline_user_data_job.customer_match_user_list_metadata.user_list = '${userList.getResourceName}'
                 | AND offline_user_data_job.status = 'PENDING'
                 | ORDER BY offline_user_data_job.id DESC
                 |""".stripMargin.replaceAll("\n", "")
      response <- ZIO.attempt(googleAdsServiceClient.search(connection.customerId.toString, query))
      allJobs  <- ZIO.attempt(response.iterateAll.asScala.toList.flatMap(r => Option(r.getOfflineUserDataJob)))
    } yield allJobs.map(j => OfflineUserDataJobId(j.getId)))
  }

  override def createJob(connectionId: ConnectionId, userList: UserList): Task[CreateOfflineUserDataJobResponse] = {
    ZIO.scoped {
      for {
        connection    <- repo.getOauthConnection(connectionId)
        userJobClient <- clients.offlineUserDataJobServiceClient(connection)
        res <- {
          val meta = CustomerMatchUserListMetadata.newBuilder.setUserList(userList.getResourceName)
          val job = OfflineUserDataJob.newBuilder
            .setType(OfflineUserDataJobType.CUSTOMER_MATCH_USER_LIST)
            .setCustomerMatchUserListMetadata(meta)
            .build
          ZIO.attempt(userJobClient.createOfflineUserDataJob(connection.customerId.toString, job))
        }
      } yield res
    }
  }

  override def addJobOps(connectionId: ConnectionId, userList: UserList, jobId: OfflineUserDataJobId, ops: List[OfflineUserDataJobOperation]): Task[AddOfflineUserDataJobOperationsResponse] = {
    ZIO.scoped {
      for {
        job <- getJob(connectionId, jobId).someOrFail(NotFoundError(jobId.value.toString, "Offline User Data Job"))
        _ <- ZIO
               .fail(new Exception(s"User List [${userList.getResourceName}] don't have such job [${jobId.value}]"))
               .unless(job.getCustomerMatchUserListMetadata.getUserList == userList.getResourceName)
        _ <- ZIO
               .fail(new Exception(s"Job ${jobId.value} is not in PENDING state"))
               .unless(job.getStatus == OfflineUserDataJobStatusEnum.OfflineUserDataJobStatus.PENDING)
        connection      <- repo.getOauthConnection(connectionId)
        userJobClient   <- clients.offlineUserDataJobServiceClient(connection)
        jobResourceName <- ZIO.attempt(ResourceNames.offlineUserDataJob(connection.customerId.value, jobId.value))
        response <- ZIO.attempt(
                      userJobClient.addOfflineUserDataJobOperations(
                        AddOfflineUserDataJobOperationsRequest.newBuilder
                          .setResourceName(jobResourceName)
                          .setEnablePartialFailure(true)
                          .addAllOperations(ops.asJava)
                          .build
                      )
                    )
        _ <- if (response.hasPartialFailureError) {
               val failureError     = response.getPartialFailureError
               val googleAdsFailure = ErrorUtils.getInstance.getGoogleAdsFailure(failureError)
               ZIO.logWarning(s"""Encountered ${googleAdsFailure.getErrorsCount}
                                 | partial failure errors while adding ${ops.size}
                                 | operations to the offline user ${failureError.getMessage}.
                                 | Only the successfully added operations will be executed when the job runs
                                 |""".stripMargin.replaceAll("\n", ""))
             } else {
               ZIO.logInfo(s"Successfully added ${ops.size} operations to the offline user data job $jobId")
             }
      } yield response
    }
  }

  override def runJob(connectionId: ConnectionId, jobId: OfflineUserDataJobId): Task[Unit] = {
    ZIO.scoped(for {
      connection      <- repo.getOauthConnection(connectionId)
      userJobClient   <- clients.offlineUserDataJobServiceClient(connection)
      jobResourceName <- ZIO.attempt(ResourceNames.offlineUserDataJob(connection.customerId.value, jobId.value))
      _               <- ZIO.attempt(userJobClient.runOfflineUserDataJobAsync(jobResourceName))
    } yield ())
  }
}

object GCMJobServiceImpl {
  lazy val layer: URLayer[GCMClients & GCMConnectionsStore, GCMJobService] =
    ZLayer.fromFunction(GCMJobServiceImpl.apply _)
}
