TODO
------
Create a container
POST /containers/create

Container restart policy https://docs.docker.com/engine/api/v1.24/#31-containers

"RestartPolicy": { "Name": "", "MaximumRetryCount": 0 },

RestartPolicy – The behavior to apply when the container exits. The value is an object with a Name property of either 
	"always" to always restart, 
	"unless-stopped" to restart always except when user has manually stopped the container or 
	"on-failure" to restart only when the container exit code is non-zero. 
If on-failure is used, MaximumRetryCount controls the number of times to retry before giving up. The default is not to restart. (optional) An ever increasing delay (double the previous delay, starting at 100mS) is added before each restart to prevent flooding the server.

Query parameters:

    name – Assign the specified name to the container. Must match /?[a-zA-Z0-9_-]+.

=== 3.3 Misc

Check auth configuration

POST /auth

Validate credentials for a registry and get identity token, if available, for accessing the registry without password.

Example request:

POST /v1.24/auth HTTP/1.1
Content-Type: application/json
Content-Length: 12345

{
     "username": "hannibal",
     "password": "xxxx",
     "serveraddress": "https://index.docker.io/v1/"
}

Example response:

HTTP/1.1 200 OK

{
     "Status": "Login Succeeded",
     "IdentityToken": "9cbaf023786cd7..."
}

Status codes:

    200 – no error
    204 – no error
    500 – server error
    