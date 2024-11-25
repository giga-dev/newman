package com.gigaspaces.newman.usermanagment;

import com.gigaspaces.newman.config.Config;
import com.mongodb.MongoClient;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.util.security.Password;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final PropertyUserStore propertyUserStore;
    private final UserDAO userDAO;

    public UserService(PropertyUserStore userStore, Config config) throws Exception {
        logger.info("> mongoHost: " + config.getMongo().getHost());
        logger.info("> mongoDb: " + config.getMongo().getDb());

        MongoClient mongoClient = new MongoClient(config.getMongo().getHost());
        this.propertyUserStore = userStore;

        Morphia morphia = new Morphia();
        morphia.map(User.class);

        Datastore datastore = morphia.createDatastore(mongoClient, config.getMongo().getDb());
        datastore.ensureIndexes();
        datastore.ensureCaps();

        this.userDAO = new UserDAO(morphia, mongoClient, config.getMongo().getDb());

        if (!containsUsersInDatastore()) {
            this.propertyUserStore.start();
            saveUsersToMongo(this.propertyUserStore);
            logger.info("> users are now saved to MongoDB datastore");
        } else {
            propertyUserStore.setConfig(null);
            getAllUsers(true).forEach(user -> {
                // fill up store with users pulled from mongo
                propertyUserStore.addUser(user.getUsername(), new Password(user.getDecodedPassword()), new String[]{user.getRole()});
            });
            propertyUserStore.start();
            logger.info("> users have been restored from MongoDB: " + propertyUserStore.getKnownUserIdentities().size());
        }
    }

    public void addUser(User user) {
        logger.info("> adding user: " + user.getUsername());
        userDAO.save(user);
        propertyUserStore.addUser(user.getUsername(), new Password(user.getDecodedPassword()),new String[]{user.getRole()});
    }

    public Optional<User> getUserByUsername(String username) {
        User user = userDAO.find(userDAO.createIdQuery(username)).get();
        return Optional.ofNullable(user);
    }

    public List<User> getAllUsers(boolean includeRoot) {
        try {
            logger.info("> requesting all users");
            Query<User> query = userDAO.createQuery();
            return userDAO.find(query).asList().stream()
                    .filter(u -> includeRoot || !"root".equals(u.getUsername()))   // 'root' is a system account. Don't show it up
                    .collect(Collectors.toList());
        } catch (Throwable t) {
            logger.info(t.getMessage());
        }
        logger.info("> empty result");
        return new LinkedList<>();
    }

    private void printUsersFromStore() {
        this.propertyUserStore.getKnownUserIdentities().forEach((s, userIdentity) -> {
            logger.info("Name: " + s);
        });
    }

    public boolean deleteUser(String username) {
        logger.info("> deleting user: " + username);
        if ("root".equals(username)) {
            logger.info("> not allowed to delete '" + username + "'");
            return false;
        }
        Query<User> query = userDAO.createQuery().field("username").equal(username);
        User user = userDAO.findOne(query);
        if (user != null) {
            Datastore datastore = userDAO.getDatastore();
            datastore.findAndDelete(query);
            logger.info("> user has been deleted");
            return true;
        } else {
            return false;
        }
    }

    private boolean containsUsersInDatastore() {
        return getAllUsers(true).size()-1 > 0;  // exclude 'root'
    }

    private void saveUsersToMongo(PropertyUserStore inputStore) {
        inputStore.getKnownUserIdentities().forEach((username, userIdentity) -> {
            // Get roles for the user
            String role = userIdentity.getSubject().getPrincipals()
                    .stream()
                    .map(Principal::getName)
                    .collect(Collectors.joining(","));

            // Create User object
            String[] userData = role.split(",");
            User user = new User(username, userData[0].trim(), userData[1].trim());
            logger.info("user: " + user);
            // Save User to MongoDB
            userDAO.save(user);
        });
    }
}
