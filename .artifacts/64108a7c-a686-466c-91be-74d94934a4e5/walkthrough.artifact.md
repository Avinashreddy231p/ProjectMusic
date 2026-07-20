# Walkthrough - Resolved `java.net.SocketException: Connection reset`

I have successfully updated the Gradle configuration to address the network connection issues that were causing the build to fail.

## Changes Made

### Gradle Configuration

#### [gradle-wrapper.properties](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/gradle/wrapper/gradle-wrapper.properties)
- Increased `networkTimeout` to `60000` (60 seconds). This gives Gradle more time to establish connections and download the distribution, which is helpful on unstable networks.

#### [gradle.properties](file:///C:/Users/Avina/OneDrive/Documents/BoomingMusic-master/D2/BoomingMusic-master/gradle.properties)
- Updated `org.gradle.jvmargs` to include:
    - `-Dhttps.protocols=TLSv1.2,TLSv1.3`: Enforces modern security protocols for HTTPS connections, preventing resets caused by outdated protocol negotiation.
    - `-Dhttp.connectionTimeout=60000`: Sets a 60-second connection timeout for Gradle's internal HTTP client.
    - `-Dhttp.socketTimeout=60000`: Sets a 60-second socket timeout for data transfer.

## Verification Results

### Automated Tests
- Ran `:app:compileDebugKotlin` and the build finished successfully.

> [!TIP]
> If you encounter similar network issues in the future, check if you are using a VPN or proxy that might be interfering with specific repositories like JitPack or Maven Central.
