# kt-ucloud-storage-sucks
a simple artifact supposed to fail.

## verify
### account
```
mvn -DauthUrl=... -DauthUser=... -DauthKey=... verify
```
### reseller
```
mvn -DauthUrl=... -DauthUser=<account>:<user> -DauthKey=<password> verify
```

## media types
`text/plain`, `application/xml`, and `application/json`.

## common

### authenticateUser
Authenciates with `authUser` and `authKey`. It's annotated with `@BeforeMethod`.

## account

### readAllContainers
Reads all containers using `limit`(`512`) and `marker`.
* In a loop, `GET` containers. Assert `200`, or `204`.
  * Break out on `204`.

### readContainers
Reads containers in each [media types](##media types).
* `GET` containers. Assert `200`, or `204`.

### verifyContainer
Updates(creates), reads, and deletes a container.
* `PUT` a container. Assert `201`, or `202`.
* `HEAD` the container. Assert `204`.
* `DELETE` the container. Assert `204`.

### verifyObject
Updates(creates), reads, and deletes an object.
* `PUT` a container. Assert `201`, or `202`.
* `PUT` an object. Asert `201`, or `202`.
* `GET` the object. Assert `200`.
* `DELETE` the object. Assert `204`.
* `DELETE` the container. Assert `204`.

## reseller
