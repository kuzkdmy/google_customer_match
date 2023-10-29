package com.test.gcm.service

import com.google.ads.googleads.v13.common.CustomerMatchUserListMetadata
import com.google.ads.googleads.v13.enums.OfflineUserDataJobTypeEnum.OfflineUserDataJobType
import com.google.ads.googleads.v13.resources.OfflineUserDataJob
import com.google.ads.googleads.v13.services._
import com.google.ads.googleads.v13.utils.{ErrorUtils, ResourceNames}
import com.test.gcm.domain.{CustomerId, OfflineUserDataJobId, UserListResourceName}
import zio.{Task, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}

trait GCMJobService {
  def getOfflineUserDataJob(customerId: CustomerId, userListName: UserListResourceName): Task[Option[OfflineUserDataJob]]
  def createJob(customerId: CustomerId, userListName: UserListResourceName): Task[CreateOfflineUserDataJobResponse]
  def addJobOps(customerId: CustomerId, jobId: OfflineUserDataJobId, ops: List[OfflineUserDataJobOperation]): Task[AddOfflineUserDataJobOperationsResponse]
  def runJob(customerId: CustomerId, jobId: OfflineUserDataJobId): Task[Unit]
}

case class GCMJobServiceImpl(clients: GCMClients) extends GCMJobService {

  override def getOfflineUserDataJob(customerId: CustomerId, userListName: UserListResourceName): Task[Option[OfflineUserDataJob]] = {
    ZIO.scoped(for {
      googleAdsServiceClient <- clients.googleAdsServiceClient()
      query =
        s"""SELECT resource_name, id, status, type, failure_reason,
           | customer_match_user_list_metadata.user_list
           | FROM offline_user_data_job
           | WHERE resource_name = '$userListName'
           |""".stripMargin.replaceAll("\n", "")
      response <- ZIO.attempt(googleAdsServiceClient.search(customerId.toString, query))
      res      <- ZIO.attempt(response.iterateAll.asScala.toList.headOption.flatMap(r => Option(r.getOfflineUserDataJob)))
    } yield res)
  }

  override def createJob(customerId: CustomerId, userListName: UserListResourceName): Task[CreateOfflineUserDataJobResponse] = {
    ZIO.scoped {
      for {
        userJobClient <- clients.offlineUserDataJobServiceClient()
        res <- {
          val meta = CustomerMatchUserListMetadata.newBuilder.setUserList(userListName.value)
          val job = OfflineUserDataJob.newBuilder
            .setType(OfflineUserDataJobType.CUSTOMER_MATCH_USER_LIST)
            .setCustomerMatchUserListMetadata(meta)
            .build
          ZIO.attempt(userJobClient.createOfflineUserDataJob(customerId.toString, job))
        }
      } yield res
    }
  }

  override def addJobOps(customerId: CustomerId, jobId: OfflineUserDataJobId, ops: List[OfflineUserDataJobOperation]): Task[AddOfflineUserDataJobOperationsResponse] = {
    ZIO.scoped {
      for {
        userJobClient   <- clients.offlineUserDataJobServiceClient()
        jobResourceName <- ZIO.attempt(ResourceNames.offlineUserDataJob(customerId.value, jobId.value))
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

  override def runJob(customerId: CustomerId, jobId: OfflineUserDataJobId): Task[Unit] = {
    ZIO.scoped(for {
      userJobClient   <- clients.offlineUserDataJobServiceClient()
      jobResourceName <- ZIO.attempt(ResourceNames.offlineUserDataJob(customerId.value, jobId.value))
      _               <- ZIO.attempt(userJobClient.runOfflineUserDataJobAsync(jobResourceName))
    } yield ())
  }
}

object GCMJobServiceImpl {
  lazy val layer: URLayer[GCMClients, GCMJobService] =
    ZLayer.fromFunction(GCMJobServiceImpl.apply _)
}
