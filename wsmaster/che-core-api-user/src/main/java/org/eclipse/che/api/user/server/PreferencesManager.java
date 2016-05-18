package org.eclipse.che.api.user.server;

import com.google.common.util.concurrent.Striped;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.spi.PreferenceDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static java.util.Objects.requireNonNull;

/**
 * Facade for preferences related operations and
 * layer of preferences business logic validation.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class PreferencesManager {

    private static final Striped<Lock> UPDATE_LOCKS = Striped.lazyWeakLock(100);

    @Inject
    private PreferenceDao preferenceDao;

    /**
     * Associates the given {@code preferences} with the given {@code userId}.
     *
     * <p>Note that this method will override all the existing properties
     * for the user with id {@code userId}.
     *
     * @param userId
     *         the user id whom the {@code preferences} belong to
     * @param preferences
     *         the preferences to associate with the {@code userId}
     * @throws NullPointerException
     *         when either {@code userId} or {@code preferences} is null
     * @throws ServerException
     *         when any error occurs
     */
    public void save(String userId, Map<String, String> preferences) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        requireNonNull(preferences, "Required non-null preferences");
        preferenceDao.setPreferences(userId, preferences);
    }

    /**
     * Updates the preferences of the user by merging given {@code preferences}
     * with the existing preferences. If user doesn't have any preferences
     * then the given {@code preferences} will be associated with the user.
     *
     * @param userId
     *         the user whose preferences should be updated
     * @param preferences
     *         preferences update
     * @return all the user's preferences including the update
     * @throws NullPointerException
     *         when either {@code userId} or {@code preferences} is null
     * @throws ServerException
     *         when any error occurs
     */
    public Map<String, String> update(String userId, Map<String, String> preferences) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        requireNonNull(preferences, "Required non-null preferences");
        // Holding reference to prevent garbage collection
        // this lock helps to avoid race-conditions when parallel updates are applied
        final Lock lock = UPDATE_LOCKS.get(userId);
        lock.lock();
        try {
            final Map<String, String> found = preferenceDao.getPreferences(userId);
            found.putAll(preferences);
            preferenceDao.setPreferences(userId, found);
            return found;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finds user's preferences.
     *
     * @param userId
     *         user id to find preferences
     * @return found preferences or empty map, if there are no preferences related to user
     * @throws NullPointerException
     *         when {@code userId} is null
     * @throws ServerException
     *         when any error occurs
     */
    public Map<String, String> find(String userId) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        return preferenceDao.getPreferences(userId);
    }

    /**
     * Finds user's preferences.
     *
     * @param userId
     *         user id to find preferences
     * @param keyFilter
     *         regex which is used to filter preferences by keys, so
     *         result contains only the user's preferences that match {@code keyFilter} regex
     * @return found preferences filtered by {@code keyFilter} or an empty map
     * if there are no preferences related to user
     * @throws NullPointerException
     *         when {@code userId} is null
     * @throws ServerException
     *         when any error occurs
     */
    public Map<String, String> find(String userId, String keyFilter) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        return preferenceDao.getPreferences(userId, keyFilter);
    }

    /**
     * Removes(clears) user's preferences.
     *
     * @param userId
     *         the id of the user to remove preferences
     * @throws NullPointerException
     *         when {@code userId} is null
     * @throws ServerException
     *         when any error occurs
     */
    public void remove(String userId) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        preferenceDao.remove(userId);
    }

    /**
     * Removes the preferences with the given {@code names}.
     *
     * @param names
     *         the names to remove
     * @throws NullPointerException
     *         when either {@code userId} or {@code names} is null
     * @throws ServerException
     *         when any error occurs
     */
    public void remove(String userId, List<String> names) throws ServerException {
        requireNonNull(userId, "Required non-null user id");
        requireNonNull(names, "Required non-null preference names");
        // Holding reference to prevent garbage collection
        // this lock helps to avoid race-conditions when parallel updates are applied
        final Lock lock = UPDATE_LOCKS.get(userId);
        lock.lock();
        try {
            final Map<String, String> preferences = preferenceDao.getPreferences(userId);
            names.forEach(preferences::remove);
            preferenceDao.setPreferences(userId, preferences);
        } finally {
            lock.unlock();
        }
    }
}
