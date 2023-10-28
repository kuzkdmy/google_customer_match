
import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v13.common.CrmBasedUserListInfo;
import com.google.ads.googleads.v13.common.CustomerMatchUserListMetadata;
import com.google.ads.googleads.v13.common.OfflineUserAddressInfo;
import com.google.ads.googleads.v13.common.UserData;
import com.google.ads.googleads.v13.common.UserIdentifier;
import com.google.ads.googleads.v13.enums.CustomerMatchUploadKeyTypeEnum.CustomerMatchUploadKeyType;
import com.google.ads.googleads.v13.enums.OfflineUserDataJobStatusEnum.OfflineUserDataJobStatus;
import com.google.ads.googleads.v13.enums.OfflineUserDataJobTypeEnum.OfflineUserDataJobType;
import com.google.ads.googleads.v13.errors.GoogleAdsError;
import com.google.ads.googleads.v13.errors.GoogleAdsException;
import com.google.ads.googleads.v13.errors.GoogleAdsFailure;
import com.google.ads.googleads.v13.resources.OfflineUserDataJob;
import com.google.ads.googleads.v13.resources.UserList;
import com.google.ads.googleads.v13.services.AddOfflineUserDataJobOperationsRequest;
import com.google.ads.googleads.v13.services.AddOfflineUserDataJobOperationsResponse;
import com.google.ads.googleads.v13.services.CreateOfflineUserDataJobResponse;
import com.google.ads.googleads.v13.services.GoogleAdsRow;
import com.google.ads.googleads.v13.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v13.services.MutateUserListsResponse;
import com.google.ads.googleads.v13.services.OfflineUserDataJobOperation;
import com.google.ads.googleads.v13.services.OfflineUserDataJobServiceClient;
import com.google.ads.googleads.v13.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v13.services.SearchGoogleAdsStreamResponse;
import com.google.ads.googleads.v13.services.UserListOperation;
import com.google.ads.googleads.v13.services.UserListServiceClient;
import com.google.ads.googleads.v13.utils.ErrorUtils;
import com.google.ads.googleads.v13.utils.ResourceNames;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates operations to add members to a user list (a.k.a. audience) using an OfflineUserDataJob,
 * and if requested, runs the job.
 *
 * <p>If a job ID is specified, this example adds operations to that job. Otherwise, it creates a
 * new job for the operations.
 *
 * <p>IMPORTANT: Your application should create a single job containing <em>all</em> of the
 * operations for a user list. This will be far more efficient than creating and running multiple
 * jobs that each contain a small set of operations.
 *
 * <p><em>Notes:</em>
 *
 * <ul>
 *   <li>This feature is only available to accounts that meet the requirements described at
 *       https://support.google.com/adspolicy/answer/6299717.
 *   <li>It may take up to several hours for the list to be populated with members.
 *   <li>Email addresses must be associated with a Google account.
 *   <li>For privacy purposes, the user list size will show as zero until the list has at least
 *       1,000 members. After that, the size will be rounded to the two most significant digits.
 * </ul>
 */
public class AddCustomerMatchUserList {

    private static class AddCustomerMatchUserListParams {

        private Long customerId;

        private boolean runJob = true;

        private Long userListId;
        private Long offlineUserDataJobId;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        AddCustomerMatchUserListParams params = new AddCustomerMatchUserListParams();
            params.customerId = 2319161505L;
            params.runJob = true;

        GoogleAdsClient googleAdsClient = null;
        String secretDeveloperToken = "";
        // client_id/client_secret from google cloud -> api & services -> credentials -> oauth 2.0
        String secretClientId = "";
        String secretClientSecret = "";
        // I got it manually via another app https://github.com/googleads/googleads-python-lib
        String secretRefreshToken = "";
        //  id from test manager client(right top side)

            googleAdsClient =
                GoogleAdsClient
                        .newBuilder()
//    .setCredentials()
                        .setDeveloperToken(secretDeveloperToken)
                        .setCredentials(
                                UserCredentials
                                        .newBuilder()
                                        .setClientId(secretClientId)
                                        .setClientSecret(secretClientSecret)
                                        .setRefreshToken(secretRefreshToken)
//        .setAccessToken(new AccessToken(secretDeveloperToken, new Date()))
                                        .build())
                        .build();

        try {
            new AddCustomerMatchUserList()
                    .runExample(
                            googleAdsClient,
                            params.customerId,
                            params.runJob,
                            params.userListId,
                            params.offlineUserDataJobId);
        } catch (GoogleAdsException gae) {
            // GoogleAdsException is the base class for most exceptions thrown by an API request.
            // Instances of this exception have a message and a GoogleAdsFailure that contains a
            // collection of GoogleAdsErrors that indicate the underlying causes of the
            // GoogleAdsException.
            System.err.printf(
                    "Request ID %s failed due to GoogleAdsException. Underlying errors:%n",
                    gae.getRequestId());
            int i = 0;
            for (GoogleAdsError googleAdsError : gae.getGoogleAdsFailure().getErrorsList()) {
                System.err.printf("  Error %d: %s%n", i++, googleAdsError);
            }
            System.exit(1);
        }
    }

    /**
     * Runs the example.
     *
     * @param googleAdsClient      the Google Ads API client.
     * @param customerId           the client customer ID.
     * @param runJob               if true, runs the OfflineUserDataJob after adding operations. Otherwise, only
     *                             adds operations to the job.
     * @param userListId           optional ID of an existing user list. If {@code null}, creates a new user
     *                             list.
     * @param offlineUserDataJobId optional ID of an existing OfflineUserDataJob in the PENDING state.
     *                             If {@code null}, creates a new job.
     * @throws GoogleAdsException if an API request failed with one or more service errors.
     */
    private void runExample(
            GoogleAdsClient googleAdsClient,
            long customerId,
            boolean runJob,
            Long userListId,
            Long offlineUserDataJobId)
            throws UnsupportedEncodingException {
        String userListResourceName = null;
        if (offlineUserDataJobId == null) {
            if (userListId == null) {
                // Creates a Customer Match user list.
                userListResourceName = createCustomerMatchUserList(googleAdsClient, customerId);
            } else if (userListId != null) {
                // Uses the specified Customer Match user list.
                userListResourceName = ResourceNames.userList(customerId, userListId);
            }
        }

        // Adds members to the user list.
        addUsersToCustomerMatchUserList(
                googleAdsClient, customerId, runJob, userListResourceName, offlineUserDataJobId);
    }

    /**
     * Creates a Customer Match user list.
     *
     * @param googleAdsClient the Google Ads API client.
     * @param customerId      the client customer ID.
     * @return the resource name of the newly created user list.
     */
    private String createCustomerMatchUserList(GoogleAdsClient googleAdsClient, long customerId) {
        // Creates the new user list.
        UserList userList =
                UserList.newBuilder()
                        .setName("AKhrupalyk Customer Match list")
                        .setDescription("A list of customers that originated from email addresses")
                        // Customer Match user lists can use a membership life span of 10,000 to indicate
                        // unlimited; otherwise normal values apply.
                        // Sets the membership life span to 30 days.
                        .setMembershipLifeSpan(30)
                        // Sets the upload key type to indicate the type of identifier that will be used to
                        // add users to the list. This field is immutable and required for a CREATE operation.
                        .setCrmBasedUserList(
                                CrmBasedUserListInfo.newBuilder()
                                        .setUploadKeyType(CustomerMatchUploadKeyType.CONTACT_INFO))
                        .build();

        // Creates the operation.
        UserListOperation operation = UserListOperation.newBuilder().setCreate(userList).build();

        // Creates the service client.
        try (UserListServiceClient userListServiceClient =
                     googleAdsClient.getLatestVersion().createUserListServiceClient()) {
            // Adds the user list.
            MutateUserListsResponse response =
                    userListServiceClient.mutateUserLists(
                            Long.toString(customerId), ImmutableList.of(operation));
            // Prints the response.
            System.out.printf(
                    "Created Customer Match user list with resource name: %s.%n",
                    response.getResults(0).getResourceName());
            return response.getResults(0).getResourceName();
        }
    }

    /**
     * Creates and executes an asynchronous job to add users to the Customer Match user list.
     *
     * @param googleAdsClient      the Google Ads API client.
     * @param customerId           the client customer ID.
     * @param runJob               if true, runs the OfflineUserDataJob after adding operations. Otherwise, only
     *                             adds operations to the job.
     * @param userListResourceName the resource name of the Customer Match user list to add members
     *                             to.
     * @param offlineUserDataJobId optional ID of an existing OfflineUserDataJob in the PENDING state.
     *                             If {@code null}, creates a new job.
     */
    private void addUsersToCustomerMatchUserList(
            GoogleAdsClient googleAdsClient,
            long customerId,
            boolean runJob,
            String userListResourceName,
            Long offlineUserDataJobId)
            throws UnsupportedEncodingException {
        try (OfflineUserDataJobServiceClient offlineUserDataJobServiceClient =
                     googleAdsClient.getLatestVersion().createOfflineUserDataJobServiceClient()) {
            String offlineUserDataJobResourceName;
            if (offlineUserDataJobId == null) {
                // Creates a new offline user data job.
                OfflineUserDataJob offlineUserDataJob =
                        OfflineUserDataJob.newBuilder()
                                .setType(OfflineUserDataJobType.CUSTOMER_MATCH_USER_LIST)
                                .setCustomerMatchUserListMetadata(
                                        CustomerMatchUserListMetadata.newBuilder().setUserList(userListResourceName))
                                .build();

                // Issues a request to create the offline user data job.
                CreateOfflineUserDataJobResponse createOfflineUserDataJobResponse =
                        offlineUserDataJobServiceClient.createOfflineUserDataJob(
                                Long.toString(customerId), offlineUserDataJob);
                offlineUserDataJobResourceName = createOfflineUserDataJobResponse.getResourceName();
                System.out.printf(
                        "Created an offline user data job with resource name: %s.%n",
                        offlineUserDataJobResourceName);
            } else {
                // Reuses the specified offline user data job.
                offlineUserDataJobResourceName =
                        ResourceNames.offlineUserDataJob(customerId, offlineUserDataJobId);
            }

            // Issues a request to add the operations to the offline user data job. This example
            // only adds a few operations, so it only sends one AddOfflineUserDataJobOperations request.
            // If your application is adding a large number of operations, split the operations into
            // batches and send multiple AddOfflineUserDataJobOperations requests for the SAME job. See
            // https://developers.google.com/google-ads/api/docs/remarketing/audience-types/customer-match#customer_match_considerations
            // and https://developers.google.com/google-ads/api/docs/best-practices/quotas#user_data
            // for more information on the per-request limits.
            List<OfflineUserDataJobOperation> userDataJobOperations = buildOfflineUserDataJobOperations();
            AddOfflineUserDataJobOperationsResponse response =
                    offlineUserDataJobServiceClient.addOfflineUserDataJobOperations(
                            AddOfflineUserDataJobOperationsRequest.newBuilder()
                                    .setResourceName(offlineUserDataJobResourceName)
                                    .setEnablePartialFailure(true)
                                    .addAllOperations(userDataJobOperations)
                                    .build());

            // Prints the status message if any partial failure error is returned.
            // NOTE: The details of each partial failure error are not printed here, you can refer to
            // the example HandlePartialFailure.java to learn more.
            if (response.hasPartialFailureError()) {
                GoogleAdsFailure googleAdsFailure =
                        ErrorUtils.getInstance().getGoogleAdsFailure(response.getPartialFailureError());
                System.out.printf(
                        "Encountered %d partial failure errors while adding %d operations to the offline user "
                                + "data job: '%s'. Only the successfully added operations will be executed when "
                                + "the job runs.%n",
                        googleAdsFailure.getErrorsCount(),
                        userDataJobOperations.size(),
                        response.getPartialFailureError().getMessage());
            } else {
                System.out.printf(
                        "Successfully added %d operations to the offline user data job.%n",
                        userDataJobOperations.size());
            }

            if (!runJob) {
                System.out.printf(
                        "Not running offline user data job '%s', as requested.%n",
                        offlineUserDataJobResourceName);
                return;
            }

            // Issues an asynchronous request to run the offline user data job for executing
            // all added operations.
            offlineUserDataJobServiceClient.runOfflineUserDataJobAsync(offlineUserDataJobResourceName);

            // BEWARE! The above call returns an OperationFuture. The execution of that future depends on
            // the thread pool which is owned by offlineUserDataJobServiceClient. If you use this future,
            // you *must* keep the service client in scope too.
            // See https://developers.google.com/google-ads/api/docs/client-libs/java/lro for more detail.

            // Offline user data jobs may take 6 hours or more to complete, so instead of waiting for the
            // job to complete, retrieves and displays the job status once. If the job is completed
            // successfully, prints information about the user list. Otherwise, prints the query to use
            // to check the job again later.
            checkJobStatus(googleAdsClient, customerId, offlineUserDataJobResourceName);
        }
    }

    /**
     * Creates a list of offline user data job operations that will add users to the list.
     *
     * @return a list of operations.
     */
    private List<OfflineUserDataJobOperation> buildOfflineUserDataJobOperations()
            throws UnsupportedEncodingException {
        MessageDigest sha256Digest;
        try {
            sha256Digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing SHA-256 algorithm implementation", e);
        }

        // Creates a raw input list of unhashed user information, where each element of the list
        // represents a single user and is a map containing a separate entry for the keys "email",
        // "phone", "firstName", "lastName", "countryCode", and "postalCode". In your application, this
        // data might come from a file or a database.
        List<Map<String, String>> rawRecords = new ArrayList<>();
        // The first user data has an email address and a phone number.

        String filePath = "/Users/andrii.khrupalyk/onlyEmails.txt";
        List<String> emails = new ArrayList<>();

        System.out.println("emails: " + emails.size());
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                emails.add(line.trim());
            }
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }

        // Now you can iterate over the list of emails
        for (String email : emails) {
            Map<String, String> rawRecord =
                    ImmutableMap.<String, String>builder()
                            .put("email", email)
                            .build();
            rawRecords.add(rawRecord);

        }

        // Iterates over the raw input list and creates a UserData object for each record.
        List<UserData> userDataList = new ArrayList<>();
        for (Map<String, String> rawRecord : rawRecords) {
            // Creates a builder for the UserData object that represents a member of the user list.
            UserData.Builder userDataBuilder = UserData.newBuilder();
            // Checks if the record has email, phone, or address information, and adds a SEPARATE
            // UserIdentifier object for each one found. For example, a record with an email address and a
            // phone number will result in a UserData with two UserIdentifiers.

            // IMPORTANT: Since the identifier attribute of UserIdentifier
            // (https://developers.google.com/google-ads/api/reference/rpc/latest/UserIdentifier) is a
            // oneof
            // (https://protobuf.dev/programming-guides/proto3/#oneof-features), you must set only ONE of
            // hashedEmail, hashedPhoneNumber, mobileId, thirdPartyUserId, or addressInfo. Setting more
            // than one of these attributes on the same UserIdentifier will clear all the other members
            // of the oneof. For example, the following code is INCORRECT and will result in a
            // UserIdentifier with ONLY a hashedPhoneNumber.
            //
            // UserIdentifier incorrectlyPopulatedUserIdentifier =
            //     UserIdentifier.newBuilder()
            //         .setHashedEmail("...")
            //         .setHashedPhoneNumber("...")
            //         .build();
            //
            // The separate 'if' statements below demonstrate the correct approach for creating a UserData
            // for a member with multiple UserIdentifiers.

            // Checks if the record has an email address, and if so, adds a UserIdentifier for it.
            if (rawRecord.containsKey("email")) {
                UserIdentifier hashedEmailIdentifier =
                        UserIdentifier.newBuilder()
                                .setHashedEmail(normalizeAndHash(sha256Digest, rawRecord.get("email"), true))
                                .build();
                // Adds the hashed email identifier to the UserData object's list.
                userDataBuilder.addUserIdentifiers(hashedEmailIdentifier);
            }

            if (!userDataBuilder.getUserIdentifiersList().isEmpty()) {
                // Builds the UserData and adds it to the list.
                userDataList.add(userDataBuilder.build());
            }
        }

        // Creates the operations to add users.
        List<OfflineUserDataJobOperation> operations = new ArrayList<>();
        for (UserData userData : userDataList) {
            operations.add(OfflineUserDataJobOperation.newBuilder().setCreate(userData).build());
        }

        return operations;
    }

    /**
     * Returns the result of normalizing and then hashing the string using the provided digest.
     * Private customer data must be hashed during upload, as described at
     * https://support.google.com/google-ads/answer/7474263.
     *
     * @param digest                 the digest to use to hash the normalized string.
     * @param s                      the string to normalize and hash.
     * @param trimIntermediateSpaces if true, removes leading, trailing, and intermediate spaces from
     *                               the string before hashing. If false, only removes leading and trailing spaces from the
     *                               string before hashing.
     */
    private String normalizeAndHash(MessageDigest digest, String s, boolean trimIntermediateSpaces)
            throws UnsupportedEncodingException {
        // Normalizes by first converting all characters to lowercase, then trimming spaces.
        String normalized = s.toLowerCase();
        if (trimIntermediateSpaces) {
            // Removes leading, trailing, and intermediate spaces.
            normalized = normalized.replaceAll("\\s+", "");
        } else {
            // Removes only leading and trailing spaces.
            normalized = normalized.trim();
        }
        // Hashes the normalized string using the hashing algorithm.
        byte[] hash = digest.digest(normalized.getBytes("UTF-8"));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    /**
     * Retrieves, checks, and prints the status of the offline user data job.
     *
     * @param googleAdsClient                the Google Ads API client.
     * @param customerId                     the client customer ID.
     * @param offlineUserDataJobResourceName the resource name of the OfflineUserDataJob to get the
     *                                       status for.
     */
    private void checkJobStatus(
            GoogleAdsClient googleAdsClient, long customerId, String offlineUserDataJobResourceName) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
                     googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            String query =
                    String.format(
                            "SELECT offline_user_data_job.resource_name, "
                                    + "offline_user_data_job.id, "
                                    + "offline_user_data_job.status, "
                                    + "offline_user_data_job.type, "
                                    + "offline_user_data_job.failure_reason, "
                                    + "offline_user_data_job.customer_match_user_list_metadata.user_list "
                                    + "FROM offline_user_data_job "
                                    + "WHERE offline_user_data_job.resource_name = '%s'",
                            offlineUserDataJobResourceName);
            // Issues the query and gets the GoogleAdsRow containing the job from the response.
            GoogleAdsRow googleAdsRow =
                    googleAdsServiceClient
                            .search(Long.toString(customerId), query)
                            .iterateAll()
                            .iterator()
                            .next();
            OfflineUserDataJob offlineUserDataJob = googleAdsRow.getOfflineUserDataJob();
            System.out.printf(
                    "Offline user data job ID %d with type '%s' has status: %s%n",
                    offlineUserDataJob.getId(), offlineUserDataJob.getType(), offlineUserDataJob.getStatus());
            OfflineUserDataJobStatus jobStatus = offlineUserDataJob.getStatus();
            if (OfflineUserDataJobStatus.SUCCESS == jobStatus) {
                // Prints information about the user list.
                printCustomerMatchUserListInfo(
                        googleAdsClient,
                        customerId,
                        offlineUserDataJob.getCustomerMatchUserListMetadata().getUserList());
            } else if (OfflineUserDataJobStatus.FAILED == jobStatus) {
                System.out.printf("  Failure reason: %s%n", offlineUserDataJob.getFailureReason());
            } else if (OfflineUserDataJobStatus.PENDING == jobStatus
                    || OfflineUserDataJobStatus.RUNNING == jobStatus) {
                System.out.println();
                System.out.printf(
                        "To check the status of the job periodically, use the following GAQL query with"
                                + " GoogleAdsService.search:%n%s%n",
                        query);
            }
        }
    }

    /**
     * Prints information about the Customer Match user list.
     *
     * @param googleAdsClient      the Google Ads API client.
     * @param customerId           the client customer ID .
     * @param userListResourceName the resource name of the Customer Match user list.
     */
    private void printCustomerMatchUserListInfo(
            GoogleAdsClient googleAdsClient, long customerId, String userListResourceName) {
        try (GoogleAdsServiceClient googleAdsServiceClient =
                     googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
            // Creates a query that retrieves the user list.
            String query =
                    String.format(
                            "SELECT user_list.size_for_display, user_list.size_for_search "
                                    + "FROM user_list "
                                    + "WHERE user_list.resource_name = '%s'",
                            userListResourceName);

            // Constructs the SearchGoogleAdsStreamRequest.
            SearchGoogleAdsStreamRequest request =
                    SearchGoogleAdsStreamRequest.newBuilder()
                            .setCustomerId(Long.toString(customerId))
                            .setQuery(query)
                            .build();

            // Issues the search stream request.
            ServerStream<SearchGoogleAdsStreamResponse> stream =
                    googleAdsServiceClient.searchStreamCallable().call(request);

            // Gets the first and only row from the response.
            GoogleAdsRow googleAdsRow = stream.iterator().next().getResultsList().get(0);
            UserList userList = googleAdsRow.getUserList();
            System.out.printf(
                    "User list '%s' has an estimated %d users for Display and %d users for Search.%n",
                    userList.getResourceName(), userList.getSizeForDisplay(), userList.getSizeForSearch());
            System.out.println(
                    "Reminder: It may take several hours for the user list to be populated with the users.");
        }
    }
}
