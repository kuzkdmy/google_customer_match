import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v13.common.{CrmBasedUserListInfo, CustomerMatchUserListMetadata, UserData, UserIdentifier}
import com.google.ads.googleads.v13.enums.CustomerMatchUploadKeyTypeEnum.CustomerMatchUploadKeyType
import com.google.ads.googleads.v13.enums.OfflineUserDataJobTypeEnum.OfflineUserDataJobType
import com.google.ads.googleads.v13.enums.UserListCrmDataSourceTypeEnum.UserListCrmDataSourceType
import com.google.ads.googleads.v13.enums.UserListMembershipStatusEnum
import com.google.ads.googleads.v13.resources.{OfflineUserDataJob, OfflineUserDataJobMetadata, UserList}
import com.google.ads.googleads.v13.services._
import com.google.auth.oauth2.UserCredentials
import com.google.common.collect.ImmutableList

import java.security.MessageDigest
import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}

object Main extends App {
  // format: off
  // Google Ads UI, under TOOLS & SETTINGS > SETUP > API Center
  val secretDeveloperToken = "XXXXXXXXXXX"
  // client_id/client_secret from google cloud -> api & services -> credentials -> oauth 2.0
  val secretClientId       = "XXXXXXXXXXX"
  val secretClientSecret   = "XXXXXXXXXXX"
  // I got it manually via another app https://github.com/googleads/googleads-python-lib
  val secretRefreshToken   = "XXXXXXXXXXX"
  //  id from test manager client(right top side)
  val customerWhoseListIsModifiedId = "XXXXXXXXXXX"
  // format: on

  private val googleAdsClient = GoogleAdsClient
    .newBuilder()
    .setDeveloperToken(secretDeveloperToken)
    .setCredentials(
      UserCredentials
        .newBuilder()
        .setClientId(secretClientId)
        .setClientSecret(secretClientSecret)
        .setRefreshToken(secretRefreshToken)
        .build())
    .build()

  private val userListServiceClient: UserListServiceClient =
    googleAdsClient.getLatestVersion.createUserListServiceClient()

  private val offlineUserDataJobServiceClient: OfflineUserDataJobServiceClient =
    googleAdsClient.getLatestVersion.createOfflineUserDataJobServiceClient()

  private val userList = getOrCreateUserList(
    "Dima Kuzkin(dmitriy.kuzkin@gmail.com) test GCM audiences")

  setUserListMembers(
    userList,
    List(
      "dmitriy.kuzkin@gmail.com",
      "dmitriykuzkin@gmail.com", // check
      "email1@example.com",
      "email2@example.com"))

  private def getUserListByName(userListName: String): Option[UserList] = {
    val query =
      s"SELECT user_list.id, user_list.name, user_list.description, user_list.membership_status FROM user_list WHERE user_list.name = '$userListName'"
    val request = SearchGoogleAdsRequest
      .newBuilder()
      .setCustomerId(customerWhoseListIsModifiedId)
      .setQuery(query)
      .build()

    val response: GoogleAdsServiceClient.SearchPagedResponse = googleAdsClient.getLatestVersion
      .createGoogleAdsServiceClient()
      .search(request)

    response.iterateAll.asScala.toList.headOption.flatMap(r => Option(r.getUserList))
  }

  private def getOrCreateUserList(userListName: String): UserList = {
    getUserListByName(userListName) match {
      case Some(list) => list
      case None =>
        val userList: UserList = UserList
          .newBuilder()
          .setName(userListName)
          .setDescription("API created users list")
          .setMembershipStatus(UserListMembershipStatusEnum.UserListMembershipStatus.OPEN)
          .setCrmBasedUserList(
            CrmBasedUserListInfo
              .newBuilder()
              .setUploadKeyType(CustomerMatchUploadKeyType.CONTACT_INFO)
              .setDataSourceType(UserListCrmDataSourceType.FIRST_PARTY))
          .build()
        val userListOp: UserListOperation =
          UserListOperation.newBuilder().setCreate(userList).build()
        userListServiceClient.mutateUserLists(
          customerWhoseListIsModifiedId,
          ImmutableList.of(userListOp))
        getUserListByName(userListName).get
    }
  }

  private def setUserListMembers(userList: UserList, emails: List[String]) = {
    val offlineUserDataJob: OfflineUserDataJob = OfflineUserDataJob
      .newBuilder()
      .setType(OfflineUserDataJobType.CUSTOMER_MATCH_USER_LIST)
      .setCustomerMatchUserListMetadata(
        CustomerMatchUserListMetadata
          .newBuilder()
          .setUserList(userList.getResourceName))
      .setOperationMetadata(OfflineUserDataJobMetadata.newBuilder().build())
      .build()

    val offlineUserDataJobResponse = offlineUserDataJobServiceClient.createOfflineUserDataJob(
      customerWhoseListIsModifiedId,
      offlineUserDataJob)
    val offlineUserDataJobResourceName: String = offlineUserDataJobResponse.getResourceName
    val sha256Digest = MessageDigest.getInstance("SHA-256")
    val identifiers =
      emails
        .map(e =>
          UserIdentifier
            .newBuilder()
            .setHashedEmail(normalizeAndHashEmailAddress(sha256Digest, e))
            .build())
    val userData = UserData.newBuilder().addAllUserIdentifiers(identifiers.asJava).build()
    val offlineUserDataJobOperation: OfflineUserDataJobOperation =
      OfflineUserDataJobOperation.newBuilder().setCreate(userData).build()

    val addUserDataResponse = offlineUserDataJobServiceClient.addOfflineUserDataJobOperations(
      AddOfflineUserDataJobOperationsRequest
        .newBuilder()
        .setResourceName(offlineUserDataJobResourceName)
//        .setEnablePartialFailure(true)
        .addOperations(offlineUserDataJobOperation)
        .build())

    println("!!!!!!!!")
    println(addUserDataResponse)
    println("!!!!!!!!")
    addUserDataResponse
  }

  private def normalizeAndHash(digest: MessageDigest, s: String): String = {
    // Normalizes by removing leading and trailing whitespace and converting all characters to
    // lower case.
    val normalized = s.trim.toLowerCase
    // Hashes the normalized string using the hashing algorithm.
    val hash = digest.digest(normalized.getBytes("UTF-8"))
    val result = new StringBuilder()
    for (b <- hash) {
      result.append(String.format("%02x", b))
    }
    result.toString
  }

  private def normalizeAndHashEmailAddress(
      digest: MessageDigest,
      emailAddress: String): String = {
    var normalizedEmail = emailAddress.toLowerCase
    val emailParts = normalizedEmail.split("@")
    if (emailParts.length > 1 && emailParts(1).matches("^(gmail|googlemail)\\.com\\s*")) {
      // Removes any '.' characters from the portion of the email address before the domain if the
      // domain is gmail.com or googlemail.com.
      emailParts(0) = emailParts(0).replaceAll("\\.", "")
      normalizedEmail = String.format("%s@%s", emailParts(0), emailParts(1))
    }
    normalizeAndHash(digest, normalizedEmail)
  }

}
