package com.gigaspaces.newman.usermanagment;

import org.eclipse.jetty.security.PropertyUserStore;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Path("/users")
public class UserController {

    private final UserService userService;

    public UserController(@Context ServletContext servletContext) throws Exception {
        this.userService = (UserService) servletContext.getAttribute("userService");

        PropertyUserStore userStore = (PropertyUserStore) servletContext.getAttribute("userStore");
        userStore.start();
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(UserDTO userDTO) {
        userService.addUser(userDTO.toEntity());

        // Return a response indicating success
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{username}")
    public Response removeUser(@PathParam("username") String username) {
        // Call UserService to remove the user
        boolean success = userService.deleteUser(username);

        if (success) {
            // Return 204 No Content if user was successfully removed
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            // Return 404 Not Found if user doesn't exist
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("User not found with username: " + username)
                    .build();
        }
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserDTO> listUsers() {
        return userService.getAllUsers(false).stream()
                .map(UserDTO::fromEntity)
                .sorted(Comparator.comparing(UserDTO::getUsername))
                .collect(Collectors.toList());
    }
}
