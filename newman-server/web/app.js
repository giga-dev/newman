var app = document.querySelector("#app");
app.heading="Hello Newman users!";
app.selected=0;
app.ajaxResponse = function(event, object){
    app.user = object.response;
    app.heading = "Hello " + app.user.userName;
}