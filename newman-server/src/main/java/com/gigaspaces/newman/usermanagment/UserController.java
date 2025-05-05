package com.gigaspaces.newman.usermanagment;

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

    public UserController(@Context ServletContext servletContext) {
        this.userService = (UserService) servletContext.getAttribute("userService");
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(UserDTO userDTO) {
        if (userService.addUser(userDTO.toEntity())) {
            // Return a response indicating success
            return Response.status(Response.Status.CREATED).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @DELETE
    @Path("/{username}")
    public Response removeUser(@PathParam("username") String username) {
        // Call UserService to remove the user
        if (userService.deleteUser(username)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity("User not found with username: " + username)
                .build();
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
