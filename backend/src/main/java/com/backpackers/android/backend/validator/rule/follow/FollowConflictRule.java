package com.backpackers.android.backend.validator.rule.follow;

import com.google.api.server.spi.response.ConflictException;
import com.google.appengine.api.users.User;

import com.googlecode.objectify.Key;
import com.backpackers.android.backend.model.follow.Follow;
import com.backpackers.android.backend.model.user.Account;
import com.backpackers.android.backend.validator.Rule;

import static com.backpackers.android.backend.service.OfyHelper.ofy;

public class FollowConflictRule implements Rule<ConflictException> {

    private final String websafeFolloweeKey;
    private final User user;

    /**
     * Instantiates a new Follow conflict rule.
     *
     * @param websafeFolloweeKey the websafe followee key
     * @param user               the parentUserKey
     */
    public FollowConflictRule(String websafeFolloweeKey, User user) {
        this.websafeFolloweeKey = websafeFolloweeKey;
        this.user = user;
    }

    @Override
    public void validate() throws ConflictException {
        Key<Account> followerKey = Key.create(user.getUserId());
        Key<Account> followeeKey = Key.create(websafeFolloweeKey);

        if (followeeKey.equivalent(followerKey)) {
            throw new ConflictException("You can't follow yourself.");
        }

        Key<?> key = ofy().load().type(Follow.class)
                .ancestor(followerKey)
                .filter("followeeKey =", followeeKey)
                .keys().first().now();

        if (key != null) {
            throw new ConflictException("Already following.");
        }
    }
}
