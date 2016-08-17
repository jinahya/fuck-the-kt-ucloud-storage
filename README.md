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

## account

### `authenticateUser`

### `readContainers`
Reads all containers in `text/plain`, `application/xml`, `applicatoin/json`.

## reseller
