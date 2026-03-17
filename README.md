# HCGateway
HCGateway is a platform to let developers connect to the Health Connect API on Android via a REST API

# How it works
The platform consists of two parts:
- A REST API/server
- A mobile application that pings the server periodically

> [!NOTE]
> This project is still in development. The API may change without notice. The mobile application is also in development and may not work as expected. Please report any issues you find.

> [!IMPORTANT]
> The database was recently migrated from Appwrite to MongoDB. If you were using the Appwrite version, you will need to migrate your data to the new database. You can find the migration script in the `scripts/` folder. You will need to install the `appwrite` and `pymongo` libraries to run the script, then run the script with the following command: `python3 migrate_1.5.0.py`.


## How it Works
- The mobile application pings the server every 1 hour to send data. The following data types are supported-
    - Sleep (`sleepSession`)

- Each sync takes approximatly 1 minute
- The server encrypts the data using Fernet encryption, then stores it in a mongo database.
- The server exposes an API to let developers login and get the data for their users.

## Get Started
- You can install the mobile application through the APK file. You can find the latest APK file in the releases section of this repository.
- Once you install the Android APK file, signup by entering a username and password
- Once you see a screen showing state, you have successfully signed up. Your data will sync in 1 hour. You also have the option to force a sync any time through the application.

## Database
### Users Structure
```
users {
    _id: string
    username: string
    password: string
    fcmToken: string
    expiry: datetime
    token: string
    refresh: string
}
```
> [!NOTE]
> The password of the user encrypted using Argon 2 format. The password is never stored as is, and cannot be retrieved through any API.

### Database Structure
```
hcgateway_[user_id]: string {
    dataType: string {
        _id: string
        data: string
        id: string
        start: datetime
        end: datetime
        app: string
    }
}
```

### Parameters
- `$id` - The ID of the object. 
- `data` - The data of the object encrypted using Fernet. When asked for through the API, the data will be decrypted for you using the user's hashed password found from the user id.
- `id` - The ID of the object- This is the same as `_id` and is only kept for backward compatibility. May be removed in future versions.
- `start` - The start date and time of the object
- `end` - The end date and time of the object. Might not be present for some objects.
- `app` - The app package string that the object was synced from.

## Mobile Application
The mobile application is a simple Android application that pings the server every 1 hour to send data. It starts a foreground service to do this, and the service will run even if the application is closed. The application is written in ~~React Native~~ **FUCK YOU!!** This app works on Kotlin, on fucking native Android Language without trash with expo and JS.

## Self Hosting
You can self host the server and database for full control.

### Docker (recommended)
1. **Prerequisites**\
    Ensure that you have Docker and Docker Compose installed on your system.

2. **Setting up the Environment**

   - You’ll need to configure environment variables before starting the services.
   - Copy the provided `.env.example` file to `.env` inside the `api/` directory and configure it as necessary. When setting the `MONGO_URI` variable, the following format should be used: `mongodb://<username>:<password>@db:27017/hcgateway?authSource=admin`
   - Set the mongo DB username and password in the `docker-compose.yml` file as well.
   - Configure IP whitelist for server in `nginx/nginx.conf:52`. It necessary if you fetching api from remote server

3. **Running the Containers with Docker Compose**\
    The project uses Docker Compose for easier container orchestration. To run the API using Docker Compose, run the following command:
    ```bash
   docker-compose up -d
   ```
You can access the API at `http://localhost:6644`

### Manual
#### Server
- Prerequisites: Python 3, mongoDB
- Clone this repository
- `cd` into the api/ folder
- run `pip install -r requirements.txt`
- rename `.env.example` to `.env` and fill in the values
- run `python3 main.py` to start the server

#### Mobile Application
- Prerequisites: Android Studio (SDK, build-tools, platform-tools), Java 17
- Clone this repository
- `cd` into app_new/ folder
- Change API server in `app/build.gradle.kts:20`
- run `./gradlew assembleRelease` to build the APK file

idk what write to app building, it basic android app, nothing dificult

