package com.backpackers.android.backend.api;

import com.google.api.client.util.Base64;
import com.google.api.client.util.StringUtils;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiClass;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.users.User;
import com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import com.backpackers.android.backend.authenticator.FacebookAuthenticator;
import com.backpackers.android.backend.authenticator.GoogleAuthenticator;
import com.backpackers.android.backend.model.user.Account;
import com.backpackers.android.backend.validator.Validator;
import com.backpackers.android.backend.validator.rule.common.IdValidationRule;
import com.backpackers.android.backend.validator.rule.common.NotFoundRule;
import com.backpackers.android.backend.validator.rule.credentials.CredentialsRule;
import com.backpackers.android.backend.validator.rule.credentials.UserConflictRule;
import com.backpackers.android.backend.Constants;
import com.backpackers.android.backend.authenticator.YolooAuthenticator;
import com.backpackers.android.backend.controller.UserController;
import com.backpackers.android.backend.validator.rule.common.AuthenticationRule;
import com.backpackers.android.backend.validator.rule.credentials.CredentialsExistenceRule;

import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Api(
        name = "yolooApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = Constants.API_OWNER,
                ownerName = Constants.API_OWNER,
                packagePath = Constants.API_PACKAGE_PATH
        )
)
@ApiClass(
        resource = "users",
        clientIds = {
                Constants.ANDROID_CLIENT_ID,
                Constants.IOS_CLIENT_ID,
                Constants.WEB_CLIENT_ID},
        audiences = {Constants.AUDIENCE_ID,},
        authenticators = {
                GoogleAuthenticator.class,
                FacebookAuthenticator.class,
                YolooAuthenticator.class
        }
)
public class UserEndpoint {

    private static final Logger logger =
            Logger.getLogger(UserEndpoint.class.getName());

    /**
     * Returns the {@link Account} with the corresponding ID.
     *
     * @param websafeUserId the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.get",
            path = "users/{userId}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public Account get(@Named("userId") final String websafeUserId, final User user)
            throws ServiceException {
        Validator.builder()
                .addRule(new IdValidationRule(websafeUserId))
                .addRule(new AuthenticationRule(user))
                .addRule(new NotFoundRule(websafeUserId))
                .validate();

        return UserController.newInstance().get(websafeUserId, user);
    }

    /**
     * Returns the {@link Account}.
     *
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.me.get",
            path = "users/me",
            httpMethod = ApiMethod.HttpMethod.GET)
    public Account getSelf(final User user) throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().getSelf(user.getUserId());
    }

    /**
     * Returns the {@link Account}.
     *
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.me.followers",
            path = "users/me/followers",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Account> getSelfFollowers(final User user,
                                                        @Nullable @Named("cursor") final String cursor,
                                                        @Nullable @Named("limit") Integer limit)
            throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().getFollowers(user.getUserId(), cursor, limit);
    }

    /**
     * Returns the {@link Account}.
     *
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.me.followees",
            path = "users/me/followees",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Account> getSelfFollowees(final User user,
                                                        @Nullable @Named("cursor") final String cursor,
                                                        @Nullable @Named("limit") Integer limit)
            throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().getFollowings(user.getUserId(), cursor, limit);
    }

    /**
     * Returns the {@link Account}.
     *
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.followers",
            path = "users/{userId}/followers",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Account> getFollowers(final User user,
                                                    @Named("userId") String targetUserId,
                                                    @Nullable @Named("cursor") final String cursor,
                                                    @Nullable @Named("limit") Integer limit)
            throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().getFollowers(targetUserId, cursor, limit);
    }

    /**
     * Returns the {@link Account}.
     *
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code Account} with the provided ID.
     */
    @ApiMethod(
            name = "users.followees",
            path = "users/{userId}/followees",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<Account> getFollowees(final User user,
                                                    @Named("userId") String targetUserId,
                                                    @Nullable @Named("cursor") final String cursor,
                                                    @Nullable @Named("limit") Integer limit)
            throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().getFollowings(targetUserId, cursor, limit);
    }

    /**
     * Updates an existing {@code Account}.
     *
     * @param user the user
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code Account}
     */
    @ApiMethod(
            name = "users.me.update",
            path = "users/me",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public Account update(@Nullable @Named("mediaId") final String mediaId,
                          @Nullable @Named("badgeName") final String badgeName,
                          final User user) throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        return UserController.newInstance().update(mediaId, badgeName, user);
    }

    /**
     * Deletes the specified {@code Account}.
     *
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code Account}
     */
    @ApiMethod(
            name = "users.me.remove",
            path = "users/me",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(final User user) throws ServiceException {
        Validator.builder()
                .addRule(new AuthenticationRule(user))
                .validate();

        // TODO: 7.07.2016 Implement parentUserKey delete.
    }

    @ApiMethod(
            name = "users.register.google",
            path = "users/google",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Account createGoogleAccount(@Named("idToken") final String idToken,
                                       @Named("locale") final String locale,
                                       final HttpServletRequest request)
            throws ServiceException {

        Payload payload = GoogleAuthenticator.processGoogleToken(idToken);
        if (payload != null) {
            return UserController.newInstance().add(payload, locale);
        }
        return null;
    }

    @ApiMethod(
            name = "users.register.facebook",
            path = "users/facebook",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Account createFacebookAccount(@Named("idToken") final String idToken,
                                         final HttpServletRequest request)
            throws ServiceException {

        // TODO: 26.06.2016 Implement facebook account.
        return null;
    }

    @ApiMethod(
            name = "users.register.yoloo",
            path = "users",
            httpMethod = ApiMethod.HttpMethod.POST)
    public Account createYolooAccount(@Named("credentials") final String credentials,
                                      @Named("locale") final String locale)
            throws ServiceException {
        Validator.builder()
                .addRule(new CredentialsRule(credentials))
                .validate();

        final String decodedCredentials =
                StringUtils.newStringUtf8(Base64.decodeBase64(credentials));
        // credentials pattern = username:password:email
        final String[] values = decodedCredentials.split("\\s*:\\s*");

        Validator.builder()
                .addRule(new CredentialsExistenceRule(values))
                .addRule(new UserConflictRule(values[0] /* username */, values[2] /* email */))
                .validate();

        return UserController.newInstance().add(values, locale);
    }
}