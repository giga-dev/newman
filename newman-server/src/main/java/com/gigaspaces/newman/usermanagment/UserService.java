package com.gigaspaces.newman.usermanagment;

import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.util.security.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private PropertyUserStore propertyUserStore;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void updateUsers(PropertyUserStore userStore) throws Exception {
        this.propertyUserStore = userStore;

        if (!containsUsersInDatastore()) {
            this.propertyUserStore.start();
            saveUsersToDB(this.propertyUserStore);
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

    public boolean addUser(User user) {
        logger.info("> adding user: " + user.getUsername());
        if (userRepository.existsByUsername(user.getUsername())) {
            logger.info("> user with name '" + user.getUsername() + "' already exists");
            return false;
        }
        userRepository.save(user);
        propertyUserStore.addUser(user.getUsername(), new Password(user.getDecodedPassword()),new String[]{user.getRole()});
        return true;
    }

    public Optional<User> getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        return Optional.ofNullable(user);
    }

    public List<User> getAllUsers(boolean includeRoot) {
        try {
            logger.info("> requesting all users");
            return includeRoot ? (List<User>)userRepository.findAll() : userRepository.findByUsernameNot("root");
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

        userRepository.deleteByUsername(username);
        logger.info("> user has been deleted");

        return true;
    }

    private boolean containsUsersInDatastore() {
        return getAllUsers(true).size()-1 > 0;  // exclude 'root'
    }

    private void saveUsersToDB(PropertyUserStore inputStore) {
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
            userRepository.save(user);
        });
    }
}
