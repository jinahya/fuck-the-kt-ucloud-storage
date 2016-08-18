# kt-ucloud-storage-sucks

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

### `authenticateUser`
Authenciates with `authUser` and `authKey`. It's annotated with `@BeforeMethod`.

## account

### `readAllContainers`
Reads all containers using `limit`(`512`) and `marker`.
* Loops reading account. Uses `GET`. Asserts `200`, or `204`.
  * Breaks out the loop on `204`.

### `readContainers`
Reads containers in `text/plain`, `application/xml`, `applicatoin/json`.
* Reads account. Uses `GET`. Asserts `200`, or `204`.

### `verifyContainer`
Updates(creates) and deletes a container.
* `PUT` a container. Assert `201`.
* `GET` the container in [media types](##media types) `text/plain`, `application/xml`, `application/json`. Assert `200`.
* `DELETE` the container. Assert `204`.

## reseller
